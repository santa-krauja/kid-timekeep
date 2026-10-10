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

    @Test
    fun goldenFirstValuesPinTheRngPort() {
        // Independently computed LCG values: seed 777 -> 0.5371962, 0.7553924; seed 4242 -> 0.8800654, 0.0439387.
        assertEquals(0.5371962f, SandParticles.stream[0].ph, 1e-6f)
        assertEquals(600f + 0.75539244f * 420f, SandParticles.stream[0].per, 1e-3f)
        assertEquals(0.8800654f, SandParticles.splash[0].ph, 1e-6f)
        assertEquals(280f + 0.04393873f * 380f, SandParticles.splash[0].per, 1e-3f)
    }
}
