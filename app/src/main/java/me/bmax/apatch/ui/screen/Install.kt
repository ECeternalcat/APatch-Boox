package me.bmax.apatch.ui.screen

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import me.bmax.apatch.ui.component.EinkExtendedFloatingActionButton as ExtendedFloatingActionButton
import me.bmax.apatch.ui.component.EinkButton as FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import me.bmax.apatch.ui.component.EinkButton as OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.topjohnwu.superuser.io.SuFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.KeyEventBlocker
import me.bmax.apatch.ui.component.EinkLogView
import me.bmax.apatch.ui.component.LogCaptureViewModel
import me.bmax.apatch.ui.component.coalescedLogSnapshots
import me.bmax.apatch.ui.component.rememberCustomDialog
import me.bmax.apatch.util.hasMetaModule
import me.bmax.apatch.util.installModule
import me.bmax.apatch.util.reboot
import me.bmax.apatch.util.ui.LocalSnackbarHost
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

enum class MODULE_TYPE {
    KPM, APM
}

@Composable
@Destination<RootGraph>
fun InstallScreen(navigator: DestinationsNavigator, uri: Uri, type: MODULE_TYPE) {
    var text by rememberSaveable { mutableStateOf("") }
    val capture = viewModel<LogCaptureViewModel>()
    val logContent = capture.buffer
    val running by capture.running.collectAsState()
    val result by capture.result.collectAsState()
    var showFloatAction by rememberSaveable { mutableStateOf(false) }
    var taskStarted by rememberSaveable { mutableStateOf(false) }

    val metaModuleAlertDialog = rememberCustomDialog { dismiss: () -> Unit ->
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { dismiss() },
            icon = {
                Icon(Icons.Outlined.Info, contentDescription = null)
            },
            title = {
                Row(modifier = Modifier
                    .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = stringResource(R.string.warning_of_meta_module_title))
                }
            },
            text = {
                Text(text = stringResource(R.string.warning_of_meta_module_summary))
            },
            confirmButton = {
                FilledTonalButton(onClick = { dismiss() }) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    uriHandler.openUri("https://apatch.dev/meta-module.html")
                }) {
                    Text(text = stringResource(id = R.string.learn_more))
                }
            },
        )
    }

    val context = LocalContext.current
    val snackBarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val unavailableLog = stringResource(R.string.eink_log_unavailable)

    LaunchedEffect(logContent) {
        coalescedLogSnapshots(logContent.updates, { it.complete }, logContent::snapshot).collect {
            // After process death only the saved reading projection may remain.
            if (logContent.hasEntries) text = it
        }
    }

    LaunchedEffect(Unit) {
        if (taskStarted) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val start = capture.claimStart()
            taskStarted = true
            if (!start) return@withContext
            capture.running.value = true
            try {
                installModule(uri, type, onFinish = { success ->
                    capture.result.value = success
                    if (!success) return@installModule

                    scope.launch {
                        showFloatAction = true

                        // check metamodule
                        if (hasMetaModule()) return@launch
                        val mountOldDirectory =
                            SuFile.open("/data/adb/modules/${getModuleIdFromUri(context, uri)}/system")
                        val mountNewDirectory =
                            SuFile.open("/data/adb/modules_update/${getModuleIdFromUri(context, uri)}/system")
                        if (!mountNewDirectory.isDirectory && !mountOldDirectory.isDirectory) return@launch

                        metaModuleAlertDialog.show()
                    }

                }, onStdout = { logContent.append(it) }, onStderr = { logContent.append(it) })
            } finally {
                logContent.finish()
                capture.running.value = false
            }
        }
        text = logContent.snapshot()
    }

    Scaffold(topBar = {
        TopBar(onBack = dropUnlessResumed {
            navigator.popBackStack()
        }, onSave = {
            scope.launch {
                if (text.isNotEmpty() && !logContent.hasEntries) {
                    snackBarHost.showSnackbar(unavailableLog)
                    return@launch
                }
                val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
                val date = format.format(Date())
                val file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "APatch_install_${type}_log_${date}.log"
                )
                file.writeText(logContent.export())
                snackBarHost.showSnackbar("Log saved to ${file.absolutePath}")
            }
        })
    }, floatingActionButton = {
        if (showFloatAction) {
            val reboot = stringResource(id = R.string.reboot)
            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            reboot()
                        }
                    }
                },
                icon = { Icon(Icons.Filled.Refresh, reboot) },
                text = { Text(text = reboot) },
            )
        }

    }, snackbarHost = { SnackbarHost(snackBarHost) }) { innerPadding ->
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
            EinkLogView(text, modifier = Modifier.weight(1f).fillMaxWidth())
        }
    }
}

fun isUriAccessible(context: Context, uri: Uri): Boolean {
    if (uri == Uri.EMPTY) return false

    return try {
        context.contentResolver.openInputStream(uri)?.use {} != null
    } catch (e: Exception) {
        Log.e("ModuleInstall", "URI is inaccessible: $uri", e)
        false
    }
}


fun extractModuleId(context: Context, uri: Uri): String? {
    if (uri == Uri.EMPTY) return null

    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        ZipInputStream(inputStream).use { zip ->
            var entry: ZipEntry?

            while (zip.nextEntry.also { entry = it } != null) {
                if (entry?.name == "module.prop") {
                    val prop = Properties()
                    prop.load(zip)
                    return prop.getProperty("id")
                }
            }
        }
    }

    return null
}

suspend fun getModuleIdFromUri(context: Context, uri: Uri): String? {
    return withContext(Dispatchers.IO) {
        try {
            if (uri == Uri.EMPTY) {
                return@withContext null
            }
            if (!isUriAccessible(context, uri)) {
                return@withContext null
            }
            extractModuleId(context, uri)
        } catch (_: Exception) {
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(onBack: () -> Unit = {}, onSave: () -> Unit = {}) {
    TopAppBar(title = { Text(stringResource(R.string.apm_install)) }, navigationIcon = {
        IconButton(
            onClick = onBack
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
    }, actions = {
        IconButton(onClick = onSave) {
            Icon(
                imageVector = Icons.Filled.Save, contentDescription = stringResource(R.string.save_log)
            )
        }
    })
}

@Preview
@Composable
fun InstallPreview() {
//    InstallScreen(DestinationsNavigator(), uri = Uri.EMPTY)
}