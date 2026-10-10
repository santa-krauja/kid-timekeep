package lv.zarin.timekeep.domain

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import lv.zarin.timekeep.domain.timer.*
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class ModelsSerializationTest {
    private val look = Look(PictureId.HEART, SandColor.SKY, PictureId.ROCKET, SandColor.NIGHT)

    private fun timer(state: RunState) = Timer("id", "Tea", 60_000, look, null, state, 1, 2)

    @Test fun timerRoundTripsEveryState() {
        listOf(RunState.Running(5, 6), RunState.Paused(7), RunState.Finished(8)).forEach {
            val t = timer(it)
            assertEquals(t, Json.decodeFromString<Timer>(Json.encodeToString(t)))
        }
    }

    @Test fun presetRoundTrips() {
        listOf(null, look).forEach {
            val p = Preset("p", "Brush", 120_000, it, look, 0, 1, 2)
            assertEquals(p, Json.decodeFromString<Preset>(Json.encodeToString(p)))
        }
        val f = FavouriteLook("f", look, 1, 2)
        assertEquals(f, Json.decodeFromString<FavouriteLook>(Json.encodeToString(f)))
    }

    @Test fun durationValidity() {
        assertFalse(isValidDuration(9_999))
        assertTrue(isValidDuration(10_000))
        assertTrue(isValidDuration(14_400_000))
        assertFalse(isValidDuration(14_400_001))
    }
}
