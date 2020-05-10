package cz.bezvadilna.prazskawc.model.database.dao

import androidx.room.*
import cz.bezvadilna.prazskawc.model.database.entity.OpeningHours

@Dao
interface OpeningHoursDao{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOpeningHours(openingHours: OpeningHours)

    @Update
    fun updateOpeningHours(openingHours: OpeningHours)

    @Delete
    fun deleteOpeningHours(openingHours: OpeningHours)

    @Query("DELETE FROM openingHours")
    fun deleteAllOpeningHours()

    @Query("SELECT placeId FROM OpeningHours WHERE dayOfWeek == :dayOfWeek AND (:timeFromMidnight BETWEEN openTimeMinutes AND closeTimeMinutes)")
    fun getOpenedPlaces(dayOfWeek: Int, timeFromMidnight: Int) : List<Int>
}