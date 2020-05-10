package cz.bezvadilna.prazskawc.screen.map

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.maps.android.clustering.ClusterManager
import cz.bezvadilna.prazskawc.R
import cz.bezvadilna.prazskawc.api.*
import cz.bezvadilna.prazskawc.extensions.getLatLng
import cz.bezvadilna.prazskawc.model.Point
import cz.bezvadilna.prazskawc.screen.PointHolderInterface
import cz.bezvadilna.prazskawc.screen.PointViewerInterface
import kotlinx.android.synthetic.main.bottom_sheet.*
import kotlinx.coroutines.*
import kotlin.properties.Delegates

private const val LOCATION_PERMISSION_REQUEST_CODE = 1

class MapFragment : Fragment(), OnMapReadyCallback,
    View.OnClickListener,
    PointViewerInterface,
    ClusterManager.OnClusterItemClickListener<Point> {

    private lateinit var mapFragmentView: View
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private lateinit var dataHelper: DataHelper
    private lateinit var fragmentContext: Context
    private lateinit var clusterManager: ClusterManager<Point>
    private lateinit var holderActivity: PointHolderInterface
    private lateinit var clusterRender: PointClusterRenderer

    private val mapFragmentScope = CoroutineScope(Dispatchers.Default)

    /*Check for selectedPoint changes*/
    private var selectedPoint: Point? by Delegates.observable<Point?>(null) { property, oldValue, newValue ->
        //Log.d("selectedPoint_old",oldValue.toString())
        //Log.d("selectedPoint_new",newValue.toString())
        oldValue?.let {
            it.selected = false
            if (this::clusterRender.isInitialized) {
                clusterRender.setHueOnPoint(it)
            }
        }
        newValue?.let {
            it.selected = true
            if (this::clusterRender.isInitialized) {
                clusterRender.setHueOnPoint(it)
            }
            bottomSheetUpdate(it, true)
        }
    }

    /**
     * Part of fragment lifecycle
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mapFragmentView = inflater.inflate(R.layout.fragment_map, container, false)
        context?.let {
            fragmentContext = it
        }
        //For communicating with mainActivity
        holderActivity = activity as PointHolderInterface

        // Helper classs instance
        dataHelper = holderActivity.dataHelper

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager.findFragmentById(R.id.frgmap) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        // createLocationRequest()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(fragmentContext)

        return mapFragmentView
    }

    override fun onDestroyView() {
        mapFragmentScope.cancel()
        super.onDestroyView()
    }

    /**
     *  Part of Fragment lifecycle
     */
    override fun onStart() {
        super.onStart()
        //BottomSheet listener
        bottom_sheet?.setOnClickListener(this)
    }

    /**
     *  Listen to click on bottom sheet
     */
    override fun onClick(view: View?) {
        if (view?.id == bottom_sheet.id) {
            selectedPoint?.position?.let {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 13f))
            }
        }
    }

    /*Listen to click on cluster items*/
    override fun onClusterItemClick(point: Point?): Boolean {
        point?.let {
            selectedPoint = point
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(point.position, 13f))
        }
        return false
    }

    /* Manipulates the map and data once available.*/
    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap!!
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMapToolbarEnabled = true

        setUpMap()
        setUpCluster()
        //Starts method on separate dispatcher
        val fillWithPoints = holderActivity.pointHolderScope.async { fillPoints() }
    }

    /**
     * Function update locations
     */
    private fun onLocationUpdate() {
        //Update distances in holder
        if (this::lastLocation.isInitialized) {
            holderActivity.updateDistanceToCachedPoints(lastLocation.getLatLng())
        }
        selectedPoint?.let {
            mapFragmentScope.launch(Dispatchers.Main) {
                bottomSheetUpdate(it)
            }
        }
    }

    /**
     *  Loads Points to cachedPointsList in holder and add them to cluster
     */
    private suspend fun fillPoints() {
        val fc = holderActivity.pointHolderScope.async { dataHelper.fillCache(map) }
        if (holderActivity.tryToReloadCache) {
            fc.await()
            //Fill List with points from database
            holderActivity.cachedPoints = dataHelper.getCachedPoints()
            holderActivity.tryToReloadCache = false
        }

        //Add points as cluster items
        for (point in holderActivity.cachedPoints) {
            clusterManager.addItem(point)
        }

        //Set opening hours
        val uoh = holderActivity.pointHolderScope.async { holderActivity.updateOpeningHours() }

        //Force reclustering
        uoh.await()
        //Sets nearest point for newly cached points
        mapFragmentScope.launch(Dispatchers.Main) {
            clusterManager.cluster()
            setNearestPoint()
        }
    }

    /**
     * Find nearest Point by user location and propagate him thought fragment
     * When location is not prepared fallback to center of Prague
     */
    private suspend fun setNearestPoint() {
        if (!this::lastLocation.isInitialized) {
            // fallback to center of Prague
            val fallbackLocation = Location("")
            fallbackLocation.latitude = 50.073658
            fallbackLocation.longitude = 14.418540
            lastLocation = fallbackLocation
            withContext(Dispatchers.Main){
                Toast.makeText(context, getString(R.string.no_location), Toast.LENGTH_LONG).show()
            }
        }

        //select nearest point from cached
        selectedPoint = getNearest(holderActivity.cachedPoints, lastLocation.getLatLng())
        Log.d("selectedPoint", selectedPoint.toString())
        //Sets bounds between user position and selected point and zoom to it
        val builder = LatLngBounds.builder()
        builder.include(lastLocation.getLatLng())
        selectedPoint?.position?.let { builder.include(it) }
        val bounds = builder.build()
        withContext(Dispatchers.Main) {
            try {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            } catch (e: Exception) {
                Log.d(MAPHELPER_LOG_TAG, "Can't animate camera to bounds" + e.printStackTrace())
            }
            //Updates bottom sheet and expand it
            selectedPoint?.let { bottomSheetUpdate(it, true) }
        }
    }

    /**
     * Prepares map and request for permission if needed
     * If user deny permission show him explanation dialog
     */
    private fun setUpMap() {
        if (ContextCompat.checkSelfPermission(
                holderActivity as Activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    holderActivity as Activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                //Show alert with explanation to user
                val dialogBuilder = AlertDialog.Builder(fragmentContext)
                dialogBuilder.setMessage(getString(R.string.alert_position_permission_text))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.alert_position_permission_button))
                    { dialog, id ->
                        requestPermissions(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            LOCATION_PERMISSION_REQUEST_CODE
                        )
                    }
                val alert = dialogBuilder.create()
                alert.setTitle(getString(R.string.alert_positoin_permission_title))
                alert.show()
            } else {
                // No explanation needed, we can request the permission.
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        } else {
            // Permission has already been granted
            /**
             *  draws blue dot on map
             *  mylocation requires permission ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION
             */
            map.isMyLocationEnabled = true

            //get recent location
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    lastLocation = location
                    //fillNearestPoint
                    mapFragmentScope.launch(Dispatchers.Main) { setNearestPoint() }
                    onLocationUpdate()
                }
            }
        }
    }

    /**
     * Called when user deny or allow permission
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                setUpMap()
            }
        }
    }

    /**
     * Setups cluster manager and renderer
     * Must be called after map is ready
     */
    private fun setUpCluster() {
        // Initialize the manager with the context and the map.
        clusterManager = ClusterManager(fragmentContext, map)
        //Point the map's listeners at the listeners implemented by the cluster manager.
        map.setOnCameraIdleListener(clusterManager)
        map.setOnMarkerClickListener(clusterManager)
        //Custom cluster renderer
        clusterRender = PointClusterRenderer(fragmentContext, map, clusterManager)
        clusterManager.renderer = clusterRender
        clusterManager.setOnClusterItemClickListener(this)
        //clusterManager.cluster()
    }

    /**
     * Place non-clustering marker on map
     */
    private fun placeMarkerOnMap(location: LatLng) {
        val markerOptions = MarkerOptions().position(location)
        map.addMarker(markerOptions)
    }

    /**
     * Function called from holder interface, to change visual of points
     */
    override fun onUpdateCachedPoints() {
        if (this::mapFragmentView.isInitialized && this::clusterRender.isInitialized && this::holderActivity.isInitialized) {
            mapFragmentScope.launch(Dispatchers.Main) {
                for (point in holderActivity.cachedPoints) {
                    clusterRender.setHueOnPoint(point)
                }
            }
        }
        //Update bottom sheet distance
        selectedPoint?.let { bottomSheetUpdate(it) }
        Log.d("MAPFRAGMENT", "onupdatecachedpoints")
    }

    /**
     *  Updates bottom sheets texts from Point
     */
    private fun bottomSheetUpdate(point: Point, expand: Boolean = false) =
        mapFragmentScope.launch(Dispatchers.Main) {
            try {
                subAddressText.text = point.place.address.take(25)
                distanceText.text = formatDistanceNumber(point.distance)
                addressText.text = point.place.address
                priceText.text =
                    formatPriceNumber(
                        point.place.price,
                        point.place.priceAlternate,
                        fragmentContext
                    )
                openingText.text = formatOpeningHours(point.place.openingHours, fragmentContext)
                if (expand) {
                    try {
                        val bottomSheetBehavior =
                            BottomSheetBehavior.from(bottom_sheet as LinearLayout)
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    } catch (e: Exception) {
                        Log.d(MAPHELPER_LOG_TAG, "Can't expand bottom sheet" + e.printStackTrace())
                    }
                }
            } catch (e: Exception) {
                Log.e("MAPFRAGMENT", "Bottom sheet update failed. $e")
            }
        }

    /**
     * Bottom Sheet Class
     */
    class OwnBottomSheet : BottomSheetDialogFragment() {
        private val TAG = "OwnBottomSheet"

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val dialog = super.onCreateDialog(savedInstanceState)
            val view = View.inflate(context, R.layout.bottom_sheet, null)
            dialog.setContentView(view)
            return dialog
        }

        fun show(fa: FragmentActivity) {
            show(fa.supportFragmentManager, TAG)
        }
    }
}
