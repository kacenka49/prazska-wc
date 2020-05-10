package cz.bezvadilna.prazskawc.api

import android.content.Context
import android.util.Log
import com.google.maps.android.data.geojson.GeoJsonFeature
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.google.maps.android.data.geojson.GeoJsonPoint
import cz.bezvadilna.prazskawc.R
import cz.bezvadilna.prazskawc.database.AppDatabase
import cz.bezvadilna.prazskawc.model.database.dao.OpeningHoursDao
import cz.bezvadilna.prazskawc.model.database.dao.PlaceDao
import cz.bezvadilna.prazskawc.model.database.entity.OpeningHours
import cz.bezvadilna.prazskawc.model.database.entity.Place
import kotlinx.coroutines.*

/**
 * Class that handle filling database from GeoJson
 */

const val DATAPARSER_LOG_TAG_ERROR = "DATAPARSER_ERROR"
const val DATAPARSER_LOG_TAG_INFO = "DATAPARSER_INFO"

class DataParser(private val applicationContext: Context) {
    private var db: AppDatabase? = AppDatabase.invoke(applicationContext)
    private var openingHoursDao: OpeningHoursDao? = db?.OpeningHoursDao()
    private var placeDao: PlaceDao? = db?.PlaceDao()
    //private var settingsDao: SettingsDao? = db?.SettingsDao()

    /**
     * Parse and insert GeoJSON to database, cleans previous data
     * Runs on multiple-threads using coroutines,and wait for competition
     */
    suspend fun putGeoJsonToDatabase(geoJsonLayer: GeoJsonLayer) {
        val scope = CoroutineScope(CoroutineName("putGeoJsonToDatabase"))
        val putGeoJsonToDatabase = scope.async(Dispatchers.Default) {
            openingHoursDao?.let {
                it.deleteAllOpeningHours()
            }
            placeDao?.let {
                it.deleteAllPlaces()
            }

            for (feature in geoJsonLayer.features) {
                launch { putFeatureToDatabase(feature) }
            }
        }
        putGeoJsonToDatabase.await()
    }

    /**
     *  Goes thought Feature and place it to database
     */
    private suspend fun putFeatureToDatabase(feature: GeoJsonFeature){
        val placeToInsert = Place(0, 1.0, 1.0, "", null, null, null)
        var generatedOpeningHours = false

        val geoJsonPoint = feature.geometry as GeoJsonPoint
        placeToInsert.latitude = geoJsonPoint.coordinates.latitude
        placeToInsert.longitude = geoJsonPoint.coordinates.longitude
        if (feature.hasProperty("OBJECTID")) {
            placeToInsert.objectId = feature.getProperty("OBJECTID").toInt()
        }
        if (feature.hasProperty("ADRESA")) {
            //When there is no address, get one from coordinates
            if (feature.getProperty("ADRESA").isNullOrBlank()) {
                placeToInsert.address =
                    getAddress(
                        applicationContext,
                        placeToInsert.latitude,
                        placeToInsert.longitude
                    )
            } else
                placeToInsert.address = feature.getProperty("ADRESA")
        }
        if (feature.hasProperty("CENA")) {
            val cenaProperty = feature.getProperty("CENA")
            when {
                cenaProperty.isNullOrBlank() -> {
                    placeToInsert.priceAlternate = null
                }
                cenaProperty.equals("zdarma") -> {
                    placeToInsert.price = 0
                }
                else -> {
                    val parsedPrice = parsePrice(cenaProperty)
                    if (parsedPrice !== null) {
                        placeToInsert.price = parsedPrice
                    } else {
                        placeToInsert.priceAlternate = cenaProperty
                    }

                }
            }
        }
        if (feature.hasProperty("OTEVRENO")) {
            val otevrenoProperty = feature.getProperty("OTEVRENO")
            if (!otevrenoProperty.isNullOrBlank()) {
                generatedOpeningHours = true
                placeToInsert.openingHours = otevrenoProperty
                /**
                 * There must be Place in database firstly to insert OH,
                 * because of foreign key restriction
                 */
                placeDao?.let { it.insertPlace(placeToInsert) }
                parseAndFillOpeningHours(placeToInsert.objectId, otevrenoProperty)
            }
        }

        if (!generatedOpeningHours) {
            placeDao?.let { it.insertPlace(placeToInsert) }
        }
    }

    /**
     *   Parse price from given text string
     *   @return price from string
     *   @return null when can't find match
     */
    private suspend fun parsePrice(priceProperty: String): Int? {
        var toReturn: Int? = null
        //delete whitespaces on both sides
        priceProperty.trim()
        //catch when like 'jednotná cena 10 Kč' or any price with Kč at end
        val regexPrice = """(\d+)\sKč""".toRegex()

        if (regexPrice.containsMatchIn(priceProperty)) {
            val groups = regexPrice.find(priceProperty)?.groups
            groups?.let {
                toReturn = groups[1]?.value?.toInt()
            }
        }
        return toReturn
    }

    /**
     *  Parse opening hours using regex and put them in database
     */
    private suspend fun parseAndFillOpeningHours(objectId: Int, otevrenoProperty: String) {
        //delete whitespaces on both sides
        val otevrenoProperty = otevrenoProperty.trim()

        /**
         *   Can be lately modified to somehow highlight matched String for user
         *  @null when regex match
         *  @String with text when match not found
         */
        /**
         *   Can be lately modified to somehow highlight matched String for user
         *  @null when regex match
         *  @String with text when match not found
         */
        var toReturn: String? = otevrenoProperty
        //split if there are more OH like 'po-pá 7-20 h; so-ne 8-19 h' or 'po-pá 7-20 h, so-ne 8-19 h'
        val regexMore = """([;,])""".toRegex()
        //catch when OH is 'nonstop'
        val regexNonstop =
            """(^nonstop)""".toRegex()
        //catch when OH is like 'po-st nonstop'
        val regexNonstopWithDay =
            """(po|út|st|čt|pá|so|ne)\s*-\s*(po|út|st|čt|pá|so|ne)\s*(nonstop)""".toRegex()
        //catch when OH is like 'pá nonstop'
        val regexNonstopWithOneDay =
            """((po|út|st|čt|pá|so|ne)\s*(nonstop))""".toRegex()
        //catch when OH like 'po-pá 7-20 h'
        val regexWithDay =
            """(po|út|st|čt|pá|so|ne)\s*-\s*(po|út|st|čt|pá|so|ne)\s*(\d{1,2})-(\d{1,2})""".toRegex()
        //catch when like 'po-ne 8:30-21:30'
        val regexWithDayMinutes =
            """(po|út|st|čt|pá|so|ne)\s*-\s*(po|út|st|čt|pá|so|ne)\s*(\d{1,2})\S(\d{1,2})\s*-\s*(\d{1,2})\S(\d{1,2})""".toRegex()
        //catch when like 'ne 09:00-23:00'
        val regexWithOneDay =
            """(po|út|st|čt|pá|so|ne)\s*(\d{1,2})\S(\d{1,2})\s*-\s*(\d{1,2})\S(\d{1,2})""".toRegex()
        //catch when like '7:00 - 22:00'
        val regexHoursMinutes = """(\d{1,2})\S(\d{1,2})\s*-\s*(\d{1,2})\S(\d{1,2})""".toRegex()

        when {
            otevrenoProperty.isBlank() -> {
                toReturn = null
            }
            regexMore.containsMatchIn(otevrenoProperty) -> {
                for (splitted in regexMore.split(otevrenoProperty)) {
                    Log.e(
                        DATAPARSER_LOG_TAG_INFO,
                        "${Thread.currentThread().name} Místo: $objectId, text: $otevrenoProperty, rozdělen na: $splitted"
                    )
                    parseAndFillOpeningHours(objectId, splitted)
                }
            }
            regexNonstop.containsMatchIn(otevrenoProperty) -> {
                Log.e(
                    DATAPARSER_LOG_TAG_INFO,
                    "${Thread.currentThread().name} Místo: $objectId, text: $otevrenoProperty, zachyceno regexNonstop"
                )
                fillOpeningHours(objectId, 1, 7, 0, 1440)
                toReturn = null
            }
            regexNonstopWithDay.containsMatchIn(otevrenoProperty) -> {
                Log.e(
                    DATAPARSER_LOG_TAG_INFO,
                    "${Thread.currentThread().name} Místo: $objectId, text: $otevrenoProperty, zachyceno regexNonstopWithDay"
                )
                val groups = regexNonstopWithDay.find(otevrenoProperty)?.groups
                groups?.let {
                    val fromDay = getDayNumber(groups[1]?.value)
                    val toDay = getDayNumber(groups[2]?.value)
                    fillOpeningHours(objectId, fromDay, toDay, 0, 1440)
                }
                toReturn = null
            }
            regexNonstopWithOneDay.containsMatchIn(otevrenoProperty) -> {
                Log.e(
                    DATAPARSER_LOG_TAG_INFO,
                    "${Thread.currentThread().name} Místo: $objectId, text: $otevrenoProperty, zachyceno regexNonstopWithOneDay"
                )
                val groups = regexNonstopWithOneDay.find(otevrenoProperty)?.groups
                groups?.let {
                    val onDay = getDayNumber(groups[1]?.value)
                    fillOpeningHours(objectId, onDay, 0, 1440)
                }
                toReturn = null
            }
            regexWithDay.containsMatchIn(otevrenoProperty) -> {
                Log.e(
                    DATAPARSER_LOG_TAG_INFO,
                    "${Thread.currentThread().name} Místo: $objectId, text: $otevrenoProperty, zachyceno regexWithDay"
                )
                val groups = regexWithDay.find(otevrenoProperty)?.groups
                groups?.let {
                    val fromDay = getDayNumber(groups[1]?.value)
                    val toDay = getDayNumber(groups[2]?.value)
                    val fromHour = groups[3]?.value?.toInt() ?: 0
                    val toHour = groups[4]?.value?.toInt() ?: 0
                    fillOpeningHours(objectId, fromDay, toDay, fromHour * 60, toHour * 60)
                }
                toReturn = null
            }
            regexWithDayMinutes.matches(otevrenoProperty) -> {
                Log.e(
                    DATAPARSER_LOG_TAG_INFO,
                    "${Thread.currentThread().name} Místo: $objectId, text: $otevrenoProperty, zachyceno regexWithDayMinutes"
                )
                val groups = regexWithDayMinutes.find(otevrenoProperty)?.groups
                groups?.let {
                    val fromDay = getDayNumber(groups[1]?.value)
                    val toDay = getDayNumber(groups[2]?.value)
                    val fromHour = groups[3]?.value?.toInt() ?: 0
                    val fromHourMinutes = groups[4]?.value?.toInt() ?: 0
                    val toHour = groups[5]?.value?.toInt() ?: 0
                    val toHourMinutes = groups[6]?.value?.toInt() ?: 0
                    fillOpeningHours(
                        objectId,
                        fromDay,
                        toDay,
                        fromHour * 60 + fromHourMinutes,
                        toHour * 60 + toHourMinutes
                    )
                    toReturn = null
                }
            }
            regexWithOneDay.matches(otevrenoProperty) -> {
                Log.e(
                    DATAPARSER_LOG_TAG_INFO,
                    "${Thread.currentThread().name} Místo: $objectId, text: $otevrenoProperty, zachyceno regexWithOneDay"
                )
                val groups = regexWithOneDay.find(otevrenoProperty)?.groups
                groups?.let {
                    val onDay = getDayNumber(groups[1]?.value)
                    val fromHour = groups[2]?.value?.toInt() ?: 0
                    val fromHourMinutes = groups[3]?.value?.toInt() ?: 0
                    val toHour = groups[4]?.value?.toInt() ?: 0
                    val toHourMinutes = groups[5]?.value?.toInt() ?: 0
                    fillOpeningHours(
                        objectId,
                        onDay,
                        onDay,
                        fromHour * 60 + fromHourMinutes,
                        toHour * 60 + toHourMinutes
                    )
                }
                toReturn = null
            }
            regexHoursMinutes.matches(otevrenoProperty) -> {
                Log.e(
                    DATAPARSER_LOG_TAG_INFO,
                    "${Thread.currentThread().name} Místo: $objectId, text: $otevrenoProperty, zachyceno regexHoursMinutes"
                )
                val groups = regexHoursMinutes.find(otevrenoProperty)?.groups
                groups?.let {
                    val fromHour = groups[1]?.value?.toInt() ?: 0
                    val fromHourMinutes = groups[2]?.value?.toInt() ?: 0
                    val toHour = groups[3]?.value?.toInt() ?: 0
                    val toHourMinutes = groups[4]?.value?.toInt() ?: 0
                    fillOpeningHours(
                        objectId,
                        1,
                        7,
                        fromHour * 60 + fromHourMinutes,
                        toHour * 60 + toHourMinutes
                    )
                }
                toReturn = null
            }
            else -> {
                Log.e(
                    DATAPARSER_LOG_TAG_INFO,
                    "${Thread.currentThread().name} Wrong data: $otevrenoProperty provided to fun fillOpeningHours with objectId: $objectId"
                )
            }
        }
    }

    /**
     *  Insert OH in given interval to database
     *  Hours are stored as minutes from midnight
     */
    private suspend fun fillOpeningHours(
        objectId: Int,
        fromDay: Int,
        toDay: Int,
        fromHourMinutes: Int,
        toHourMinutes: Int
    ) {
        if (fromDay > toDay) {
            //catch if opening goes over Sunday like 'so-út'
            fillOpeningHours(objectId, fromDay, 7, fromHourMinutes, toHourMinutes)
            fillOpeningHours(objectId, 1, toDay, fromHourMinutes, toHourMinutes)
        } else if (fromDay in 1..7 && toDay in 1..7 && fromHourMinutes in 0..1440 && toHourMinutes in 0..1440) {
            for (dayOfWeek in fromDay..toDay) {
                fillOpeningHours(objectId, dayOfWeek, fromHourMinutes, toHourMinutes)
            }
        } else {
            Log.e(
                DATAPARSER_LOG_TAG_ERROR,
                "Provided data didn't fulfill requirements, by fun fillOpeningHours. objectId: $objectId"
            )
        }
    }

    /**
     *  Insert OH for given day into database
     *  Hours are stored as minutes from midnight
     */
    private suspend fun fillOpeningHours(
        objectId: Int,
        dayOfWeek: Int,
        fromHourMinutes: Int,
        toHourMinutes: Int
    ) {
        //catch if opening goes over midnight, like '00:35-2:00'
        if (fromHourMinutes > toHourMinutes) {
            fillOpeningHours(objectId, dayOfWeek, fromHourMinutes, 1440)
            fillOpeningHours(objectId, dayOfWeek, 0, toHourMinutes)
        } else {
            val openingHours = OpeningHours(dayOfWeek, fromHourMinutes, toHourMinutes, objectId)
            openingHoursDao?.let {
                it.insertOpeningHours(openingHours)
            }
        }
    }

    /**
     *  Get day of week number from Czech String
     *  @return Number of day in week 1=Monday
     *  @return 0 on error
     */
    private fun getDayNumber(dayOfWeek: String?): Int {
        return when (dayOfWeek) {
            "po" -> 1
            "út" -> 2
            "st" -> 3
            "čt" -> 4
            "pá" -> 5
            "so" -> 6
            "ne" -> 7
            else -> {
                Log.e(DATAPARSER_LOG_TAG_ERROR, "Can't get day of week number from String.")
                0
            }
        }
    }
}