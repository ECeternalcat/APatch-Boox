package me.bmax.apatch.ui.component

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Retain captured logs across configuration changes, without owning/restarting commands. */
class LogCaptureViewModel : ViewModel() {
    val buffer = LogBuffer()
    val running = MutableStateFlow(false)
    val result = MutableStateFlow<Boolean?>(null)
    private var started = false

    /** Claim only after entering IO; cancellation before dispatch must not mark a task as run. */
    @Synchronized fun claimStart(): Boolean {
        if (started) return false
        started = true
        return true
    }
}

/** Event-driven, trailing-window coalescing. Completion cancels the wait and flushes immediately. */
internal fun <T : Any> coalescedLogSnapshots(
    updates: Flow<T>,
    completed: (T) -> Boolean,
    snapshot: () -> String,
    awaitWindow: suspend () -> Unit = { delay(300) },
): Flow<String> = channelFlow {
    val latest = MutableStateFlow<T?>(null)
    launch { updates.collect { latest.value = it } }
    latest.filterNotNull().map(completed).distinctUntilChanged().collectLatest { done ->
        latest.filterNotNull().collect {
            if (!done) awaitWindow()
            // Read AFTER the wait, so a sustained burst publishes the latest output.
            send(snapshot())
        }
    }
}.buffer(Channel.CONFLATED).distinctUntilChanged()
