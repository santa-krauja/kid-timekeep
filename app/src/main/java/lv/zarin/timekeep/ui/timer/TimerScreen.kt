package lv.zarin.timekeep.ui.timer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import lv.zarin.timekeep.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.timer_title_placeholder)) }) },
    ) { padding ->
        Text(text = "", modifier = Modifier.padding(padding))
    }
}
