package lv.zarin.timekeep.domain

import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import lv.zarin.timekeep.domain.control.AllowAllControlPolicy
import lv.zarin.timekeep.domain.control.Control
import lv.zarin.timekeep.domain.control.ControlPolicy
import lv.zarin.timekeep.domain.look.LookPicker
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.MAX_DURATION_MS
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.Preset
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.SandColor
import lv.zarin.timekeep.testutil.FakeClock
import lv.zarin.timekeep.testutil.InMemoryPresetRepository
import lv.zarin.timekeep.testutil.InMemoryTimerRepository
import lv.zarin.timekeep.testutil.RecordingAlarmScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerServiceTest {
    private val clock = FakeClock(1_000_000L)
    private val timers = InMemoryTimerRepository()
    private val presets = InMemoryPresetRepository()
    private val alarms = RecordingAlarmScheduler()
    private var counter = 0

    private fun service(policy: ControlPolicy = AllowAllControlPolicy) =
        TimerService(timers, presets, alarms, clock, LookPicker(Random(7)), policy, ids = { "id${counter++}" })

    private val pinned = Look(PictureId.HEART, SandColor.NIGHT, PictureId.STAR, SandColor.LEMON)

    private fun preset(id: String = "p1", pinnedLook: Look? = null, durationMs: Long = 60_000L) =
        Preset(id, "Tea", durationMs, pinnedLook, null, 0, 0L, 0L)

    @Test
    fun startFromPresetPicksFreshLookEachTime() = runTest {
        presets.upsert(preset())
        val s = service()
        val a = s.startFromPreset("p1")
        assertEquals(a.look, presets.get("p1")!!.lastLook)
        val b = s.startFromPreset("p1")
        assertNotEquals(a.look, b.look)
        assertEquals(b.look, presets.get("p1")!!.lastLook)
        assertEquals("p1", b.presetId)
        assertEquals("Tea", b.name)
    }

    @Test
    fun pinnedPresetAlwaysUsesPinnedLook() = runTest {
        presets.upsert(preset(pinnedLook = pinned))
        val s = service()
        assertEquals(pinned, s.startFromPreset("p1").look)
        assertEquals(pinned, s.startFromPreset("p1").look)
    }

    @Test(expected = IllegalArgumentException::class)
    fun startFromUnknownPresetThrows() = runTest { service().startFromPreset("nope") }

    @Test
    fun startSchedulesAlarmAtFinish() = runTest {
        presets.upsert(preset(durationMs = 90_000L))
        val t = service().startFromPreset("p1")
        assertEquals(clock.now + 90_000L, alarms.scheduled[t.id])
        assertEquals(t, timers.get(t.id))
    }

    @Test
    fun startOneOffValidatesAndTrims() = runTest {
        val s = service()
        val t = s.startOneOff("  Bath  ", 30_000L, pinned)
        assertEquals("Bath", t.name)
        assertNull(t.presetId)
        assertEquals(clock.now + 30_000L, alarms.scheduled[t.id])
        assertTrue(runCatching { s.startOneOff("x", 5_000L, pinned) }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { s.startOneOff("x", MAX_DURATION_MS + 1, pinned) }.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun suggestLookAvoidsNonFinishedTimers() = runTest {
        val s = service()
        val running = s.startOneOff("a", 60_000L, pinned)
        val paused = s.startOneOff("b", 60_000L, s.suggestLook())
        s.pause(paused.id)
        val finished = s.startOneOff("c", 10_000L, s.suggestLook())
        clock.advance(10_000L)
        s.finishIfOverdue(finished.id)
        repeat(50) {
            val l = s.suggestLook()
            assertNotEquals(running.look, l)
            assertNotEquals(paused.look, l)
        }
    }

    @Test
    fun pauseCancelsAlarm() = runTest {
        val s = service()
        val t = s.startOneOff("a", 60_000L, pinned)
        clock.advance(10_000L)
        s.pause(t.id)
        assertEquals(RunState.Paused(10_000L), timers.get(t.id)!!.state)
        assertTrue(t.id !in alarms.scheduled)
        assertTrue(t.id in alarms.cancelled)
    }

    @Test
    fun resumeReschedules() = runTest {
        val s = service()
        val t = s.startOneOff("a", 60_000L, pinned)
        clock.advance(10_000L)
        s.pause(t.id)
        clock.advance(100_000L)
        s.resume(t.id)
        assertEquals(clock.now + 50_000L, alarms.scheduled[t.id])
    }

    @Test
    fun restartKeepsLookAndReschedules() = runTest {
        val s = service()
        val t = s.startOneOff("a", 60_000L, pinned)
        clock.advance(70_000L)
        s.finishIfOverdue(t.id)
        s.restart(t.id)
        val r = timers.get(t.id)!!
        assertEquals(pinned, r.look)
        assertTrue(r.state is RunState.Running)
        assertEquals(clock.now + 60_000L, alarms.scheduled[t.id])
    }

    @Test
    fun addMinuteExtendsAndReschedules() = runTest {
        val s = service()
        val t = s.startOneOff("a", 60_000L, pinned)
        s.addMinute(t.id)
        assertEquals(120_000L, timers.get(t.id)!!.durationMs)
        assertEquals(clock.now + 120_000L, alarms.scheduled[t.id])
    }

    @Test
    fun addMinuteAtMaxIsNoOp() = runTest {
        val s = service()
        val t = s.startOneOff("a", MAX_DURATION_MS, pinned)
        val calls = alarms.scheduleCalls
        val cancels = alarms.cancelled.size
        s.addMinute(t.id)
        assertEquals(t, timers.get(t.id))
        assertEquals(calls, alarms.scheduleCalls)
        assertEquals(cancels, alarms.cancelled.size)
    }

    @Test
    fun finishIfOverdueIsIdempotent() = runTest {
        val s = service()
        val t = s.startOneOff("a", 10_000L, pinned)
        assertNull(s.finishIfOverdue(t.id))
        clock.advance(10_000L)
        val first = s.finishIfOverdue(t.id)
        assertNotNull(first)
        assertTrue(first!!.state is RunState.Finished)
        assertNull(s.finishIfOverdue(t.id))
        assertNull(s.finishIfOverdue("missing"))
        assertTrue(t.id !in alarms.scheduled)
    }

    @Test
    fun reconcileFinishesOverdueAndReschedulesOthers() = runTest {
        val s = service()
        val overdue = s.startOneOff("a", 10_000L, pinned)
        val other = s.startOneOff("b", 60_000L, s.suggestLook())
        val paused = s.startOneOff("c", 60_000L, s.suggestLook())
        s.pause(paused.id)
        alarms.scheduled.clear()
        clock.advance(20_000L)
        val finished = s.reconcile()
        assertEquals(listOf(overdue.id), finished.map { it.id })
        assertTrue(timers.get(overdue.id)!!.state is RunState.Finished)
        assertEquals(setOf(other.id), alarms.scheduled.keys)
        assertEquals(other.createdAtMs + 60_000L, alarms.scheduled[other.id])
        assertTrue(s.reconcile().isEmpty())
    }

    @Test
    fun dismissDeletesAndCancels() = runTest {
        val s = service()
        val t = s.startOneOff("a", 60_000L, pinned)
        s.dismiss(t.id)
        assertNull(timers.get(t.id))
        assertTrue(t.id !in alarms.scheduled)
        assertTrue(t.id in alarms.cancelled)
    }

    @Test
    fun deletingPresetKeepsActiveRun() = runTest {
        presets.upsert(preset())
        val s = service()
        val t = s.startFromPreset("p1")
        presets.delete("p1")
        clock.advance(5_000L)
        s.pause(t.id)
        assertEquals(RunState.Paused(5_000L), timers.get(t.id)!!.state)
        s.resume(t.id)
        assertTrue(timers.get(t.id)!!.state is RunState.Running)
    }

    @Test
    fun disallowedPauseIsIgnored() = runTest {
        val s = service { it != Control.PAUSE }
        val t = s.startOneOff("a", 60_000L, pinned)
        clock.advance(1_000L)
        s.pause(t.id)
        assertEquals(t, timers.get(t.id))
        assertEquals(clock.now + 59_000L, alarms.scheduled[t.id])
    }

    @Test
    fun disallowedAddMinuteIsIgnored() = runTest {
        val s = service { it != Control.ADD_MINUTE }
        val t = s.startOneOff("a", 60_000L, pinned)
        s.addMinute(t.id)
        assertEquals(60_000L, timers.get(t.id)!!.durationMs)
    }

    @Test
    fun unknownIdsAreNoOps() = runTest {
        val s = service()
        s.pause("x"); s.resume("x"); s.restart("x"); s.addMinute("x"); s.dismiss("x")
        assertTrue(timers.getAll().isEmpty())
    }

    @Test
    fun pauseAfterDueTimeFinishesInsteadOfStranding() = runTest {
        val s = service()
        val t = s.startOneOff("a", 10_000L, pinned)
        clock.advance(15_000L)
        s.pause(t.id)
        assertTrue(timers.get(t.id)!!.state is RunState.Finished)
        assertTrue(t.id !in alarms.scheduled)
    }

    @Test
    fun addMinuteAfterDueTimeGivesFullMinute() = runTest {
        val s = service()
        val t = s.startOneOff("a", 10_000L, pinned)
        clock.advance(15_000L)
        s.addMinute(t.id)
        val r = timers.get(t.id)!!
        assertEquals(70_000L, r.durationMs)
        assertEquals(clock.now + 60_000L, alarms.scheduled[t.id])
    }

    @Test
    fun earlyAlarmReschedules() = runTest {
        val s = service()
        val t = s.startOneOff("a", 60_000L, pinned)
        alarms.scheduled.clear()
        clock.advance(5_000L)
        assertNull(s.finishIfOverdue(t.id))
        assertEquals(t.createdAtMs + 60_000L, alarms.scheduled[t.id])
    }

    private val otherLook = Look(PictureId.SUN, SandColor.SKY, PictureId.MOON, SandColor.MINT)

    @Test
    fun renameRunningTimerKeepsStateAndAlarm() = runTest {
        val s = service()
        val t = s.startOneOff("a", 60_000L, pinned)
        clock.advance(5_000L)
        alarms.scheduled.clear()
        assertEquals(EditResult.Saved, s.editTimer(t.id, "  Tea  ", null, otherLook))
        val r = timers.get(t.id)!!
        assertEquals("Tea", r.name)
        assertEquals(otherLook, r.look)
        assertEquals(t.state, r.state)
        assertEquals(60_000L, r.durationMs)
        assertEquals(t.createdAtMs + 60_000L, alarms.scheduled[t.id])
    }

    @Test
    fun nullLookKeepsLook() = runTest {
        val s = service()
        val t = s.startOneOff("a", 60_000L, pinned)
        s.editTimer(t.id, "b", null, null)
        assertEquals(pinned, timers.get(t.id)!!.look)
    }

    @Test
    fun durationChangeOnPausedTimerKeepsElapsedAndNoAlarm() = runTest {
        val s = service()
        val t = s.startOneOff("a", 60_000L, pinned)
        clock.advance(20_000L)
        s.pause(t.id)
        assertEquals(EditResult.Saved, s.editTimer(t.id, "a", 120_000L, null))
        val r = timers.get(t.id)!!
        assertEquals(120_000L, r.durationMs)
        assertEquals(RunState.Paused(20_000L), r.state)
        assertTrue(t.id !in alarms.scheduled)
    }

    @Test
    fun resumeAfterDurationChangeSchedulesAlarmForNewRemaining() = runTest {
        val s = service()
        val t = s.startOneOff("a", 60_000L, pinned)
        clock.advance(20_000L)
        s.pause(t.id)
        s.editTimer(t.id, "a", 120_000L, null)
        s.resume(t.id)
        assertEquals(clock.now + 100_000L, alarms.scheduled[t.id])
    }

    @Test
    fun durationChangeWhileRunningNeedsPause() = runTest {
        val s = service()
        val t = s.startOneOff("a", 60_000L, pinned)
        assertEquals(EditResult.DurationNeedsPause, s.editTimer(t.id, "b", 120_000L, null))
        assertEquals(t, timers.get(t.id))
    }

    @Test
    fun sameDurationWhileRunningIsFine() = runTest {
        val s = service()
        val t = s.startOneOff("a", 60_000L, pinned)
        assertEquals(EditResult.Saved, s.editTimer(t.id, "b", 60_000L, null))
        assertEquals("b", timers.get(t.id)!!.name)
    }

    @Test
    fun durationNotLongerThanElapsedIsRejected() = runTest {
        val s = service()
        val t = s.startOneOff("a", 60_000L, pinned)
        clock.advance(30_000L)
        s.pause(t.id)
        assertEquals(EditResult.DurationTooShort, s.editTimer(t.id, "a", 30_000L, null))
        assertEquals(EditResult.DurationTooShort, s.editTimer(t.id, "a", 5_000L, null))
        assertEquals(60_000L, timers.get(t.id)!!.durationMs)
        assertEquals(EditResult.Saved, s.editTimer(t.id, "a", 30_001L, null))
    }

    @Test
    fun invalidNameIsRejected() = runTest {
        val s = service()
        val t = s.startOneOff("a", 60_000L, pinned)
        assertEquals(EditResult.InvalidName, s.editTimer(t.id, "   ", null, null))
        assertEquals(EditResult.InvalidName, s.editTimer(t.id, "x".repeat(41), null, null))
        assertEquals(EditResult.Saved, s.editTimer(t.id, "x".repeat(40), null, null))
    }

    @Test
    fun policyDenyBlocksEdit() = runTest {
        val s = service(ControlPolicy { it != Control.EDIT })
        val t = s.startOneOff("a", 60_000L, pinned)
        assertEquals(EditResult.NotAllowed, s.editTimer(t.id, "b", null, null))
        assertEquals(t, timers.get(t.id))
    }

    @Test
    fun unknownTimerIsNotFound() = runTest {
        assertEquals(EditResult.NotFound, service().editTimer("nope", "b", null, null))
    }

    @Test
    fun renameFinishedTimerStaysFinished() = runTest {
        val s = service()
        val t = s.startOneOff("a", 10_000L, pinned)
        clock.advance(15_000L)
        s.finishIfOverdue(t.id)
        assertEquals(EditResult.Saved, s.editTimer(t.id, "b", null, null))
        assertTrue(timers.get(t.id)!!.state is RunState.Finished)
    }

    @Test
    fun editingAnOverdueRunningTimerFinishesItFirst() = runTest {
        val s = service()
        val t = s.startOneOff("a", 10_000L, pinned)
        clock.advance(15_000L)
        assertEquals(EditResult.Saved, s.editTimer(t.id, "b", null, null))
        val r = timers.get(t.id)!!
        assertEquals("b", r.name)
        assertTrue(r.state is RunState.Finished)
    }
}
