package lv.zarin.timekeep.domain.look

import kotlin.math.sqrt
import kotlin.random.Random
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.SandColor

class LookPicker(private val random: Random = Random.Default) {

    fun pick(avoid: Set<Look> = emptySet()): Look {
        val pictures = PictureId.entries
        val sands = SandColor.entries
        repeat(MAX_DRAWS) {
            val look = Look(
                top = pictures[random.nextInt(pictures.size)],
                topSand = sands[random.nextInt(sands.size)],
                bottom = pictures[random.nextInt(pictures.size)],
                bottomSand = sands[random.nextInt(sands.size)],
            )
            if (isGoodLook(look) && look !in avoid) return look
        }
        return goodLooks.firstOrNull { it !in avoid }
            ?: goodLooks.firstOrNull()
            ?: FALLBACK
    }

    companion object {
        const val PICTURE_SAND_MIN_DISTANCE = 110.0
        const val SAND_SAND_MIN_DISTANCE = 60.0
        private const val MAX_DRAWS = 1000

        private val FALLBACK = Look(PictureId.HEART, SandColor.LAVENDER, PictureId.STAR, SandColor.SKY)

        /** Euclidean distance over the R, G, B bytes of two ARGB values (alpha ignored). */
        fun rgbDistance(a: Long, b: Long): Double {
            val dr = ((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)
            val dg = ((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)
            val db = (a and 0xFF) - (b and 0xFF)
            return sqrt((dr * dr + dg * dg + db * db).toDouble())
        }

        fun contrastOk(picture: PictureId, sand: SandColor): Boolean =
            rgbDistance(picture.dominantArgb, sand.argb) > PICTURE_SAND_MIN_DISTANCE

        fun isGoodLook(look: Look): Boolean =
            look.top != look.bottom &&
                look.topSand != look.bottomSand &&
                contrastOk(look.top, look.topSand) &&
                contrastOk(look.bottom, look.bottomSand) &&
                rgbDistance(look.topSand.argb, look.bottomSand.argb) > SAND_SAND_MIN_DISTANCE

        private val goodLooks: List<Look> by lazy {
            buildList {
                for (t in PictureId.entries) for (ts in SandColor.entries)
                    for (b in PictureId.entries) for (bs in SandColor.entries) {
                        val l = Look(t, ts, b, bs)
                        if (isGoodLook(l)) add(l)
                    }
            }
        }
    }
}
