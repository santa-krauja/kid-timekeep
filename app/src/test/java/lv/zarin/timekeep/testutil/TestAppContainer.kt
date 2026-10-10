package lv.zarin.timekeep.testutil

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import lv.zarin.timekeep.AppContainer
import lv.zarin.timekeep.alarm.AndroidAlarmScheduler
import lv.zarin.timekeep.data.db.AppDatabase
import lv.zarin.timekeep.data.repo.RoomFavouriteLookRepository
import lv.zarin.timekeep.data.repo.RoomPresetRepository
import lv.zarin.timekeep.data.repo.RoomTimerRepository
import lv.zarin.timekeep.data.settings.DataStoreSeedFlagStore
import lv.zarin.timekeep.data.settings.DataStoreSettingsRepository
import lv.zarin.timekeep.data.settings.settingsDataStore
import lv.zarin.timekeep.domain.PresetSeeder
import lv.zarin.timekeep.domain.TimerService
import lv.zarin.timekeep.domain.control.AllowAllControlPolicy
import lv.zarin.timekeep.domain.control.ControlPolicy
import lv.zarin.timekeep.domain.look.LookPicker
import lv.zarin.timekeep.domain.ports.AlarmScheduler
import lv.zarin.timekeep.domain.ports.Clock
import lv.zarin.timekeep.domain.ports.FavouriteLookRepository
import lv.zarin.timekeep.domain.ports.PresetRepository
import lv.zarin.timekeep.domain.ports.SettingsRepository
import lv.zarin.timekeep.domain.ports.TimerRepository
import java.io.File
import java.util.UUID
import java.util.concurrent.Executor

fun inMemoryDatabase(context: Context): AppDatabase {
    val direct = Executor(Runnable::run)
    return Room.inMemoryDatabaseBuilder(context.applicationContext, AppDatabase::class.java)
        .setQueryExecutor(direct)
        .setTransactionExecutor(direct)
        .allowMainThreadQueries()
        .build()
}

class TestAppContainer(
    context: Context,
    override val clock: Clock = FakeClock(),
    override val controlPolicy: ControlPolicy = AllowAllControlPolicy,
    alarmScheduler: AlarmScheduler? = null,
) : AppContainer {
    override val appContext: Context = context.applicationContext
    override val lookPicker = LookPicker()
    override val alarmScheduler: AlarmScheduler = alarmScheduler ?: AndroidAlarmScheduler(appContext)

    private val database: AppDatabase by lazy { inMemoryDatabase(appContext) }
    private val settingsDataStore: DataStore<Preferences> by lazy {
        settingsDataStore { File(appContext.cacheDir, "settings-test-${UUID.randomUUID()}.preferences_pb") }
    }

    override val timerRepository: TimerRepository = RoomTimerRepository(database.timerDao())
    override val presetRepository: PresetRepository = RoomPresetRepository(database.presetDao())
    override val favouriteLookRepository: FavouriteLookRepository =
        RoomFavouriteLookRepository(database.favouriteLookDao())
    override val settingsRepository: SettingsRepository = DataStoreSettingsRepository(settingsDataStore)
    override val presetSeeder = PresetSeeder(presetRepository, DataStoreSeedFlagStore(settingsDataStore), clock)
    override val timerService =
        TimerService(timerRepository, presetRepository, this.alarmScheduler, clock, lookPicker, controlPolicy)
}
