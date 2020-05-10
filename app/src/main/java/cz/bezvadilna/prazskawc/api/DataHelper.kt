package cz.bezvadilna.prazskawc.api

import NamespaceResolver
import android.content.Context
import android.util.Log
import androidx.multidex.BuildConfig
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.data.geojson.GeoJsonPoint
import cz.bezvadilna.prazskawc.database.AppDatabase
import cz.bezvadilna.prazskawc.extensions.localizedDayOfWeek
import cz.bezvadilna.prazskawc.extensions.withoutTime
import cz.bezvadilna.prazskawc.model.Point
import cz.bezvadilna.prazskawc.model.database.dao.OpeningHoursDao
import cz.bezvadilna.prazskawc.model.database.dao.PlaceDao
import cz.bezvadilna.prazskawc.model.database.dao.SettingsDao
import cz.bezvadilna.prazskawc.model.database.entity.Place
import cz.bezvadilna.prazskawc.model.database.entity.Settings
import cz.bezvadilna.prazskawc.network.downloadFileFromUrl
import kotlinx.coroutines.*
import org.w3c.dom.Node
import org.xml.sax.SAXParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants.NODE
import javax.xml.xpath.XPathFactory

const val APP_VERSION = BuildConfig.VERSION_NAME
const val XML_URL =
    "https://app.iprpraha.cz/geoportal/rest/document?id=CZ-70883858-FSV_CUR.FSV_VerejnaWC_b-FC%0A"
const val GEOJSON_URL =
    "http://opendata.iprpraha.cz/CUR/FSV/FSV_VerejnaWC_b/WGS_84/FSV_VerejnaWC_b.json"
const val DATAHELPER_LOG_TAG_ERROR = "DATAHELPER_ERROR"
const val DATAHELPER_LOG_TAG_INFO = "DATAHELPER_INFO"

class DataHelper(private val applicationContext: Context) {

    private var db: AppDatabase? = AppDatabase.invoke(applicationContext)
    private var openingHoursDao: OpeningHoursDao? = db?.OpeningHoursDao()
    private var placeDao: PlaceDao? = db?.PlaceDao()
    private var settingsDao: SettingsDao? = db?.SettingsDao()
    private var today: Date = Calendar.getInstance().time

    //
    private lateinit var dataParser: DataParser

    //Date of last update from downloaded xml
    private var lastUpdateAvailable: Date? = null

    /**
     * Look into database if we checked for updates today
     * There is no need to check for updates on same day
     * @true if data was checked today
     * @false if data wasn't checked today yet or if can't tell due to problem
     */
    private fun wasCheckedToday(): Boolean {
        var lastDbUpdateCheck: Date? = null

        settingsDao?.getSettings()?.let {
            lastDbUpdateCheck = it.lastUpdateCheck
        } ?: Log.e(DATAHELPER_LOG_TAG_ERROR, "Can't get Date of last check from database")
        val copyLastDbUpdateCheck = lastDbUpdateCheck

        return if (copyLastDbUpdateCheck === null) {
            //App should continue even if there is error with getting date from database
            false
        } else {
            (copyLastDbUpdateCheck.withoutTime() == today.withoutTime())
        }
    }

    /**
     * Downloads descriptive XML and compare update date with date from database
     * @true if new data are available or if can't tell due to problem
     * @false if there is no new data available
     */
    private suspend fun isNewDataAvailable(): Boolean {
        var lastUpdateData: Date? = null
        lastUpdateAvailable = null

        withContext(Dispatchers.IO) {
            downloadFileFromUrl(XML_URL)?.let {
                lastUpdateAvailable = getDateFromXml(it)
            } ?: Log.e(
                DATAHELPER_LOG_TAG_ERROR,
                "Can't get Date of last update from downloaded xml"
            )
        }
        settingsDao?.getSettings()?.let {
            lastUpdateData = it.lastUpdateData
        } ?: Log.e(DATAHELPER_LOG_TAG_ERROR, "Can't get Date of last caching from database")

        val copyLastUpdateData: Date? = lastUpdateData
        val copyLastUpdateAvailable: Date? = lastUpdateAvailable

        //We try to continue even if there is error with getting data from database or xml
        if (copyLastUpdateAvailable != null && copyLastUpdateData != null) {
            if (copyLastUpdateAvailable <= copyLastUpdateData)
                return false
            if (copyLastUpdateAvailable > copyLastUpdateData)
                return true
        }
        if (copyLastUpdateAvailable === null || copyLastUpdateData === null) {
            Log.e(DATAHELPER_LOG_TAG_ERROR, "Date of available or cached update is null")
            return true
        }

        Log.e(
            DATAHELPER_LOG_TAG_ERROR,
            "Return from fun isNewDataAvailable, that should be unreachable!!"
        )
        return false
    }

    /**
     *   Parse Date from given xml, when data lastly changed
     */
    private fun getDateFromXml(xml: String): Date? {
        try {
            val docBuilderFactory = DocumentBuilderFactory.newInstance()
            docBuilderFactory.isNamespaceAware = true
            val document = docBuilderFactory
                .newDocumentBuilder()
                .parse(xml.byteInputStream())
            val xpath = XPathFactory.newInstance().newXPath()
            xpath.namespaceContext = (NamespaceResolver(document))

            val versionDateNode =
                xpath.evaluate("//gfc:versionDate/gco:DateTime", document, NODE) as Node
            val versionDate = versionDateNode.textContent
            val sdf = SimpleDateFormat("yyyyMMdd")
            return sdf.parse(versionDate)
        } catch (e: SAXParseException) {
            Log.e(DATAHELPER_LOG_TAG_ERROR, "Error when parsing String to xml. $e")
        } catch (e: Exception) {
            Log.e(DATAHELPER_LOG_TAG_ERROR, "Can't get Date from given String. $e")
        }
        return null
    }

    /**
     * Fill the settings table with given data
     * Should be called when data is successfully updated
     */
    private fun fillSettings(lastUpdateCheck: Date, lastUpdateData: Date) {
        val settings = Settings(
            0,
            lastUpdateCheck,
            lastUpdateData,
            APP_VERSION
        )
        settingsDao?.let {
            it.upsert(settings)
        }
    }

    /**
     * Fill the settings table with given data
     * Should be called on success update check
     */
    private fun fillSettings(lastUpdateCheck: Date) {
        settingsDao?.getSettings()?.let {
            fillSettings(lastUpdateCheck, it.lastUpdateData)
        }
    }

    /**
     * Returns actual settings from database
     */
    suspend fun getSettings(): Settings? {
        settingsDao?.getSettings()?.let {
            return it
        }
        return null
    }

    /**
     *  Main function, which fill the database when new data are available
     */
    suspend fun fillCache(map: GoogleMap) {
        /**
         * For debugging the database update process, uncomment these lines and insert:
         * lastUpdateData before the real date from descriptive xml
         * lastUpdateCheck before today
         *
         * val lastUpdateData = "20191101"
         * val lastUpdateCheck = "20191207"
         * val sdf = SimpleDateFormat("yyyyMMdd")
         * fillSettings(sdf.parse(lastUpdateCheck), sdf.parse(lastUpdateData))
         */

        val lastUpdateData = "20191101"
        val lastUpdateCheck = "20191207"
        val sdf = SimpleDateFormat("yyyyMMdd")
        //fillSettings(sdf.parse(lastUpdateCheck), sdf.parse(lastUpdateData))

        var geoJsonFile: String? = null

        if (!wasCheckedToday()) {
            if (isNewDataAvailable()) {
                withContext(Dispatchers.IO) { geoJsonFile =
                    downloadFileFromUrl(
                        GEOJSON_URL
                    )
                }
                geoJsonFile?.let {
                    dataParser = DataParser(applicationContext)
                    dataParser.putGeoJsonToDatabase(getGeoJsonLayer(map, it))
                    fillSettings(today, lastUpdateAvailable ?: today)
                }
            } else {
                fillSettings(today)
            }
        }
    }

    /**
     *   Main function, which return GeoJsonLayer of Places from the stored data
     */
    suspend fun getCachedPoints(): MutableList<Point> {
        val points = mutableListOf<Point>()

        //get Places from database
        placeDao?.let {
            val places = it.listPlaces()
            for (place in places) {
                //fill with features
                val pointGeometry = GeoJsonPoint(LatLng(place.latitude, place.longitude))
                val properties: HashMap<String, String?> = HashMap()
                properties["address"] = place.address
                properties["openingHours"] = place.openingHours
                properties["price"] = place.price.toString()
                properties["priceAlternate"] = place.priceAlternate
                val pointFeature = Point(
                    pointGeometry,
                    place.objectId.toString(),
                    properties,
                    null,
                    place
                )
                //geoJsonLayer.addFeature(pointFeature)
                points.add(pointFeature)
                Log.e(
                    DATAHELPER_LOG_TAG_INFO,
                    "place with objectId: ${place.objectId}, loaded successfully"
                )
            }
        } ?: Log.e(DATAHELPER_LOG_TAG_ERROR, "Can't load places from database. Dao is null.")
        return points
    }

    /**
     * Check if place with given id is opened
     * Timezone is set to be same as the GeoJson timezone
     */
    suspend fun isPlaceOpened(place: Place): Boolean {
        return place.objectId in getOpenedPlaces()
    }

    /**
     * Returns List of Ids with opened places
     * Timezone is set to be same as the GeoJson timezone
     */
    suspend fun getOpenedPlaces(): List<Int> {
        val timeZone = TimeZone.getTimeZone("Europe/Prague")
        val locale = Locale("cs", "CZ")
        val calendar = Calendar.getInstance(timeZone, locale)
        openingHoursDao?.let {
            return it.getOpenedPlaces(
                calendar.localizedDayOfWeek(),
                calendar[Calendar.HOUR_OF_DAY] * 60 + calendar[Calendar.MINUTE]
            )
        }
        return listOf<Int>()
    }
}