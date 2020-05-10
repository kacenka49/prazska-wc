package cz.bezvadilna.prazskawc.model.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

const val SETTINGS_ID_0 = 0

@Entity
data class Settings(
    @PrimaryKey(autoGenerate = false)
    val settings_id: Int = SETTINGS_ID_0,
    var lastUpdateCheck: Date,
    var lastUpdateData: Date,
    var appVersion: String
)
