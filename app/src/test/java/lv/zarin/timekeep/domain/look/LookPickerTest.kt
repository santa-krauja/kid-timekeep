package lv.zarin.timekeep.domain.look

import kotlin.random.Random
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.SandColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LookPickerTest {
    @Test
    fun everyPickedLookIsGood() {
        val picker = LookPicker(Random(42))
        repeat(1000) {
            val look = picker.pick()
            assertTrue("Not good: $look", LookPicker.isGoodLook(look))
        }
    }

    @Test
    fun avoidsGivenLooks() {
        val first = LookPicker(Random(7)).pick()
        val picker = LookPicker(Random(7))
        repeat(200) {
            assertNotEquals(first, picker.pick(avoid = setOf(first)))
        }
    }

    @Test
    fun sameSeedSameSequence() {
        val a = LookPicker(Random(99))
        val b = LookPicker(Random(99))
        repeat(50) { assertEquals(a.pick(), b.pick()) }
    }

    @Test
    fun fallsBackWhenEverythingAvoided() {
        val all = allGoodLooks()
        assertTrue(all.isNotEmpty())
        val look = LookPicker(Random(1)).pick(avoid = all)
        assertTrue(LookPicker.isGoodLook(look))
    }

    @Test
    fun moonOnSandIsRejected() {
        val d = LookPicker.rgbDistance(PictureId.MOON.dominantArgb, SandColor.SAND.argb)
        assertTrue("distance=$d should be < 110", d < LookPicker.PICTURE_SAND_MIN_DISTANCE)
        assertFalse(LookPicker.contrastOk(PictureId.MOON, SandColor.SAND))
    }

    private fun allGoodLooks(): Set<Look> = buildSet {
        for (t in PictureId.entries) for (ts in SandColor.entries)
            for (b in PictureId.entries) for (bs in SandColor.entries) {
                val l = Look(t, ts, b, bs)
                if (LookPicker.isGoodLook(l)) add(l)
            }
    }
}
