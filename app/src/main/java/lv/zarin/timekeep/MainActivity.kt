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
        val intentAlreadyHandled = savedInstanceState != null
        if (!intentAlreadyHandled) takeTimerId(intent)
        val container = (application as KidTimekeepApp).container
        // Re-runs on the recreation after a language change, so channel names follow the new language.
        Notifications.ensureChannels(this)
        enableEdgeToEdge()
        setContent {
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = Settings())
            val dark = isDarkTheme(settings.themeMode)
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                        detectDarkMode = { dark }
                    ),
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                        detectDarkMode = { dark }
                    )
                )
                onDispose { }
            }
            KidTimekeepTheme(themeMode = settings.themeMode) {
                AppNavHost(
                    navController = rememberNavController(),
                    startTimerId = startTimerId,
                    onStartTimerConsumed = { startTimerId = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        takeTimerId(intent)
    }

    private fun takeTimerId(intent: Intent?) {
        val id = intent?.getStringExtra(EXTRA_TIMER_ID) ?: return
        intent.removeExtra(EXTRA_TIMER_ID)
        startTimerId = id
    }

    companion object {
        const val EXTRA_TIMER_ID = "lv.zarin.timekeep.extra.TIMER_ID"
    }
}
