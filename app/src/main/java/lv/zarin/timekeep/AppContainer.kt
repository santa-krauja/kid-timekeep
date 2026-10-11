package lv.zarin.timekeep

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import lv.zarin.timekeep.alarm.AndroidAlarmScheduler
import lv.zarin.timekeep.data.db.AppDatabase
import lv.zarin.timekeep.data.repo.RoomFavouriteLookRepository
import lv.zarin.timekeep.data.repo.RoomPresetRepository
import lv.zarin.timekeep.data.repo.RoomTimerRepository
import lv.zarin.timekeep.data.settings.DataStoreSeedFlagStore
import lv.zarin.timekeep.data.settings.DataStoreSettingsRepository
import lv.zarin.timekeep.data.settings.settingsDataStore
import lv.zarin.timekeep.domain.PresetSeeder
import lv.zarin.timekeep.domain.StarterPresetNames
import lv.zarin.timekeep.domain.TimerService
import lv.zarin.timekeep.domain.control.AllowAllControlPolicy
import lv.zarin.timekeep.domain.control.ControlPolicy
import lv.zarin.timekeep.domain.look.LookPicker
import lv.zarin.timekeep.domain.ports.AlarmScheduler
import lv.zarin.timekeep.domain.ports.Clock
import lv.zarin.timekeep.domain.ports.FavouriteLookRepository
import lv.zarin.timekeep.domain.ports.PresetRepository
import lv.zarin.timekeep.domain.ports.SettingsRepository
import lv.zarin.timekeep.domain.ports.SystemClock
import lv.zarin.timekeep.domain.ports.TimerRepository

interface AppContainer {
    val appContext: Context
    val clock: Clock
    val controlPolicy: ControlPolicy
    val timerRepository: TimerRepository
    val presetRepository: PresetRepository
    val favouriteLookRepository: FavouriteLookRepository
    val settingsRepository: SettingsRepository
    val presetSeeder: PresetSeeder
    val lookPicker: LookPicker
    val alarmScheduler: AlarmScheduler
    val timerService: TimerService

    fun starterPresetNames() = StarterPresetNames(
        teeth = appContext.getString(R.string.seed_preset_teeth),
        dressed = appContext.getString(R.string.seed_preset_dressed),
        reading = appContext.getString(R.string.seed_preset_reading),
    )
}

class DefaultAppContainer(context: Context) : AppContainer {
    override val appContext: Context = context.applicationContext
    override val clock: Clock = SystemClock
    override val controlPolicy: ControlPolicy = AllowAllControlPolicy
    override val lookPicker = LookPicker()

    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }
    private val settingsDataStore: DataStore<Preferences> by lazy {
        settingsDataStore { appContext.preferencesDataStoreFile("settings") }
    }

    override val timerRepository: TimerRepository by lazy { RoomTimerRepository(database.timerDao()) }
    override val presetRepository: PresetRepository by lazy { RoomPresetRepository(database.presetDao()) }
    override val favouriteLookRepository: FavouriteLookRepository by lazy {
        RoomFavouriteLookRepository(database.favouriteLookDao())
    }
    override val settingsRepository: SettingsRepository by lazy { DataStoreSettingsRepository(settingsDataStore) }
    override val presetSeeder: PresetSeeder by lazy {
        PresetSeeder(presetRepository, DataStoreSeedFlagStore(settingsDataStore), clock)
    }
    override val alarmScheduler: AlarmScheduler by lazy { AndroidAlarmScheduler(appContext) }
    override val timerService: TimerService by lazy {
        TimerService(timerRepository, presetRepository, alarmScheduler, clock, lookPicker, controlPolicy)
    }
}
