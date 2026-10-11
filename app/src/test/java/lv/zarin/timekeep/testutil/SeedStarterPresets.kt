package lv.zarin.timekeep.testutil

import kotlinx.coroutines.runBlocking
import lv.zarin.timekeep.AppContainer

fun seedStarterPresets(container: AppContainer) = runBlocking {
    container.presetSeeder.seedIfNeeded(container.starterPresetNames())
}
