package lv.zarin.timekeep

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import lv.zarin.timekeep.alarm.Notifications
import lv.zarin.timekeep.domain.ports.Settings
import lv.zarin.timekeep.ui.nav.AppNavHost
import lv.zarin.timekeep.ui.theme.KidTimekeepTheme
import lv.zarin.timekeep.ui.theme.isDarkTheme

class MainActivity : AppCompatActivity() {
    private var startTimerId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // A recreated activity gets the original intent again; it was handled already.
        if (savedInstanceState == null) takeTimerId(intent)
        val container = (application as KidTimekeepApp).container
        // Runs again after the recreation caused by a language change, so channel names follow the new language.
        Notifications.ensureChannels(this)
        enableEdgeToEdge()
        setContent {
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = Settings())
            val dark = isDarkTheme(settings.themeMode)
            DisposableEffect(dark) {
                // Transparent bars; icon contrast follows the resolved theme, not the system.
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT, Color.TRANSPARENT, detectDarkMode = { dark },
                    ),
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT, Color.TRANSPARENT, detectDarkMode = { dark },
                    ),
                )
                onDispose { }
            }
            KidTimekeepTheme(themeMode = settings.themeMode) {
                AppNavHost(
                    navController = rememberNavController(),
                    startTimerId = startTimerId,
                    onStartTimerConsumed = { startTimerId = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        takeTimerId(intent)
    }

    /** Reads the notification deep-link extra and removes it so it is only ever handled once. */
    private fun takeTimerId(intent: Intent?) {
        val id = intent?.getStringExtra(EXTRA_TIMER_ID) ?: return
        intent.removeExtra(EXTRA_TIMER_ID)
        startTimerId = id
    }

    companion object {
        const val EXTRA_TIMER_ID = "lv.zarin.timekeep.extra.TIMER_ID"
    }
}
