package lv.zarin.timekeep.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import lv.zarin.timekeep.data.db.AppDatabase
import lv.zarin.timekeep.data.repo.RoomFavouriteLookRepository
import lv.zarin.timekeep.data.repo.RoomTimerRepository
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.SandColor
import lv.zarin.timekeep.domain.timer.Timer
import lv.zarin.timekeep.testutil.FakeClock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EnumColumnsTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.build(context, inMemory = true, clock = FakeClock(1_000L))
    }

    @After
    fun tearDown() = db.close()

    private fun insertRawTimer(id: String, state: String, topPicture: String, topSand: String) {
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO timers (id, name, durationMs, look_topPicture, look_topSand, look_bottomPicture, " +
                "look_bottomSand, presetId, stateType, runningSinceMs, elapsedMs, finishedAtMs, createdAtMs, updatedAtMs) " +
                "VALUES ('$id', 'n', 1000, '$topPicture', '$topSand', 'CAT', 'MINT', NULL, '$state', NULL, NULL, 5, 1, 2)",
        )
    }

    @Test
    fun everyPictureAndSandRoundTrips() = runTest {
        val repo = RoomTimerRepository(db.timerDao())
        val sands = SandColor.entries
        PictureId.entries.forEachIndexed { i, picture ->
            val look = Look(picture, sands[i % sands.size], PictureId.entries[(i + 1) % PictureId.entries.size], sands[(i + 1) % sands.size])
            val timer = Timer("t$i", "n", 1_000L, look, null, RunState.Paused(10L), 1L, 2L)
            repo.upsert(timer)
            assertEquals(timer, repo.get(timer.id))
        }
        SandColor.entries.forEach { sand ->
            val look = Look(PictureId.HEART, sand, PictureId.STAR, sand)
            val timer = Timer("s${sand.name}", "n", 1_000L, look, null, RunState.Finished(9L), 1L, 2L)
            repo.upsert(timer)
            assertEquals(timer, repo.get(timer.id))
        }
    }

    @Test
    fun unknownStateNameReadsAsFinished() = runTest {
        insertRawTimer("x", state = "EXPLODED", topPicture = "CAT", topSand = "SKY")
        assertEquals(RunState.Finished(5L), RoomTimerRepository(db.timerDao()).get("x")!!.state)
    }

    @Test
    fun unknownPictureAndSandNamesReadAsFallbacks() = runTest {
        insertRawTimer("y", state = "FINISHED", topPicture = "DRAGON", topSand = "NEON")
        val look = RoomTimerRepository(db.timerDao()).get("y")!!.look
        assertEquals(PictureId.HEART, look.top)
        assertEquals(SandColor.LAVENDER, look.topSand)
        assertEquals(PictureId.CAT, look.bottom)
        assertEquals(SandColor.MINT, look.bottomSand)
    }

    @Test
    fun favouriteLookIsFoundByEnumColumns() = runTest {
        val repo = RoomFavouriteLookRepository(db.favouriteLookDao())
        val look = Look(PictureId.DOG, SandColor.PEACH, PictureId.SUN, SandColor.SKY)
        val first = repo.add(look, 1L)
        assertEquals(first, repo.add(look, 1L))
    }
}
