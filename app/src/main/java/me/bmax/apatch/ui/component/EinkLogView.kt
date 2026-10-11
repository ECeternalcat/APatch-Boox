package me.bmax.apatch.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.R

internal fun logTextForReader(previous: String, incoming: String, following: Boolean): String =
    if (following) incoming else previous

/** Retain a frozen reading snapshot without duplicating large logs in saved-state Bundles. */
class LogReadingViewModel : ViewModel() {
    var following by mutableStateOf(true)
    var wrap by mutableStateOf(true)
    var displayed by mutableStateOf("")
}

/** Pausing freezes the reading snapshot, never the producer or the full export. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EinkLogView(text: String, modifier: Modifier = Modifier) {
    val reader = viewModel<LogReadingViewModel>()
    val following = reader.following
    val wrap = reader.wrap
    val displayed = reader.displayed
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()
    val readingState = stringResource(
        if (following) R.string.eink_log_following else R.string.eink_log_reading
    )

    LaunchedEffect(text, following) {
        reader.displayed = logTextForReader(reader.displayed, text, following)
    }
    // maxValue changes after measurement; using it as a key prevents following stale bounds.
    LaunchedEffect(displayed, following, vertical.maxValue) {
        if (following) vertical.scrollTo(vertical.maxValue)
    }

    Column(modifier) {
        Text(readingState, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EinkButton(
                modifier = Modifier.semantics { stateDescription = readingState },
                onClick = {
                    // Capture the latest visible text before freezing, or resume immediately.
                    reader.displayed = text
                    reader.following = !following
                },
            ) {
                Text(stringResource(if (following) R.string.eink_log_pause else R.string.eink_log_resume))
            }
            EinkButton(onClick = { reader.wrap = !wrap }) {
                Text(stringResource(if (wrap) R.string.eink_log_no_wrap else R.string.eink_log_wrap))
            }
        }
        SelectionContainer(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(vertical)
                    .then(if (wrap) Modifier else Modifier.horizontalScroll(horizontal)),
            ) {
                Text(
                    text = displayed,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    softWrap = wrap,
                )
            }
        }
    }
}
