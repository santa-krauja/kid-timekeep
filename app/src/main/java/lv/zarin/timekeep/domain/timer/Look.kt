package lv.zarin.timekeep.domain.timer

import kotlinx.serialization.Serializable

@Serializable
enum class PictureId(val codepoint: String, val dominantArgb: Long) {
    HEART("2764", 0xFFE5484D),
    STAR("2b50", 0xFFF5B800),
    BLOSSOM("1f338", 0xFFF7A8C8),
    SMILEY("1f60a", 0xFFFFCC33),
    SUN("2600", 0xFFFFB000),
    MOON("1f319", 0xFFFFD75E),
    FISH("1f41f", 0xFF4AA8FF),
    CAT("1f431", 0xFFF2B65A),
    DOG("1f436", 0xFFC68B59),
    CAR("1f697", 0xFFE5484D),
    TREE("1f333", 0xFF3A9D5D),
    BUTTERFLY("1f98b", 0xFF4AA8FF),
    APPLE("1f34e", 0xFFE5484D),
    RAINBOW("1f308", 0xFFFF6B6B),
    UNICORN("1f984", 0xFFF0E6FF),
    ROCKET("1f680", 0xFFB0B8C8),
}

@Serializable
enum class SandColor(val argb: Long) {
    LAVENDER(0xFFC9B8F0),
    SKY(0xFF9FD3F5),
    MINT(0xFFA8E6C4),
    PEACH(0xFFFFC9A8),
    SAND(0xFFF1DCA7),
    PINK(0xFFFFB3CF),
    NIGHT(0xFF3C4A7A),
    LEMON(0xFFFFF0A0),
}

@Serializable
data class Look(
    val top: PictureId,
    val topSand: SandColor,
    val bottom: PictureId,
    val bottomSand: SandColor,
)
