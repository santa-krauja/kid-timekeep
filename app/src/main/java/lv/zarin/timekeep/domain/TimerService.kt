package lv.zarin.timekeep.domain

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import lv.zarin.timekeep.domain.control.AllowAllControlPolicy
import lv.zarin.timekeep.domain.control.Control
import lv.zarin.timekeep.domain.control.ControlPolicy
import lv.zarin.timekeep.domain.look.LookPicker
import lv.zarin.timekeep.domain.ports.AlarmScheduler
import lv.zarin.timekeep.domain.ports.Clock
import lv.zarin.timekeep.domain.ports.PresetRepository
import lv.zarin.timekeep.domain.ports.TimerRepository
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.Timer
import lv.zarin.timekeep.domain.timer.TimerActions
import lv.zarin.timekeep.domain.timer.finishAtMs
import lv.zarin.timekeep.domain.timer.isOverdue
import lv.zarin.timekeep.domain.timer.isValidDuration
import lv.zarin.timekeep.domain.timer.newId

/** Application core: orchestrates timer transitions, persistence and alarms. */
class TimerService(
    private val timers: TimerRepository,
    private val presets: PresetRepository,
    private val alarms: AlarmScheduler,
    private val clock: Clock,
    private val lookPicker: LookPicker,
    private val policy: ControlPolicy = AllowAllControlPolicy,
    private val ids: () -> String = ::newId,
) {
    private val mutex = Mutex()

    suspend fun suggestLook(): Look = mutex.withLock { lookPicker.pick(activeLooks()) }

    suspend fun startFromPreset(presetId: String): Timer = mutex.withLock {
        val preset = presets.get(presetId)
            ?: throw IllegalArgumentException("Unknown preset: $presetId")
        val look = preset.pinnedLook
            ?: lookPicker.pick(activeLooks() + listOfNotNull(preset.lastLook))
        val timer = TimerActions.newRun(ids(), preset.name, preset.durationMs, look, preset.id, clock.nowMs())
        save(timer)
        presets.upsert(preset.copy(lastLook = look))
        timer
    }

    suspend fun startOneOff(name: String, durationMs: Long, look: Look, presetId: String? = null): Timer =
        mutex.withLock {
            require(isValidDuration(durationMs)) { "Invalid duration: $durationMs" }
            val timer = TimerActions.newRun(ids(), name.trim(), durationMs, look, presetId, clock.nowMs())
            save(timer)
            timer
        }

    suspend fun pause(id: String): Unit {
        if (!policy.isAllowed(Control.PAUSE)) return
        transition(id) { TimerActions.pause(it, clock.nowMs()) }
    }

    suspend fun resume(id: String): Unit = transition(id) { TimerActions.resume(it, clock.nowMs()) }

    suspend fun restart(id: String): Unit = transition(id) { TimerActions.restart(it, clock.nowMs()) }

    suspend fun addMinute(id: String): Unit {
        if (!policy.isAllowed(Control.ADD_MINUTE)) return
        transition(id) { TimerActions.addMinute(it, clock.nowMs()) }
    }

    /** Returns the timer only if this call finished it. */
    suspend fun finishIfOverdue(id: String): Timer? = mutex.withLock {
        val t = timers.get(id) ?: return@withLock null
        finishOverdue(t, clock.nowMs()) ?: run {
            if (t.state is RunState.Running) syncAlarm(t) // early or stale fire: re-arm
            null
        }
    }

    suspend fun dismiss(id: String): Unit = mutex.withLock {
        timers.delete(id)
        alarms.cancel(id)
    }

    /** Finishes overdue running timers (returned) and reschedules alarms of the other running ones. */
    suspend fun reconcile(): List<Timer> = mutex.withLock {
        val now = clock.nowMs()
        val finished = mutableListOf<Timer>()
        for (t in timers.getAll()) {
            val done = finishOverdue(t, now)
            if (done != null) finished += done else if (t.state is RunState.Running) syncAlarm(t)
        }
        finished
    }

    private suspend fun transition(id: String, change: (Timer) -> Timer): Unit = mutex.withLock {
        val t = timers.get(id) ?: return@withLock
        // An overdue Running timer is finished first, so actions never strand or shortchange it.
        val current = TimerActions.finish(t, clock.nowMs()).takeIf { t.isOverdue(clock.nowMs()) } ?: t
        val updated = change(current)
        if (updated === t) return@withLock
        save(updated)
    }

    private suspend fun finishOverdue(t: Timer, now: Long): Timer? {
        if (!t.isOverdue(now)) return null
        val finished = TimerActions.finish(t, now)
        save(finished)
        return finished
    }

    private suspend fun save(t: Timer) {
        timers.upsert(t)
        syncAlarm(t)
    }

    private fun syncAlarm(t: Timer) {
        val at = t.finishAtMs()
        if (at != null) alarms.schedule(t.id, at) else alarms.cancel(t.id)
    }

    private suspend fun activeLooks(): Set<Look> =
        timers.getAll().filter { it.state !is RunState.Finished }.map { it.look }.toSet()
}
