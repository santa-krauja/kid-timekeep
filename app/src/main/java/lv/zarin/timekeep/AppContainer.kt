package lv.zarin.timekeep

import android.content.Context
import lv.zarin.timekeep.data.db.AppDatabase
import lv.zarin.timekeep.data.repo.RoomFavouriteLookRepository
import lv.zarin.timekeep.data.repo.RoomPresetRepository
import lv.zarin.timekeep.data.repo.RoomTimerRepository
import lv.zarin.timekeep.domain.ports.Clock
import lv.zarin.timekeep.domain.ports.FavouriteLookRepository
import lv.zarin.timekeep.domain.ports.PresetRepository
import lv.zarin.timekeep.domain.ports.SystemClock
import lv.zarin.timekeep.domain.ports.TimerRepository

/** Hand-wired dependencies. Fields are added by later tasks. */
class AppContainer(
    context: Context,
    val inMemoryDb: Boolean = false,
    val clock: Clock = SystemClock,
) {
    val appContext: Context = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.build(appContext, inMemoryDb, clock) }
    val timerRepository: TimerRepository by lazy { RoomTimerRepository(database.timerDao()) }
    val presetRepository: PresetRepository by lazy { RoomPresetRepository(database.presetDao()) }
    val favouriteLookRepository: FavouriteLookRepository by lazy { RoomFavouriteLookRepository(database.favouriteLookDao()) }
}
