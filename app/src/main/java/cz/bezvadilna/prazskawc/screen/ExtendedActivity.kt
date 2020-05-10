package cz.bezvadilna.prazskawc.screen

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cz.bezvadilna.prazskawc.R

open class ExtendedActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        // Setting theme back before anything else
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
    }

}