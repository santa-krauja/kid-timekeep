package lv.zarin.timekeep.domain.hourglass

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SandLevelSolverTest {
    @Test
    fun fullAtZero() {
        val l = SandLevelSolver.levels(0f)
        assertEquals(BulbShape.FILL0, l.topLevel!!, 0.01f)
        assertNull(l.bottomLevel)
    }

    @Test
    fun emptyAtOne() {
        val l = SandLevelSolver.levels(1f)
        assertNull(l.topLevel)
        assertNotNull(l.bottomLevel)
    }

    @Test
    fun areasMatchProgress() {
        val total = SandLevelSolver.totalSand
        for (p in listOf(0.1f, 0.25f, 0.5f, 0.75f, 0.9f)) {
            val l = SandLevelSolver.levels(p)
            val top = SandLevelSolver.topArea(l.topLevel!!, l.dip)
            val bot = SandLevelSolver.bottomArea(l.bottomLevel!!, l.mound)
            assertTrue("top at $p: $top", abs(top - (1 - p) * total) <= 0.005f * total)
            assertTrue("bottom at $p: $bot", abs(bot - p * total) <= 0.005f * total)
        }
    }

    @Test
    fun levelsMonotonicAcrossFullRange() {
        val funnelDipWobbleTolerance = 0.1f
        var prevTop = Float.NEGATIVE_INFINITY
        var prevBot = Float.POSITIVE_INFINITY
        for (i in 0..4000) {
            val l = SandLevelSolver.levels(i / 4000f)
            l.topLevel?.let {
                assertTrue("top at $i", it >= prevTop - funnelDipWobbleTolerance)
                prevTop = it
            }
            l.bottomLevel?.let {
                assertTrue("bottom at $i", it <= prevBot + funnelDipWobbleTolerance)
                prevBot = it
            }
        }
    }

    @Test
    fun halfWidthIsNeckInNeck() {
        assertEquals(BulbShape.NECK, BulbShape.halfWidth(BulbShape.TOP1), 1e-6f)
        assertEquals(BulbShape.NECK, BulbShape.halfWidth(64f), 1e-6f)
        assertEquals(BulbShape.NECK, BulbShape.halfWidth(BulbShape.BOT0), 1e-6f)
    }
}
