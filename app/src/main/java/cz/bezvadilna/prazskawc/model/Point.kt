package cz.bezvadilna.prazskawc.model

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.data.geojson.GeoJsonFeature
import com.google.maps.android.data.geojson.GeoJsonPoint
import cz.bezvadilna.prazskawc.model.database.entity.Place
import java.util.*

class Point(
    geometry: GeoJsonPoint,
    id: String?,
    properties: HashMap<String, String?>?,
    boundingBox: LatLngBounds?,
    var place: Place,
    var distance: Double= 0.0,
    var opened: Boolean = false,
    var selected: Boolean = false
) : ClusterItem,
    GeoJsonFeature(geometry, id, properties, boundingBox){

    override fun getPosition(): LatLng {
        return (geometry as GeoJsonPoint).coordinates
    }

    /**
     * The title of this marker.
     */
    override fun getTitle(): String?{
        return null
    }

    /**
     * The description of this marker.
     */
    override fun getSnippet(): String?{
        return null
    }

    /**
     * Set distance of point from given position, typically user location
     */
    fun setDistance(fromLatLng: LatLng){
        distance = SphericalUtil.computeDistanceBetween(this.position, fromLatLng)
    }
}