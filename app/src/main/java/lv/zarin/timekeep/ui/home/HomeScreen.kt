package lv.zarin.timekeep.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import lv.zarin.timekeep.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenTimer: (String) -> Unit,
    onNewTimer: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        val description = stringResource(R.string.cd_settings)
                        // Gear glyph: material-icons is not a dependency.
                        Text("\u2699", modifier = Modifier.semantics { contentDescription = description })
                    }
                },
            )
        },
    ) { padding ->
        Text(text = "", modifier = Modifier.padding(padding))
    }
}
