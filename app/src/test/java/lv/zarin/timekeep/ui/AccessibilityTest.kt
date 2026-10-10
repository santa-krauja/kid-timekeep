package lv.zarin.timekeep.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.SemanticsNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import lv.zarin.timekeep.testutil.TestAppContainer
import lv.zarin.timekeep.KidTimekeepApp
import lv.zarin.timekeep.testutil.seedStarterPresets
import lv.zarin.timekeep.MainActivity
import lv.zarin.timekeep.domain.ports.ThemeMode
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.SandColor
import lv.zarin.timekeep.domain.timer.Timer
import lv.zarin.timekeep.testutil.FakeClock
import lv.zarin.timekeep.testutil.allowNotifications
import lv.zarin.timekeep.ui.edit.EditTimerScreen
import lv.zarin.timekeep.ui.settings.SettingsScreen
import lv.zarin.timekeep.ui.theme.KidTimekeepTheme
import lv.zarin.timekeep.ui.timer.TimerScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

private fun flatten(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::flatten)

/** Every node with a click action needs a spoken label and a >= 48 dp touch target. */
private fun assertClickablesAccessible(root: SemanticsNode, density: Float, screen: String) {
    val clickables = flatten(root).filter { it.config.contains(SemanticsActions.OnClick) }
    assertTrue("$screen: no clickables found", clickables.isNotEmpty())
    for (n in clickables) {
        // Off-screen items of scrolling containers are not laid out; they are checked when scrolled in.
        val cfg = n.config
        val label = buildList {
            cfg.getOrNull(SemanticsProperties.ContentDescription)?.let { addAll(it) }
            cfg.getOrNull(SemanticsProperties.Text)?.let { t -> addAll(t.map { it.text }) }
            cfg.getOrNull(SemanticsProperties.EditableText)?.let { add(it.text) }
        }.joinToString("").trim()
        // Text fields are labelled by their placeholder/label child; accept a text-input role as labelled.
        val labelled = label.isNotEmpty() || cfg.contains(SemanticsActions.SetText)
        assertTrue("$screen: clickable node ${n.id} has no label: $cfg", labelled)
        val b = n.touchBoundsInRoot
        val w = b.width / density
        val h = b.height / density
        assertTrue("$screen: node ${n.id} '$label' touch target ${w}x$h dp < 48", w >= 48f && h >= 48f)
    }
}

private fun contrast(a: Color, b: Color): Double {
    val l1 = a.luminance().toDouble()
    val l2 = b.luminance().toDouble()
    return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
}

// Large window so scrolling lists are laid out unclipped and touch bounds are meaningful.
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w1000dp-h3000dp-mdpi")
class AccessibilityTest {
    private val clock = FakeClock(100_000)
    private val app = ApplicationProvider.getApplicationContext<KidTimekeepApp>().also {
        it.allowNotifications()
        it.container = TestAppContainer(it, clock = clock)
        seedStarterPresets(it.container)
    }
    private val look = Look(PictureId.HEART, SandColor.SKY, PictureId.STAR, SandColor.MINT)
    private val density get() = app.resources.displayMetrics.density

    @get:Rule
    val rule = createComposeRule()

    private fun pump() {
        rule.mainClock.advanceTimeBy(50)
        ShadowLooper.idleMainLooper()
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        repeat(200) {
            pump()
            if (condition()) return
        }
        throw AssertionError("Timed out waiting for $what")
    }

    private fun showTimer(state: RunState) {
        runBlocking {
            app.container.timerRepository.upsert(Timer("t", "Reading", 15 * 60_000L, look, null, state, 0, 0))
        }
        rule.mainClock.autoAdvance = false
        rule.setContent { TimerScreen("t", onBack = {}) }
    }

    private fun waitForCd(desc: String) = waitFor(desc) {
        rule.onAllNodesWithContentDescription(desc).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun hourglassHasTimePhrase() {
        // 7 minutes elapsed of 15 -> 8 left.
        showTimer(RunState.Running(sinceMs = 100_000 - 7 * 60_000L, elapsedBeforeMs = 0))
        waitForCd("Reading: 8 minutes left of 15 minutes")
    }

    @Test
    fun hourglassPhraseSaysPaused() {
        showTimer(RunState.Paused(elapsedMs = 7 * 60_000L))
        waitForCd("Reading: 8 minutes left of 15 minutes, paused")
    }

    @Test
    fun hourglassPhraseSaysTimesUpWhenFinished() {
        showTimer(RunState.Finished(100_000))
        waitForCd("Reading: time's up")
    }

    @Test
    fun overdueRunningTimerReadsTimesUp() {
        // Started 20 minutes ago on a 15 minute timer, not yet reconciled to Finished.
        showTimer(RunState.Running(sinceMs = 100_000 - 20 * 60_000L, elapsedBeforeMs = 0))
        waitForCd("Reading: time's up")
    }

    @Test
    fun timerClickablesHaveLabelsAndSize() {
        showTimer(RunState.Running(sinceMs = 40_000, elapsedBeforeMs = 0))
        waitForCd("Start over")
        assertClickablesAccessible(rule.onRoot().fetchSemanticsNode(), density, "timer")
    }

    @Test
    fun timerTraversalOrderIsTitleHourglassTimeControls() {
        showTimer(RunState.Running(sinceMs = 40_000, elapsedBeforeMs = 0))
        waitForCd("Start over")
        val nodes = flatten(rule.onRoot().fetchSemanticsNode())
        fun SemanticsNode.cd() = config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString()
        fun SemanticsNode.text() = config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString { it.text }
        // Compose sorts TalkBack traversal geometrically (top, then left) unless traversalIndex is set,
        // so the order is by screen position. Tree order differs (Scaffold slots content first).
        fun top(pred: (SemanticsNode) -> Boolean) = nodes.first(pred).boundsInRoot.top
        val title = top { it.text() == "Reading" }
        val hourglass = top { it.cd().startsWith("Reading: ") }
        val time = top { Regex("\\d+:\\d\\d").matches(it.text()) && it.cd().isNotEmpty() }
        val controls = top { it.cd() == "Start over" }
        assertTrue(
            "title<hourglass<time<controls: $title $hourglass $time $controls",
            title < hourglass && hourglass < time && time < controls,
        )
    }

    @Test
    fun editClickablesHaveLabelsAndSize() {
        rule.setContent { EditTimerScreen(presetId = null, onBack = {}, onStarted = {}) }
        waitFor("edit") { rule.onAllNodesWithText("Name").fetchSemanticsNodes().isNotEmpty() }
        assertClickablesAccessible(rule.onRoot().fetchSemanticsNode(), density, "edit")
    }

    @Test
    fun settingsClickablesHaveLabelsAndSize() {
        rule.setContent { SettingsScreen(onBack = {}) }
        waitFor("settings") { rule.onAllNodesWithText("Show numbers").fetchSemanticsNodes().isNotEmpty() }
        assertClickablesAccessible(rule.onRoot().fetchSemanticsNode(), density, "settings")
    }

    @Test
    fun lookPickerSwatchesAreBigEnough() {
        rule.setContent { EditTimerScreen(presetId = null, onBack = {}, onStarted = {}) }
        waitFor("edit") { rule.onAllNodesWithText("Choose pictures & colours").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Choose pictures & colours").performScrollTo().performClick()
        waitFor("pickers") { rule.onAllNodesWithText("Top picture").fetchSemanticsNodes().isNotEmpty() }
        assertClickablesAccessible(rule.onRoot().fetchSemanticsNode(), density, "look picker")
    }

    private fun schemeFor(mode: ThemeMode): ColorScheme {
        lateinit var cs: ColorScheme
        rule.setContent { KidTimekeepTheme(themeMode = mode) { cs = MaterialTheme.colorScheme } }
        rule.waitForIdle()
        return cs
    }

    private fun assertContrast(cs: ColorScheme, name: String) {
        val pairs = mapOf(
            "onSurface/surface" to (cs.onSurface to cs.surface),
            "onBackground/background" to (cs.onBackground to cs.background),
            "onSurfaceVariant/surface" to (cs.onSurfaceVariant to cs.surface),
            "onSurfaceVariant/background" to (cs.onSurfaceVariant to cs.background),
            "onSurfaceVariant/surfaceVariant" to (cs.onSurfaceVariant to cs.surfaceVariant),
            "onPrimary/primary" to (cs.onPrimary to cs.primary),
            "onPrimaryContainer/primaryContainer" to (cs.onPrimaryContainer to cs.primaryContainer),
            "onSecondaryContainer/secondaryContainer" to (cs.onSecondaryContainer to cs.secondaryContainer),
            "primary/surface" to (cs.primary to cs.surface),
            "secondary/surface" to (cs.secondary to cs.surface),
        )
        for ((label, p) in pairs) {
            val c = contrast(p.first, p.second)
            assertTrue("$name $label contrast %.2f < 4.5".format(c), c >= 4.5)
        }
    }

    @Test
    fun lightThemeTextContrast() = assertContrast(schemeFor(ThemeMode.LIGHT), "light")

    @Test
    fun darkThemeTextContrast() = assertContrast(schemeFor(ThemeMode.DARK), "dark")
}

@RunWith(AndroidJUnit4::class)
class HomeAccessibilityTest {
    private val app = ApplicationProvider.getApplicationContext<KidTimekeepApp>().also {
        it.allowNotifications()
        it.container = TestAppContainer(it)
        seedStarterPresets(it.container)
    }

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeClickablesHaveLabelsAndSize() {
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Reading").fetchSemanticsNodes().isNotEmpty() }
        val look = Look(PictureId.HEART, SandColor.SKY, PictureId.STAR, SandColor.MINT)
        // A running hourglass animates forever, so the clock is driven by hand from here on.
        rule.mainClock.autoAdvance = false
        val now = app.container.clock.nowMs()
        runBlocking {
            app.container.timerRepository.upsert(
                Timer("t", "Tidy", 120_000, look, null, RunState.Running(now, 0), 0, 0),
            )
        }
        rule.waitUntil(10_000) {
            rule.mainClock.advanceTimeBy(50)
            rule.onAllNodesWithContentDescription("Pause").fetchSemanticsNodes().isNotEmpty()
        }
        assertClickablesAccessible(
            rule.onRoot().fetchSemanticsNode(), app.resources.displayMetrics.density, "home",
        )
    }
}

@RunWith(AndroidJUnit4::class)
class HomeFinishedCardTest {
    private val app = ApplicationProvider.getApplicationContext<KidTimekeepApp>().also {
        it.allowNotifications()
        it.container = TestAppContainer(it)
        seedStarterPresets(it.container)
    }

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun finishedCardReadsTimesUp() {
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Reading").fetchSemanticsNodes().isNotEmpty() }
        val look = Look(PictureId.HEART, SandColor.SKY, PictureId.STAR, SandColor.MINT)
        runBlocking {
            app.container.timerRepository.upsert(Timer("t", "Tidy", 120_000, look, null, RunState.Finished(1), 0, 0))
        }
        rule.waitUntil(10_000) {
            rule.onAllNodesWithContentDescription("Tidy: time's up", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
