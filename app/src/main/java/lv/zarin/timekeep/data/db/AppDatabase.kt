package lv.zarin.timekeep.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import lv.zarin.timekeep.domain.ports.Clock

@Database(
    entities = [TimerEntity::class, PresetEntity::class, FavouriteLookEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timerDao(): TimerDao
    abstract fun presetDao(): PresetDao
    abstract fun favouriteLookDao(): FavouriteLookDao

    companion object {
        const val NAME = "kidtimekeep.db"

        /** The single construction path, used by production and tests. */
        fun build(context: Context, inMemory: Boolean, clock: Clock): AppDatabase {
            val app = context.applicationContext
            val builder = if (inMemory) {
                Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            } else {
                Room.databaseBuilder(app, AppDatabase::class.java, NAME)
            }
            return builder.addCallback(SeedPresets(app, clock)).build()
        }
    }
}
