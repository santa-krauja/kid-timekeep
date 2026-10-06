package lv.zarin.timekeep

import android.content.Context

/** Hand-wired dependencies. Fields are added by later tasks. */
@Suppress("unused")
class AppContainer(
    context: Context,
    val inMemoryDb: Boolean = false,
) {
    val appContext: Context = context.applicationContext
}
