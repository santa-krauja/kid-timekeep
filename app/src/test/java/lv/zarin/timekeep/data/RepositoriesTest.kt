package lv.zarin.timekeep.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import lv.zarin.timekeep.data.db.AppDatabase
import lv.zarin.timekeep.data.repo.RoomFavouriteLookRepository
import lv.zarin.timekeep.data.repo.RoomPresetRepository
import lv.zarin.timekeep.data.repo.RoomTimerRepository
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.Preset
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.SandColor
import lv.zarin.timekeep.domain.timer.Timer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RepositoriesTest {
    private lateinit var db: AppDatabase
    private val look = Look(PictureId.CAT, SandColor.MINT, PictureId.FISH, SandColor.SKY)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.build(context, inMemory = true)
    }

    @After
    fun tearDown() = db.close()

    private fun timer(id: String, state: RunState) = Timer(
        id = id, name = "T $id", durationMs = 300_000L, look = look, presetId = "p",
        state = state, createdAtMs = 10L, updatedAtMs = 20L,
    )

    private fun preset(id: String, order: Int, created: Long, pinned: Look? = null) = Preset(
        id = id, name = id, durationMs = 60_000L, pinnedLook = pinned, lastLook = null,
        sortOrder = order, createdAtMs = created, updatedAtMs = created,
    )

    @Test
    fun timerRoundTripsAllStates() = runTest {
        val repo = RoomTimerRepository(db.timerDao())
        val timers = listOf(
            timer("a", RunState.Running(sinceMs = 100L, elapsedBeforeMs = 50L)),
            timer("b", RunState.Paused(elapsedMs = 7_000L)),
            timer("c", RunState.Finished(finishedAtMs = 999L)),
        )
        timers.forEach { repo.upsert(it) }
        timers.forEach { assertEquals(it, repo.get(it.id)) }
        assertEquals(timers.toSet(), repo.getAll().toSet())
        assertEquals(timers.toSet(), repo.observeAll().first().toSet())
    }

    @Test
    fun presetPinnedLookNullRoundTrips() = runTest {
        val repo = RoomPresetRepository(db.presetDao())
        val unpinned = preset("u", 10, 1L)
        val pinned = preset("p", 11, 2L, pinned = look).copy(lastLook = look)
        repo.upsert(unpinned)
        repo.upsert(pinned)
        assertNull(repo.get("u")!!.pinnedLook)
        assertEquals(unpinned, repo.get("u"))
        assertEquals(pinned, repo.get("p"))
    }

    @Test
    fun presetsOrderedBySortOrder() = runTest {
        val repo = RoomPresetRepository(db.presetDao())
        repo.upsert(preset("x", 20, 1L))
        repo.upsert(preset("y", 20, 0L))
        repo.upsert(preset("z", 15, 5L))
        val ids = repo.observeAll().first().map { it.id }.filter { it in setOf("x", "y", "z") }
        assertEquals(listOf("z", "y", "x"), ids)
    }

    @Test
    fun favouriteAddIsIdempotent() = runTest {
        val repo = RoomFavouriteLookRepository(db.favouriteLookDao())
        val first = repo.add(look, 5L)
        val second = repo.add(look.copy(), 9L)
        assertEquals(first, second)
        assertEquals(1, repo.observeAll().first().size)
        repo.add(look.copy(topSand = SandColor.PINK), 10L)
        assertEquals(2, repo.observeAll().first().size)
    }

    @Test
    fun deleteTimerRemovesIt() = runTest {
        val repo = RoomTimerRepository(db.timerDao())
        repo.upsert(timer("a", RunState.Paused(1L)))
        repo.delete("a")
        assertNull(repo.get("a"))
        assertEquals(emptyList<Timer>(), repo.getAll())
    }
}
