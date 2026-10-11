package lv.zarin.timekeep.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import lv.zarin.timekeep.data.settings.DataStoreSeedFlagStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SeedFlagStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun markSeededPersists() = runTest(UnconfinedTestDispatcher()) {
        val file = File(tmp.root, "flags.preferences_pb")
        val store = DataStoreSeedFlagStore(PreferenceDataStoreFactory.create(scope = backgroundScope) { file })
        assertFalse(store.isSeeded())
        store.markSeeded()
        assertTrue(store.isSeeded())
    }
}
