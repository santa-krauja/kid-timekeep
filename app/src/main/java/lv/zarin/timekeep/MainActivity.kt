package lv.zarin.timekeep

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import lv.zarin.timekeep.domain.ports.Settings
import lv.zarin.timekeep.ui.nav.AppNavHost
import lv.zarin.timekeep.ui.theme.KidTimekeepTheme
import lv.zarin.timekeep.ui.theme.isDarkTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as KidTimekeepApp).container
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
                AppNavHost(navController = rememberNavController())
            }
        }
    }
}
