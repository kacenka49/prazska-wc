package cz.bezvadilna.prazskawc.model.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity
data class Place(
    @PrimaryKey(autoGenerate = false)
    var objectId: Int,
    var latitude: Double,
    var longitude: Double,
    var address: String,
    var openingHours: String?,
    var price: Int?,
    var priceAlternate: String?
)