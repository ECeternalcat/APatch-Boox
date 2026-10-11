package me.bmax.apatch.ui.component

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogPresentationTest {
    @Test fun retainedCaptureClaimsTheCommandOnlyOnceIndependentlyOfDisplayedText() {
        val capture = LogCaptureViewModel()
        assertTrue(capture.claimStart())
        repeat(10) { assertFalse(capture.claimStart()) }
        // Clearing output or pausing reading cannot reopen the execution gate.
        capture.buffer.append("\u001B[H\u001B[J")
        assertEquals("", capture.buffer.snapshot())
        assertFalse(capture.claimStart())
        assertTrue(LogCaptureViewModel().claimStart())
    }

    @Test fun readingStateRetainsItsFrozenSnapshotWithoutASecondSavedLogString() {
        val reader = LogReadingViewModel()
        reader.displayed = "read this"
        reader.following = false
        reader.wrap = false
        reader.displayed = logTextForReader(reader.displayed, "new output", reader.following)
        assertEquals("read this", reader.displayed)
        assertFalse(reader.wrap)
        reader.following = true
        reader.displayed = logTextForReader(reader.displayed, "new output", reader.following)
        assertEquals("new output", reader.displayed)
    }

    @Test(timeout = 5000) fun sustainedBurstsPublishLatestAtEachControlledWindow() = runBlocking {
        val buffer = LogBuffer()
        val windows = Channel<Unit>(Channel.UNLIMITED)
        val ticks = Channel<Unit>(Channel.UNLIMITED)
        val frames = Channel<String>(Channel.UNLIMITED)
        val collector = launch {
            coalescedLogSnapshots(buffer.updates, { it.complete }, buffer::snapshot) {
                windows.send(Unit)
                ticks.receive()
            }.collect { frames.send(it) }
        }
        try {
            windows.receive()
            repeat(1000) { buffer.append("stdout-$it") }
            ticks.send(Unit)
            assertTrue(frames.receive().endsWith("stdout-999\n"))
            assertTrue(frames.tryReceive().isFailure)
            windows.receive()
            repeat(1000) { buffer.append("stderr-$it") }
            ticks.send(Unit)
            assertTrue(frames.receive().endsWith("stderr-999\n"))
            assertEquals(2000, buffer.export().count { it == '\n' })
        } finally {
            collector.cancelAndJoin()
        }
    }

    @Test(timeout = 5000) fun completionFlushesWithoutWaitingForTheNextTick() = runBlocking {
        val buffer = LogBuffer()
        val entered = Channel<Unit>(Channel.UNLIMITED)
        val neverTick = Channel<Unit>()
        val frames = Channel<String>(Channel.UNLIMITED)
        val collector = launch {
            coalescedLogSnapshots(buffer.updates, { it.complete }, buffer::snapshot) {
                entered.send(Unit)
                neverTick.receive()
            }.collect { frames.send(it) }
        }
        try {
            entered.receive()
            buffer.append("last output")
            buffer.finish()
            assertEquals("last output\n", frames.receive())
            // Callbacks delivered after completion must not be discarded either.
            buffer.append("late callback")
            assertEquals("last output\nlate callback\n", frames.receive())
        } finally {
            collector.cancelAndJoin()
        }
    }

    @Test(timeout = 5000) fun noEventsDoNotCreateAnIdlePollingLoop() = runBlocking {
        val buffer = LogBuffer()
        val entered = Channel<Unit>(Channel.UNLIMITED)
        val ticks = Channel<Unit>(Channel.UNLIMITED)
        val frames = Channel<String>(Channel.UNLIMITED)
        val collector = launch {
            coalescedLogSnapshots(buffer.updates, { it.complete }, buffer::snapshot) {
                entered.send(Unit)
                ticks.receive()
            }.collect { frames.send(it) }
        }
        try {
            entered.receive()
            ticks.send(Unit)
            assertEquals("", frames.receive())
            repeat(3) { yield() }
            assertTrue(entered.tryReceive().isFailure)
            assertTrue(frames.tryReceive().isFailure)
        } finally {
            collector.cancelAndJoin()
        }
    }

    @Test(timeout = 5000) fun anotherActivePhaseReenablesCoalescing() = runBlocking {
        val revisions = MutableStateFlow(LogRevision(0, true))
        var text = "previous operation"
        val entered = Channel<Unit>(Channel.UNLIMITED)
        val ticks = Channel<Unit>()
        val frames = Channel<String>(Channel.UNLIMITED)
        val collector = launch {
            coalescedLogSnapshots(revisions, { it.complete }, { text }) {
                entered.send(Unit)
                ticks.receive()
            }.collect { frames.send(it) }
        }
        try {
            assertEquals("previous operation", frames.receive())
            text = "new operation"
            revisions.value = LogRevision(1, false)
            entered.receive()
            assertTrue(frames.tryReceive().isFailure)
            text = "final output"
            revisions.value = LogRevision(2, true)
            assertEquals("final output", frames.receive())
        } finally {
            collector.cancelAndJoin()
        }
    }

    @Test fun pausedReadingSurvivesTailReplacementAndResumeUsesLatestWithoutLosingExport() {
        val buffer = LogBuffer(screenLimit = 20)
        buffer.append("read this")
        var displayed = logTextForReader("", buffer.snapshot(), following = true)
        repeat(1000) {
            buffer.append("line-$it")
            displayed = logTextForReader(displayed, buffer.snapshot(), following = false)
        }
        assertEquals("read this\n", displayed)
        assertTrue(buffer.export().startsWith("read this\n"))
        assertEquals(1001, buffer.export().count { it == '\n' })
        displayed = logTextForReader(displayed, buffer.snapshot(), following = true)
        assertTrue(displayed.endsWith("line-999\n"))
        assertFalse(displayed.contains("read this"))
    }
}
