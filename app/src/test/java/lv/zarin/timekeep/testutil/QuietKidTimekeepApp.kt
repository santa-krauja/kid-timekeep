package lv.zarin.timekeep.testutil

import lv.zarin.timekeep.KidTimekeepApp

/**
 * Test application (`@Config(application = QuietKidTimekeepApp::class)`): skips the launch reconcile, so no
 * background coroutine touches the container a test swaps in right after the application was created.
 */
class QuietKidTimekeepApp : KidTimekeepApp() {
    override fun reconcileOnLaunch(): Boolean = false
}
