package cz.bezvadilna.prazskawc.extensions

import java.util.*

/**
 * Moves Integers of day of week by first day of week defined
 */
fun Calendar.localizedDayOfWeek(): Int {
    val toReturn = this[Calendar.DAY_OF_WEEK] - (firstDayOfWeek - 1)
    return if (toReturn < 1) {
        toReturn + 7
    } else {
        toReturn
    }
}