package me.bmax.apatch.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogBufferTest {
    @Test fun preservesOrderAndBlankLinesInCompleteExport() {
        val buffer = LogBuffer()
        buffer.append("stdout")
        buffer.append("")
        buffer.append("stderr")
        assertEquals("stdout\n\nstderr\n", buffer.export())
        assertEquals(buffer.export(), buffer.snapshot())
    }

    @Test fun clearAffectsScreenOnlyAndTailIsIndependentFromExport() {
        val buffer = LogBuffer(screenLimit = 8)
        buffer.append("before")
        buffer.append("\u001B[H\u001B[Jnow")
        assertEquals("before\n\u001B[H\u001B[Jnow\n", buffer.export())
        assertEquals("now\n", buffer.snapshot())

        buffer.append("123456789")
        assertTrue(buffer.snapshot().length <= 8)
        assertTrue(buffer.export().contains("before"))
    }

    @Test fun emptyClearClearsOnlyTheReadingProjection() {
        val buffer = LogBuffer()
        buffer.append("before")
        buffer.append("\u001B[H\u001B[J")
        assertEquals("", buffer.snapshot())
        assertEquals("before\n\u001B[H\u001B[J\n", buffer.export())
    }

    @Test fun boundedTailDoesNotSplitAnEmojiSurrogatePair() {
        val buffer = LogBuffer(screenLimit = 3)
        buffer.append("A😀x")
        assertEquals("x\n", buffer.snapshot())
        assertEquals("A😀x\n", buffer.export())
    }

    @Test fun burstAndFinalSnapshotContainLatestOutput() {
        val buffer = LogBuffer()
        repeat(1000) { buffer.append("line-$it") }
        assertTrue(buffer.snapshot().contains("line-999"))
        assertTrue(buffer.export().startsWith("line-0\n"))
    }
}
