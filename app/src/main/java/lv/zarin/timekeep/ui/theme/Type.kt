package lv.zarin.timekeep.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private val Base = Typography()

private fun TextStyle.scaled(factor: Float, weight: FontWeight? = null): TextStyle = copy(
    fontSize = fontSize.scale(factor),
    lineHeight = lineHeight.scale(factor),
    fontWeight = weight ?: fontWeight,
)

private fun TextUnit.scale(factor: Float): TextUnit = if (isSp) (value * factor).sp else this

/** Material 3 defaults, body and label about 10% larger, titles bold. */
val Typography = Typography(
    titleLarge = Base.titleLarge.scaled(1f, FontWeight.Bold),
    titleMedium = Base.titleMedium.scaled(1f, FontWeight.Bold),
    titleSmall = Base.titleSmall.scaled(1f, FontWeight.Bold),
    bodyLarge = Base.bodyLarge.scaled(1.1f),
    bodyMedium = Base.bodyMedium.scaled(1.1f),
    bodySmall = Base.bodySmall.scaled(1.1f),
    labelLarge = Base.labelLarge.scaled(1.1f),
    labelMedium = Base.labelMedium.scaled(1.1f),
    labelSmall = Base.labelSmall.scaled(1.1f),
)
