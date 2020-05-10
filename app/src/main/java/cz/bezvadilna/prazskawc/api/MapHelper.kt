package cz.bezvadilna.prazskawc.api

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.google.maps.android.data.geojson.GeoJsonLayer
import cz.bezvadilna.prazskawc.R
import cz.bezvadilna.prazskawc.model.Point
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

const val MAPHELPER_LOG_TAG = "MAPHELPER_ERROR"

/**
 * Returns address from given coordinates
 */
fun getAddress(applicationContext: Context, latitude: Double, longitude: Double): String {
    val latLng = LatLng(latitude, longitude)
    return getAddress(applicationContext, latLng)
}

/*
 *   Returns address from given coordinates
 */
fun getAddress(applicationContext: Context, latLng: LatLng): String {
    val geocoder = Geocoder(applicationContext)
    val addresses: List<Address>?
    var addressText = ""

    try {
        addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            addressText = address.getAddressLine(0)
        }
    } catch (e: IOException) {
        Log.e(MAPHELPER_LOG_TAG, "Can't get address from given coordinates" + e.localizedMessage)
    }
    return addressText
}

/**
 *  Go through list of points and return the nearest one
 */
fun getNearest(cachedPoints: MutableList<Point>, location: LatLng?): Point? {
    if (location == null || cachedPoints.isEmpty()) {
        return null
    }
    var toReturn = cachedPoints[0]
    var smallestDistance: Double =
        SphericalUtil.computeDistanceBetween(cachedPoints[0].position, location)
    for (point in cachedPoints) {
        val pointDistance = SphericalUtil.computeDistanceBetween(point.position, location)
        if (pointDistance < smallestDistance) {
            toReturn = point
            smallestDistance = pointDistance
        }
    }
    return toReturn
}

/**
 *  Converts given String to GeoJsonLayer
 *  When there is any trouble getting actual file, prepacked one is provided
 */
suspend fun getGeoJsonLayer(map: GoogleMap, downloaded: String?): GeoJsonLayer {
    if (downloaded != null) {
        try {
            return GeoJsonLayer(map, JSONObject(downloaded.toString()))
        } catch (e: JSONException) {
            Log.e(MAPHELPER_LOG_TAG, "GeoJSON file could not be converted to a JSONObject")
        }
    }
    return GeoJsonLayer(map, JSONObject(R.raw.sampledata.toString()))
}