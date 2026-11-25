package lv.zarin.timekeep.ui.common

import androidx.compose.runtime.staticCompositionLocalOf

interface LockScreenAccess {
    fun acquire(timerId: String)

    fun release(timerId: String)
}

val LocalLockScreenAccess = staticCompositionLocalOf<LockScreenAccess> {
    object : LockScreenAccess {
        override fun acquire(timerId: String) = Unit

        override fun release(timerId: String) = Unit
    }
}
