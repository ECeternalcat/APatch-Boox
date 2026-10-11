package me.bmax.apatch.ui.screen

import android.os.Environment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.KeyEventBlocker
import me.bmax.apatch.ui.component.EinkLogView
import me.bmax.apatch.ui.component.LogCaptureViewModel
import me.bmax.apatch.ui.component.coalescedLogSnapshots
import me.bmax.apatch.util.runAPModuleAction
import me.bmax.apatch.util.ui.LocalSnackbarHost
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
@Destination<RootGraph>
fun ExecuteAPMActionScreen(navigator: DestinationsNavigator, moduleId: String) {
    var text by rememberSaveable { mutableStateOf("") }
    val capture = viewModel<LogCaptureViewModel>()
    val logContent = capture.buffer
    val running by capture.running.collectAsState()
    val result by capture.result.collectAsState()
    var taskStarted by rememberSaveable { mutableStateOf(false) }
    val snackBarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val unavailableLog = stringResource(R.string.eink_log_unavailable)

    LaunchedEffect(logContent) {
        coalescedLogSnapshots(logContent.updates, { it.complete }, logContent::snapshot).collect {
            if (logContent.hasEntries) text = it
        }
    }

    LaunchedEffect(Unit) {
        if (taskStarted) return@LaunchedEffect
        val actionResult = withContext(Dispatchers.IO) {
            val start = capture.claimStart()
            taskStarted = true
            if (!start) return@withContext null
            capture.running.value = true
            try {
                runAPModuleAction(moduleId, onStdout = { logContent.append(it) }, onStderr = { logContent.append(it) })
                    .also { capture.result.value = it }
            } finally {
                logContent.finish()
                capture.running.value = false
            }
        }
        text = logContent.snapshot()
        if (actionResult == true) {
            navigator.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                onBack = dropUnlessResumed {
                    navigator.popBackStack()
                },
                onSave = {
                    scope.launch {
                        if (text.isNotEmpty() && !logContent.hasEntries) {
                            snackBarHost.showSnackbar(unavailableLog)
                            return@launch
                        }
                        val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
                        val date = format.format(Date())
                        val file = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "APatch_apm_action_log_${date}.log"
                        )
                        file.writeText(logContent.export())
                        snackBarHost.showSnackbar("Log saved to ${file.absolutePath}")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackBarHost) }
    ) { innerPadding ->
        KeyEventBlocker {
            it.key == Key.VolumeDown || it.key == Key.VolumeUp
        }
        Column(
            modifier = Modifier
                .fillMaxSize(1f)
                .padding(innerPadding),
        ) {
            if (running) {
                Text(stringResource(R.string.eink_loading), modifier = Modifier.padding(8.dp))
            } else if (result != null) {
                Text(
                    stringResource(if (result == true) R.string.eink_operation_complete else R.string.eink_operation_failed),
                    modifier = Modifier.padding(8.dp),
                )
            }
            EinkLogView(text, modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(onBack: () -> Unit = {}, onSave: () -> Unit = {}) {
    TopAppBar(
        title = { Text(stringResource(R.string.apm_action)) },
        navigationIcon = {
            IconButton(
                onClick = onBack
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
        },
        actions = {
            IconButton(onClick = onSave) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = stringResource(R.string.save_log)
                )
            }
        }
    )
}
