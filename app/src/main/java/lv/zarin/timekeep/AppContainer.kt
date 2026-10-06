package lv.zarin.timekeep

import android.content.Context
import lv.zarin.timekeep.domain.ports.Clock
import lv.zarin.timekeep.domain.ports.SystemClock

/** Hand-wired dependencies. Fields are added by later tasks. */
@Suppress("unused")
class AppContainer(
    context: Context,
    val inMemoryDb: Boolean = false,
    val clock: Clock = SystemClock,
) {
    val appContext: Context = context.applicationContext
}
