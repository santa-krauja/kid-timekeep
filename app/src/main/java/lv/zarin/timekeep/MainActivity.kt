package lv.zarin.timekeep

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.compose.rememberNavController
import lv.zarin.timekeep.ui.nav.AppNavHost
import lv.zarin.timekeep.ui.theme.KidTimekeepTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KidTimekeepTheme {
                AppNavHost(navController = rememberNavController())
            }
        }
    }
}
