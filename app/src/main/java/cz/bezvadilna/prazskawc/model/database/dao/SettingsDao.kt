package cz.bezvadilna.prazskawc.model.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cz.bezvadilna.prazskawc.model.database.entity.SETTINGS_ID_0
import cz.bezvadilna.prazskawc.model.database.entity.Settings

@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(Settings: Settings)

    @Query("select * from settings where settings_id = $SETTINGS_ID_0 limit 1")
    fun getSettings(): Settings?
}
