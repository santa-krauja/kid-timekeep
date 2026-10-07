package lv.zarin.timekeep.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import lv.zarin.timekeep.domain.ports.Clock
import java.util.concurrent.Executor

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

        /**
         * The single construction path, used by production and tests. [queryExecutor] (null = Room's default
         * background executors) lets tests run every query and transaction on the calling thread, so the
         * Robolectric main looper they pump is the only thread doing work; it also allows main-thread queries.
         */
        fun build(
            context: Context,
            inMemory: Boolean,
            clock: Clock,
            queryExecutor: Executor? = null,
        ): AppDatabase {
            val app = context.applicationContext
            val builder = if (inMemory) {
                Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            } else {
                Room.databaseBuilder(app, AppDatabase::class.java, NAME)
            }
            if (queryExecutor != null) {
                builder.setQueryExecutor(queryExecutor)
                    .setTransactionExecutor(queryExecutor)
                    .allowMainThreadQueries()
            }
            return builder.addCallback(SeedPresets(app, clock)).build()
        }
    }
}
