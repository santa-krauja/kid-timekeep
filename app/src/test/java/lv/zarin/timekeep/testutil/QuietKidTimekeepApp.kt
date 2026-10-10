package lv.zarin.timekeep.testutil

import lv.zarin.timekeep.KidTimekeepApp

/**
 * Default test application (set in `robolectric.properties`): skips the launch reconcile, so no
 * background coroutine touches the container a test swaps in right after the application was created.
 */
class QuietKidTimekeepApp : KidTimekeepApp() {
    override fun reconcileOnLaunch(): Boolean = false
}
