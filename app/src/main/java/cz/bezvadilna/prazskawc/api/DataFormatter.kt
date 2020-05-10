package cz.bezvadilna.prazskawc.api

import android.content.Context
import cz.bezvadilna.prazskawc.R

/**
 * Format distance nicely
 */
fun formatDistanceNumber(distance: Double): String {
    var rdistance = distance
    var unit = " m"
    if (rdistance > 1000) {
        rdistance /= 1000.0
        unit = " km"
    }
    return String.format("%4.0f%s", rdistance, unit)
}

/**
 * Format price nicely for user
 */
fun formatPriceNumber(price: Int?, priceAlternate: String?, context: Context): String {
    return if (priceAlternate.isNullOrBlank()) {
        when (price) {
            null -> {
                context.getString(R.string.price_unknown)
            }
            0 -> {
                context.getString(R.string.price_free)
            }
            else -> {
                price.toString() + " " + context.getString(R.string.price_cz_crowns)
            }
        }
    } else {
        priceAlternate
    }
}

/**
 * Format openingHours nicely
 */
fun formatOpeningHours(openingHours: String?, context: Context): String {
    return if (openingHours.isNullOrBlank()) {
        context.getString(R.string.opening_hours_unknown)
    } else {
        openingHours
    }
}