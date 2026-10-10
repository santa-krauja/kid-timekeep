package lv.zarin.timekeep.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import lv.zarin.timekeep.domain.ports.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    primaryContainer = Color(0xFF2C2850),
    onPrimaryContainer = Color(0xFFE9E6FB),
    secondary = Color(0xFFA39D93),
    secondaryContainer = Color(0xFF282624),
    onSecondaryContainer = DarkOnSurface,
    tertiary = DarkPrimary,
    onSurfaceVariant = Color(0xFFA39D93),
    onPrimary = DarkBackground,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurface = DarkOnSurface,
    onBackground = DarkOnSurface,
    outline = DarkOutline,
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    primaryContainer = Color(0xFFE9E6FB),
    onPrimaryContainer = Color(0xFF2A2160),
    secondary = Color(0xFF6B665E),
    secondaryContainer = Color(0xFFEFEDE8),
    onSecondaryContainer = LightOnSurface,
    tertiary = LightPrimary,
    onSurfaceVariant = Color(0xFF6B665E),
    onPrimary = Color.White,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurface = LightOnSurface,
    onBackground = LightOnSurface,
    outline = LightOutline,
)

private val KidShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Whether [themeMode] resolves to the dark scheme, given the system setting. */
@Composable
fun isDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun KidTimekeepTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = isDarkTheme(themeMode)
    MaterialTheme(
        colorScheme = if (dark) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = KidShapes,
    ) {
        CompositionLocalProvider(
            LocalMinimumInteractiveComponentSize provides 56.dp,
            content = content,
        )
    }
}
