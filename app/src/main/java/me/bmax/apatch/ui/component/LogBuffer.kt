package me.bmax.apatch.ui.component

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class LogRevision(val number: Long, val complete: Boolean)

/** Thread-safe raw log capture and bounded screen projection. UI throttling never affects export. */
class LogBuffer(private val screenLimit: Int = 100_000) {
    init { require(screenLimit > 0) }
    private val revision = MutableStateFlow(LogRevision(0, false))
    internal val updates = revision.asStateFlow()
    val hasEntries: Boolean get() = revision.value.number > 0
    private val raw = StringBuilder()
    private val screen = StringBuilder()
    private val clearSequence = "\u001B[H\u001B[J"

    @Synchronized
    fun append(line: String) {
        raw.append(line).append('\n')
        val visible = if (line.startsWith(clearSequence)) {
            screen.setLength(0)
            line.removePrefix(clearSequence)
        } else line
        if (visible.isNotEmpty() || line.isEmpty()) screen.append(visible).append('\n')
        if (screen.length > screenLimit) {
            var cut = screen.length - screenLimit
            if (screen[cut].isLowSurrogate() && screen[cut - 1].isHighSurrogate()) cut++
            screen.delete(0, cut)
        }
        revision.value = revision.value.copy(number = revision.value.number + 1)
    }

    @Synchronized fun finish() {
        revision.value = revision.value.copy(complete = true)
    }

    @Synchronized fun snapshot(): String = screen.toString()
    @Synchronized fun export(): String = raw.toString()
}
