package lv.zarin.timekeep.ui.hourglass

/** One falling grain of the stream (prototype STREAM entry). [sh] is the shade index 0..2. */
internal data class StreamGrain(val ph: Float, val per: Float, val dx: Float, val r: Float, val sh: Int, val wob: Float)

/** One splash grain where the stream lands on the pile (prototype SPLASH entry). */
internal data class SplashGrain(
    val ph: Float,
    val per: Float,
    val dir: Int,
    val reach: Float,
    val hgt: Float,
    val r: Float,
    val sh: Int,
)

/** Deterministic particle tables, ported from the prototype (LCG s = s·1664525 + 1013904223 mod 2^32). */
internal object SandParticles {
    private class Rng(seed: Int) {
        private var s = seed
        fun next(): Float {
            s = s * 1664525 + 1013904223 // Int overflow wraps mod 2^32
            return (s.toUInt().toDouble() / 4294967296.0).toFloat()
        }
    }

    val stream: List<StreamGrain> by lazy {
        val r = Rng(777)
        List(120) {
            val ph = r.next()
            val per = 600f + r.next() * 420f
            val dx = (r.next() - 0.5f) * 1.8f
            val rad = 0.16f + r.next() * 0.26f
            val sh = (r.next() * 3).toInt()
            val wob = r.next() * 40f
            StreamGrain(ph, per, dx, rad, sh, wob)
        }
    }

    val splash: List<SplashGrain> by lazy {
        val r = Rng(4242)
        List(34) {
            val ph = r.next()
            val per = 280f + r.next() * 380f
            val dir = if (r.next() < 0.5f) -1 else 1
            val reach = 1f + r.next() * 5f
            val hgt = 0.5f + r.next() * 2.2f
            val rad = 0.14f + r.next() * 0.2f
            val sh = (r.next() * 3).toInt()
            SplashGrain(ph, per, dir, reach, hgt, rad, sh)
        }
    }
}
