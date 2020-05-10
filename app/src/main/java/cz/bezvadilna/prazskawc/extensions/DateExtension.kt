package cz.bezvadilna.prazskawc.extensions

import java.text.SimpleDateFormat
import java.util.*

/**
 * Extension function, removes time from Date and return it back
 */
fun Date.withoutTime(): Date {
    val formatter = SimpleDateFormat("dd/MM/yyyy")
    return formatter.parse(formatter.format(this))
}

/**
 * Extensions function, format Date to String by given pattern and return it
 */
fun Date.formatNicely(pattern: String): String{
    val simpleDateFormat = SimpleDateFormat(pattern)
    return simpleDateFormat.format(this)
}