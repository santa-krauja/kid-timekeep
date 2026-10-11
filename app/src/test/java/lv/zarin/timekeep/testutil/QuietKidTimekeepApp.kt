package lv.zarin.timekeep.testutil

import lv.zarin.timekeep.KidTimekeepApp

class QuietKidTimekeepApp : KidTimekeepApp() {
    override fun reconcileOnLaunch(): Boolean = false
}
