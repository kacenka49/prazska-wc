package cz.bezvadilna.prazskawc.screen

import com.google.android.gms.maps.model.LatLng
import cz.bezvadilna.prazskawc.api.DataHelper
import cz.bezvadilna.prazskawc.model.Point
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import kotlin.reflect.KFunction1

interface PointHolderInterface {
    /*Controls if the app should reload db cache, typically on app start*/
    var tryToReloadCache: Boolean

    /*PointViewerInterface members, for calling functions*/
    var pointViewers: MutableList<WeakReference<PointViewerInterface>>

    /*DataHelper instance for database operations*/
    var dataHelper: DataHelper

    /*Stored points loaded from database*/
    var cachedPoints: MutableList<Point>

    /*Scope for launching Coroutines on map and list fragments*/
    val pointHolderScope: CoroutineScope

    /**
     *  Function for updating distances to cachedPoints
     */
    fun updateDistanceToCachedPoints(distanceTo: LatLng) {
        for (point in cachedPoints) {
            point.setDistance(distanceTo)
        }
    }

    /**
     * Function must update pointViewers list in advance for onPointViewerInterfaces works
     */
    fun updatePointViewers()

    /**
     *  Updates OH in cachedPoints
     */
    fun updateOpeningHours() = pointHolderScope.launch {
        val openedPoints: List<Int> = dataHelper.getOpenedPlaces()
        for (point in cachedPoints) {
            point.opened = point.place.objectId in openedPoints
        }
        onPointViewerInterfaces(pointViewers, PointViewerInterface::onUpdateCachedPoints)
    }

    /**
     * Runs function on list of pointViewers
     */
    fun onPointViewerInterfaces(
        pointViewers: MutableList<WeakReference<PointViewerInterface>>,
        function: KFunction1<PointViewerInterface, Unit>
    ) {
        updatePointViewers()
        for (pointViewerInterface in pointViewers) {
            pointViewerInterface.get()?.let {
                function.invoke(it)
            }
        }
    }
}