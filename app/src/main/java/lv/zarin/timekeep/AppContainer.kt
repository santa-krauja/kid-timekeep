package lv.zarin.timekeep

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import lv.zarin.timekeep.data.settings.DataStoreSettingsRepository
import lv.zarin.timekeep.data.settings.settingsDataStore
import lv.zarin.timekeep.domain.ports.SettingsRepository
import java.io.File
import java.util.concurrent.Executor
import java.util.UUID
import lv.zarin.timekeep.data.db.AppDatabase
import lv.zarin.timekeep.data.repo.RoomFavouriteLookRepository
import lv.zarin.timekeep.data.repo.RoomPresetRepository
import lv.zarin.timekeep.data.repo.RoomTimerRepository
import lv.zarin.timekeep.alarm.AndroidAlarmScheduler
import lv.zarin.timekeep.domain.TimerService
import lv.zarin.timekeep.domain.control.AllowAllControlPolicy
import lv.zarin.timekeep.domain.control.ControlPolicy
import lv.zarin.timekeep.domain.look.LookPicker
import lv.zarin.timekeep.domain.ports.AlarmScheduler
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
    val controlPolicy: ControlPolicy = AllowAllControlPolicy,
) {
    val appContext: Context = context.applicationContext

    /** In-memory (test) databases run Room work on the calling thread so tests that pump the looper are deterministic. */
    val database: AppDatabase by lazy {
        AppDatabase.build(appContext, inMemoryDb, clock, queryExecutor = if (inMemoryDb) Executor(Runnable::run) else null)
    }
    val timerRepository: TimerRepository by lazy { RoomTimerRepository(database.timerDao()) }
    val presetRepository: PresetRepository by lazy { RoomPresetRepository(database.presetDao()) }
    val favouriteLookRepository: FavouriteLookRepository by lazy { RoomFavouriteLookRepository(database.favouriteLookDao()) }

    val settingsRepository: SettingsRepository by lazy {
        val dataStore = settingsDataStore {
            if (inMemoryDb) {
                File(appContext.cacheDir, "settings-test-${UUID.randomUUID()}.preferences_pb")
            } else {
                appContext.preferencesDataStoreFile("settings")
            }
        }
        DataStoreSettingsRepository(dataStore)
    }

    val lookPicker = LookPicker()

    val alarmScheduler: AlarmScheduler by lazy { AndroidAlarmScheduler(appContext) }

    val timerService: TimerService by lazy {
        TimerService(timerRepository, presetRepository, alarmScheduler, clock, lookPicker, controlPolicy)
    }
}
