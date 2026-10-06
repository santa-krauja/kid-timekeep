package lv.zarin.timekeep.ui.hourglass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SandParticlesTest {
    @Test
    fun particlesAreDeterministic() {
        assertEquals(SandParticles.stream, SandParticles.stream)
        assertEquals(SandParticles.splash, SandParticles.splash)
        assertEquals(SandParticles.stream.first(), SandParticles.stream.first().copy())
    }

    @Test
    fun streamCount120SplashCount34() {
        assertEquals(120, SandParticles.stream.size)
        assertEquals(34, SandParticles.splash.size)
    }

    @Test
    fun fieldsAreInPrototypeRanges() {
        assertTrue(SandParticles.stream.all { it.per in 600f..1020f && it.sh in 0..2 && it.r in 0.16f..0.42f })
        assertTrue(SandParticles.splash.all { (it.dir == 1 || it.dir == -1) && it.per in 280f..660f })
    }
}
