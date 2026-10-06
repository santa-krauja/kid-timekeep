package lv.zarin.timekeep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.SandColor
import lv.zarin.timekeep.ui.hourglass.Hourglass
import lv.zarin.timekeep.ui.theme.KidTimekeepTheme

/**
 * Debug-only showcase: a 2x2 grid of hourglasses at p = 0, 0.3, 0.7, 1 (stream runs on 0.3 and 0.7);
 * extra `--ez many true` shows 8 small running hourglasses.
 * `adb shell am start -n lv.zarin.timekeep/.HourglassShowcaseActivity`
 */
class HourglassShowcaseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val look = Look(PictureId.HEART, SandColor.LAVENDER, PictureId.STAR, SandColor.SKY)
        val many = intent.getBooleanExtra("many", false)
        setContent {
            KidTimekeepTheme {
                Surface(Modifier.fillMaxSize()) {
                    if (many) {
                        Column(
                            Modifier.safeDrawingPadding().padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(listOf(0.1f, 0.25f, 0.4f, 0.55f), listOf(0.65f, 0.75f, 0.85f, 0.95f)).forEach { row ->
                                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { p ->
                                        Hourglass(
                                            look = look,
                                            progress = { p },
                                            running = true,
                                            modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        var flip by remember { mutableIntStateOf(0) }
                        var firstP by remember { mutableFloatStateOf(0.3f) }
                        Column(
                            Modifier.safeDrawingPadding().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            // Debug: flip the first cell (p = 0.3) and reset its progress to 0.
                            Button(onClick = { firstP = 0f; flip++ }) { Text("Flip") }
                            listOf(listOf(firstP, 0.3f), listOf(0.7f, 1f)).forEachIndexed { r, row ->
                                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    row.forEachIndexed { c, p ->
                                        val isFlipCell = r == 0 && c == 0
                                        Hourglass(
                                            look = look,
                                            progress = if (isFlipCell) ({ firstP }) else ({ p }),
                                            running = p > 0f && p < 1f,
                                            modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                                            flipTrigger = if (isFlipCell) flip else 0,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
