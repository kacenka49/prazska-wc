package cz.bezvadilna.prazskawc.screen

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.appcompat.widget.Toolbar
import cz.bezvadilna.prazskawc.R
import cz.bezvadilna.prazskawc.api.DataHelper
import cz.bezvadilna.prazskawc.extensions.formatNicely
import kotlinx.android.synthetic.main.activity_info.*
import kotlinx.coroutines.*

class InfoActivity : ExtendedActivity() {

    private lateinit var mTopToolbar: Toolbar
    private lateinit var dataHelper: DataHelper
    private var infoScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        //Upper toolbar
        mTopToolbar = findViewById<View>(R.id.activity_info_toolbar) as Toolbar
        setSupportActionBar(mTopToolbar)
        //Actionbar
        val actionbar = supportActionBar
        //Set actionbar title
        actionbar!!.title = getString(R.string.action_info)
        //Set back button
        actionbar.setDisplayHomeAsUpEnabled(true)
    }

    override fun onResume() {
        fillData()
        super.onResume()
    }

    override fun onPause() {
        infoScope.cancel()
        super.onPause()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    /**
     * Fills TextViews with data from database
     */
    private fun fillData() = infoScope.launch{
        dataHelper = DataHelper(applicationContext)
        val settings = dataHelper.getSettings()

        withContext(Dispatchers.Main) {
            settings?.let {
                appVersionText.text = it.appVersion
                lastUpdateText.text = it.lastUpdateData.formatNicely("dd.MM.yyyy")
            }
            dataProviderText.text = getString(R.string.data_provider_info)
            appInfoText.movementMethod = LinkMovementMethod()
        }
    }
}
