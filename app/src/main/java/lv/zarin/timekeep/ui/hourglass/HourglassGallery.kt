package lv.zarin.timekeep.ui.hourglass

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.SandColor
import lv.zarin.timekeep.ui.theme.KidTimekeepTheme

/*
 * Design-time gallery only (not wired into navigation): every picture on every sand colour, plus a
 * large sample. Each small glass is half run so both the wiped top and the revealed bottom show.
 */

private val sampleLook = Look(PictureId.HEART, SandColor.LAVENDER, PictureId.STAR, SandColor.SKY)

@Composable
private fun Gallery() {
    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PictureId.entries.forEach { picture ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SandColor.entries.forEach { sand ->
                    Hourglass(
                        look = Look(picture, sand, picture, sand),
                        progress = { 0.5f },
                        running = false,
                        modifier = Modifier.width(48.dp),
                    )
                }
            }
        }
    }
}

@Preview(name = "Gallery 48 dp", widthDp = 460, heightDp = 1060, showBackground = true)
@Composable
private fun HourglassGalleryPreview() {
    KidTimekeepTheme { Surface { Gallery() } }
}

@Preview(
    name = "Gallery 48 dp, dark",
    widthDp = 460,
    heightDp = 1060,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HourglassGalleryDarkPreview() {
    KidTimekeepTheme { Surface { Gallery() } }
}

@Preview(name = "Sample 400 dp", widthDp = 420, heightDp = 540, showBackground = true)
@Composable
private fun HourglassLargePreview() {
    KidTimekeepTheme {
        Surface {
            Hourglass(sampleLook, progress = { 0.4f }, running = false, modifier = Modifier.padding(10.dp).width(400.dp))
        }
    }
}

@Preview(name = "Key progress values", widthDp = 440, heightDp = 160, showBackground = true)
@Composable
private fun HourglassProgressPreview() {
    KidTimekeepTheme {
        Surface {
            Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0f, 0.3f, 0.7f, 1f).forEach { p ->
                    Hourglass(sampleLook, progress = { p }, running = false, modifier = Modifier.width(100.dp))
                }
            }
        }
    }
}
