package cz.bezvadilna.prazskawc.model.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = Place::class,
        parentColumns = ["objectId"],
        childColumns = ["placeId"]
    )]
)
data class OpeningHours(
    var dayOfWeek: Int,
    //Stored as minutes from 0:01
    var openTimeMinutes: Int,
    var closeTimeMinutes: Int,
    var placeId: Int
) {
    @PrimaryKey(autoGenerate = true)
    var openingHoursId: Int = 0
}