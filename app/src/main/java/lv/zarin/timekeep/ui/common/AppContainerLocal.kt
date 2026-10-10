package lv.zarin.timekeep.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import lv.zarin.timekeep.AppContainer
import lv.zarin.timekeep.KidTimekeepApp

@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as KidTimekeepApp).container
