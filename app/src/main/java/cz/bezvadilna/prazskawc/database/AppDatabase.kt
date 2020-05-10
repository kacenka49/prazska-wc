package cz.bezvadilna.prazskawc.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cz.bezvadilna.prazskawc.model.database.converter.DateTypeConverter
import cz.bezvadilna.prazskawc.model.database.dao.OpeningHoursDao
import cz.bezvadilna.prazskawc.model.database.dao.PlaceDao
import cz.bezvadilna.prazskawc.model.database.dao.SettingsDao
import cz.bezvadilna.prazskawc.model.database.entity.OpeningHours
import cz.bezvadilna.prazskawc.model.database.entity.Place
import cz.bezvadilna.prazskawc.model.database.entity.Settings

@Database(entities = [Place::class, OpeningHours::class, Settings::class], version = 31)
@TypeConverters(DateTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun OpeningHoursDao(): OpeningHoursDao
    abstract fun PlaceDao(): PlaceDao
    abstract fun SettingsDao(): SettingsDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null
        private val LOCK = Any()

        operator fun invoke(context: Context) = instance ?: synchronized(LOCK) {
            instance ?: buildDatabase(context).also { instance = it }
        }

        private fun buildDatabase(context: Context) = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "appDatabase"
        ).fallbackToDestructiveMigration().build()
    }

}