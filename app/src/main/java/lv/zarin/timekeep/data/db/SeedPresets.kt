package lv.zarin.timekeep.data.db

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import lv.zarin.timekeep.R
import lv.zarin.timekeep.domain.ports.Clock
import lv.zarin.timekeep.domain.timer.newId

/** Inserts the three starter presets when the database file is first created. */
class SeedPresets(private val context: Context, private val clock: Clock) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        val now = clock.nowMs()
        val seeds = listOf(
            R.string.seed_preset_teeth to 120_000L,
            R.string.seed_preset_dressed to 600_000L,
            R.string.seed_preset_reading to 900_000L,
        )
        seeds.forEachIndexed { index, (nameRes, durationMs) ->
            db.execSQL(
                "INSERT INTO presets (id, name, durationMs, pinned_topPicture, pinned_topSand, " +
                    "pinned_bottomPicture, pinned_bottomSand, last_topPicture, last_topSand, " +
                    "last_bottomPicture, last_bottomSand, sortOrder, createdAtMs, updatedAtMs) " +
                    "VALUES (?, ?, ?, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, ?, ?, ?)",
                arrayOf<Any?>(newId(), context.getString(nameRes), durationMs, index, now, now),
            )
        }
    }
}
