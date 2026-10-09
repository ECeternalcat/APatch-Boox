package me.bmax.apatch.util

import com.topjohnwu.superuser.Shell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Tests the ordering contract of [refreshShellCaches], the fork's shell cache
 * refresh used after an authentication change (BooxSuperKeyCard applying a
 * SuperKey, installApatch). Against fakes: the replacement global-mount shell
 * is built before the MainShell registration reset, the replacement main
 * shell last (so libsu's cached main shell ends up being the main shell),
 * the pair is published only once both replacements exist, and stale shells
 * are closed afterwards; a failed main build never leaks the new global shell
 * nor closes the still-published old shells.
 */
class RefreshShellCachesTest {

    private class FakeShell(val id: String) : Shell() {
        var closed = false
        var closeThrows = false

        override fun isAlive(): Boolean = !closed
        override fun execTask(task: Task) = throw UnsupportedOperationException("unused in test")
        override fun submitTask(task: Task) = throw UnsupportedOperationException("unused in test")
        override fun newJob(): Job = throw UnsupportedOperationException("unused in test")
        override fun getStatus(): Int = NON_ROOT_SHELL
        override fun waitAndClose(timeout: Long, unit: TimeUnit): Boolean {
            close()
            return true
        }

        override fun close() {
            if (closeThrows) throw IllegalStateException("close failure must not break refresh")
            closed = true
        }
    }

    @Test
    fun `refresh builds replacements, publishes them and closes stale shells afterwards`() {
        val oldMain = FakeShell("old-main")
        val oldGlobal = FakeShell("old-global")
        val newMain = FakeShell("new-main")
        val newGlobal = FakeShell("new-global")
        val events = mutableListOf<String>()
        var publishedMain: Shell? = null
        var publishedGlobal: Shell? = null

        refreshShellCaches(
            currentMain = oldMain,
            currentGlobal = oldGlobal,
            buildGlobalShell = {
                events += "build-global"
                newGlobal
            },
            buildMainShell = {
                events += "build-main"
                newMain
            },
            resetMainShellRegistration = { events += "reset-main-registration" },
        ) { main, global ->
            events += "publish"
            publishedMain = main
            publishedGlobal = global
        }

        // Global first, registration reset between the two builds, main last:
        // every Builder.build registers itself as MainShell's cached shell, so
        // the main build must be the last registration.
        assertEquals(
            listOf("build-global", "reset-main-registration", "build-main", "publish"),
            events
        )
        assertSame(newMain, publishedMain)
        assertSame(newGlobal, publishedGlobal)

        // Stale shells closed only after the new pair is published.
        assertTrue(oldMain.closed)
        assertTrue(oldGlobal.closed)
        assertFalse(newMain.closed)
        assertFalse(newGlobal.closed)
    }

    @Test
    fun `failed main build closes the new global shell and keeps old shells published`() {
        val oldMain = FakeShell("old-main")
        val oldGlobal = FakeShell("old-global")
        val newGlobal = FakeShell("new-global")
        var published = false
        var oldMainClosed = false
        var oldGlobalClosed = false

        try {
            refreshShellCaches(
                currentMain = oldMain,
                currentGlobal = oldGlobal,
                buildGlobalShell = { newGlobal },
                buildMainShell = { throw IllegalStateException("shell creation failed") },
                resetMainShellRegistration = { },
            ) { _, _ -> published = true }
            throw AssertionError("refresh must propagate the build failure")
        } catch (expected: IllegalStateException) {
            // expected
        }

        assertFalse(published)
        assertTrue("new global shell must not leak", newGlobal.closed)
        assertFalse("old main shell must stay published", oldMain.closed)
        assertFalse(oldGlobal.closed)
        assertFalse(oldMainClosed)
        assertFalse(oldGlobalClosed)
    }

    @Test
    fun `stale shell close failures never break the refresh`() {
        val oldMain = FakeShell("old-main").apply { closeThrows = true }
        val oldGlobal = FakeShell("old-global")
        val newMain = FakeShell("new-main")
        val newGlobal = FakeShell("new-global")
        var publishedMain: Shell? = null

        refreshShellCaches(
            currentMain = oldMain,
            currentGlobal = oldGlobal,
            buildGlobalShell = { newGlobal },
            buildMainShell = { newMain },
            resetMainShellRegistration = { },
        ) { main, _ -> publishedMain = main }

        assertSame(newMain, publishedMain)
        assertTrue(oldGlobal.closed)
    }
}
