package lv.zarin.timekeep

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.coroutines.cancellation.CancellationException

@RunWith(AndroidJUnit4::class)
class AppStartupTest {
    private val calls = mutableListOf<String>()

    @Test
    fun seedsThenReconciles() = runTest {
        AppStartup.launch(seed = { calls += "seed" }, reconcile = { calls += "reconcile" })
        assertEquals(listOf("seed", "reconcile"), calls)
    }

    @Test
    fun reconcileStillRunsWhenSeedingFails() = runTest {
        AppStartup.launch(seed = { error("boom") }, reconcile = { calls += "reconcile" })
        assertEquals(listOf("reconcile"), calls)
    }

    @Test
    fun reconcileFailureDoesNotPropagate() = runTest {
        AppStartup.launch(seed = { calls += "seed" }, reconcile = { error("boom") })
        assertEquals(listOf("seed"), calls)
    }

    @Test
    fun cancellationDuringSeedingPropagates() = runTest {
        assertThrows(CancellationException::class.java) {
            runBlocking {
                AppStartup.launch(seed = { throw CancellationException("x") }, reconcile = { calls += "reconcile" })
            }
        }
        assertEquals(emptyList<String>(), calls)
    }
}
