package cz.bezvadilna.prazskawc.screen

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import cz.bezvadilna.prazskawc.R
import cz.bezvadilna.prazskawc.api.DataHelper
import cz.bezvadilna.prazskawc.model.Point
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.lang.ref.WeakReference

const val UPDATE_OH_DELAY: Long = 15000
const val GOOGLE_PLAY_SERVICES_VERSION_CODE = 13400000

class MainActivity : ExtendedActivity(), PointHolderInterface {
    private lateinit var navController: NavController
    private lateinit var mTopToolbar: Toolbar
    private var navHostFragment: Fragment? = null
    override var tryToReloadCache = true
    override lateinit var dataHelper: DataHelper
    override var cachedPoints = mutableListOf<Point>()

    /*List with hosted fragments working with points*/
    override var pointViewers = mutableListOf<WeakReference<PointViewerInterface>>()

    /*Scope for launching Coroutines on map and list fragments*/
    private val mainActivityScope = CoroutineScope(Dispatchers.Default)
    override val pointHolderScope = mainActivityScope

    /*Handler for schedule OH updates, from: https://guides.codepath.com/android/Repeating-Periodic-Tasks*/
    private val handler = Handler()
    private val updateOH = object : Runnable {
        override fun run() {
            updateOpeningHours()
            handler.postDelayed(this, UPDATE_OH_DELAY)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //Toolbar
        mTopToolbar = findViewById<View>(R.id.activity_main_toolbar) as Toolbar
        mTopToolbar.setLogo(R.drawable.toilet_w_space)
        setSupportActionBar(mTopToolbar)

        if(googlePlayServices()){

            //Getting the Navigation Controller
            navController = Navigation.findNavController(this, R.id.nav_host_fragment)

            //Setting the navigation controller to Bottom Nav
            bottomNav.setupWithNavController(navController)

            //Getting fragment holding navigation
            navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)

            //Setting up the action bar
            NavigationUI.setupActionBarWithNavController(this, navController)
        }

        //Helper instance
        dataHelper = DataHelper(applicationContext)

        //Initiate check of OH
        handler.post(updateOH)
    }

    /**
     * Gets fragments from navhost fragment managers and pull them to the pointViewers list
     */
    override fun updatePointViewers() {
        if (navHostFragment != null && navHostFragment is Fragment) {
            for (fragment in navHostFragment!!.childFragmentManager.fragments)
                if (fragment is PointViewerInterface){
                    val reference = WeakReference(fragment as PointViewerInterface)
                    if (reference !in pointViewers){
                        pointViewers.add(reference)
                    }
                }
        }
    }

    //Toolbar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        //Inflate the menu to use in the action bar
        val inflater = menuInflater
        inflater.inflate(R.menu.top_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        if (id == R.id.action_info) {
            val intent = Intent(this, InfoActivity::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     *  Setting Up the back button
     */
    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, null)
    }

    override fun onPause() {
        handler.removeCallbacks(updateOH)
        super.onPause()
    }

    private fun googlePlayServices():Boolean{
        val status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(applicationContext, GOOGLE_PLAY_SERVICES_VERSION_CODE)
        return if(status != ConnectionResult.SUCCESS) {
            // ask user to update google play services.
            GoogleApiAvailability.getInstance().getErrorDialog(this, status, 1).show()
            false
        } else{
            true
        }
    }
}