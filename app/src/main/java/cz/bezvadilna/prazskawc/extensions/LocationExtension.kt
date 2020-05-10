package cz.bezvadilna.prazskawc.extensions

import android.location.Location
import com.google.android.gms.maps.model.LatLng

fun Location.getLatLng():LatLng{
    return LatLng(this.latitude, this.longitude)
}