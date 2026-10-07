package lv.zarin.timekeep.testutil

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

/** Virtual milliseconds advanced per pump. Small enough that 250 ms tickers fire in order. */
const val PUMP_STEP_MS = 50L

/** Default bound: 400 pumps * 50 ms = 20 s of virtual time. */
const val DEFAULT_MAX_PUMPS = 400

/**
 * Advances Compose frames and the main looper by [steps] fixed steps of [PUMP_STEP_MS].
 * Requires `mainClock.autoAdvance = false` (running hourglasses never go idle). Never sleeps.
 */
fun ComposeTestRule.pump(steps: Int = 1) {
    repeat(steps) {
        mainClock.advanceTimeBy(PUMP_STEP_MS)
        ShadowLooper.idleMainLooper(PUMP_STEP_MS, TimeUnit.MILLISECONDS)
    }
}

/**
 * Pumps until [condition] holds, at most [maxPumps] times; fails with [description] otherwise.
 * The bound is a count of virtual steps, not a wall-clock deadline.
 */
fun ComposeTestRule.pumpUntil(description: String, maxPumps: Int = DEFAULT_MAX_PUMPS, condition: () -> Boolean) {
    repeat(maxPumps) {
        if (condition()) return
        pump()
    }
    if (!condition()) throw AssertionError("Gave up after $maxPumps pumps waiting for: $description")
}

fun ComposeTestRule.textCount(text: String): Int = onAllNodesWithText(text).fetchSemanticsNodes().size

fun ComposeTestRule.descriptionCount(description: String): Int =
    onAllNodesWithContentDescription(description).fetchSemanticsNodes().size

fun ComposeTestRule.pumpUntilText(text: String, count: Int = 1) =
    pumpUntil("text \"$text\" x$count") { textCount(text) == count }

fun ComposeTestRule.pumpUntilNoText(text: String) =
    pumpUntil("text \"$text\" gone") { textCount(text) == 0 }

fun ComposeTestRule.pumpUntilDescription(description: String, count: Int = 1) =
    pumpUntil("content description \"$description\" x$count") { descriptionCount(description) == count }
