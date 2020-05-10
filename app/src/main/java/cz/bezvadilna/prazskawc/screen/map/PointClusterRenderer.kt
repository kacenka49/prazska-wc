package cz.bezvadilna.prazskawc.screen.map

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import cz.bezvadilna.prazskawc.model.Point

const val OPENED_POINT_HUE = 160f
const val CLOSED_POINT_HUE = 10f
const val SELECTED_POINT_HUE = 200f

class PointClusterRenderer(
    var context: Context,
    map: GoogleMap,
    clusterManager: ClusterManager<Point>
) :
    DefaultClusterRenderer<Point>(context, map, clusterManager) {

    /**
     * Changes color of marker before render
     * Float values are Hue value from HSL color model
     */
    override fun onBeforeClusterItemRendered(item: Point?, markerOptions: MarkerOptions?) {
        super.onBeforeClusterItemRendered(item, getOptionsForPoint(item, markerOptions))
    }

    private fun getOptionsForPoint(point: Point?, markerOptions: MarkerOptions?): MarkerOptions? {
        val markerDescriptor = BitmapDescriptorFactory.defaultMarker(getHueForPoint(point))
        return markerOptions?.icon(markerDescriptor)
    }

    private fun getHueForPoint(point: Point?): Float {
        if (point != null) {
            if (point.selected) {
                return SELECTED_POINT_HUE
            }
            if (point.opened) {
                return OPENED_POINT_HUE
            }
        }
        return CLOSED_POINT_HUE
    }

    fun setHueOnPoint(point: Point) {
        setHueOnPoint(getMarker(point), getHueForPoint(point))
    }

    fun setHueOnPoint(point: Point, hue: Float) {
        setHueOnPoint(getMarker(point), hue)
    }

    fun setHueOnPoint(marker: Marker?, hue: Float) {
        marker?.setIcon(
            BitmapDescriptorFactory.defaultMarker(hue)
        )
    }
}