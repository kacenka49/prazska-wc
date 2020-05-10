package cz.bezvadilna.prazskawc.model.database.dao

import androidx.room.*
import cz.bezvadilna.prazskawc.model.database.entity.Place

@Dao
interface PlaceDao {

    @Query("SELECT * FROM place")
    fun listPlaces(): List<Place>

    @Update
    fun updatePlace(place: Place)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlace(place: Place)

    @Delete
    fun delete(place: Place)

    @Query("DELETE FROM place")
    fun deleteAllPlaces()
}
