package me.bmax.apatch.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

/**
 * Behavioral tests for the fork's stamp-managed artifact lifecycle used by
 * app/build.gradle.kts (kpimg/kptools/compat-kpatch URL stamps and the
 * per-.ko KernelPatch version stamps).
 */
class StampManagedDownloadTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun dest(name: String = "kpimg-android") = File(tmp.root, "assets/$name")

    private fun stamp(name: String = "kpimg-android.url") = File(tmp.root, "downloadStamps/$name")

    private fun partialFile(destFile: File) = File(destFile.parentFile, destFile.name + ".part")

    @Test
    fun `complete download publishes artifact and stamp and leaves no partial file`() {
        val destFile = dest()
        val stampFile = stamp()

        StampManagedDownload.ensure(destFile, stampFile, "url-1") { f ->
            f.parentFile.mkdirs()
            f.writeText("binary-payload")
        }

        assertEquals("binary-payload", destFile.readText())
        assertEquals("url-1", stampFile.readText())
        assertFalse(partialFile(destFile).exists())
    }

    @Test
    fun `artifact with matching stamp is reused and download is not invoked`() {
        val destFile = dest()
        val stampFile = stamp()
        destFile.parentFile.mkdirs()
        destFile.writeText("verified-payload")
        stampFile.parentFile.mkdirs()
        stampFile.writeText("url-1")

        var downloadCalled = false
        StampManagedDownload.ensure(destFile, stampFile, "url-1") { _ ->
            downloadCalled = true
        }

        assertFalse(downloadCalled)
        assertEquals("verified-payload", destFile.readText())
    }

    @Test
    fun `artifact without stamp is re-downloaded`() {
        // Deleted artifact with an otherwise-valid stamp must not count as
        // reusable, and the fresh download must restamp it.
        val destFile = dest()
        val stampFile = stamp()
        stampFile.parentFile.mkdirs()
        stampFile.writeText("url-1")

        StampManagedDownload.ensure(destFile, stampFile, "url-1") { f ->
            f.parentFile.mkdirs()
            f.writeText("fresh-payload")
        }

        assertEquals("fresh-payload", destFile.readText())
        assertEquals("url-1", stampFile.readText())
    }

    @Test
    fun `interrupted download keeps previous artifact and stamp and cleans partial file`() {
        // Realistic interrupted re-download: the stamp content changed (version
        // bump), the old artifact is still published, and the download for the
        // new content dies midway. The old artifact and its stamp must survive
        // so the next build retries instead of shipping a partial file.
        val destFile = dest()
        val stampFile = stamp()
        destFile.parentFile.mkdirs()
        destFile.writeText("old-complete-payload")
        stampFile.parentFile.mkdirs()
        stampFile.writeText("url-old")

        try {
            StampManagedDownload.ensure(destFile, stampFile, "url-new") { f ->
                f.parentFile.mkdirs()
                f.writeText("par")
                throw IOException("network interrupted")
            }
            throw AssertionError("ensure must propagate the download failure")
        } catch (expected: IOException) {
            // expected
        }

        assertEquals("old-complete-payload", destFile.readText())
        assertEquals("url-old", stampFile.readText())
        assertFalse(partialFile(destFile).exists())
    }

    @Test
    fun `interrupted download with missing artifact publishes neither artifact nor stamp`() {
        val destFile = dest()
        val stampFile = stamp()

        try {
            StampManagedDownload.ensure(destFile, stampFile, "url-1") { f ->
                f.parentFile.mkdirs()
                f.writeText("par")
                throw IOException("network interrupted")
            }
            throw AssertionError("ensure must propagate the download failure")
        } catch (expected: IOException) {
            // expected
        }

        assertFalse(destFile.exists())
        assertFalse(stampFile.exists())
        assertFalse(partialFile(destFile).exists())
    }

    @Test
    fun `changed stamp content invalidates artifact and re-downloads`() {
        // Version bump: same artifact name, new KernelPatch release.
        val destFile = dest()
        val stampFile = stamp()
        destFile.parentFile.mkdirs()
        destFile.writeText("old-release-payload")
        stampFile.parentFile.mkdirs()
        stampFile.writeText("0.13.8")

        StampManagedDownload.ensure(destFile, stampFile, "0.13.9") { f ->
            f.parentFile.mkdirs()
            f.writeText("0.13.9-payload")
        }

        assertEquals("0.13.9-payload", destFile.readText())
        assertEquals("0.13.9", stampFile.readText())
    }

    @Test
    fun `matching legacy stamp is migrated without re-download`() {
        val destFile = dest("android14-6.1_kernelpatch.ko")
        val stampFile = stamp("android14-6.1_kernelpatch.ko.version")
        val legacyStamp = stamp("jailbreak_ko.version")
        destFile.parentFile.mkdirs()
        destFile.writeText("verified-ko")
        legacyStamp.parentFile.mkdirs()
        legacyStamp.writeText("0.13.9")

        var downloadCalled = false
        StampManagedDownload.ensure(destFile, stampFile, "0.13.9", legacyStamp) { _ ->
            downloadCalled = true
        }

        assertFalse(downloadCalled)
        assertEquals("verified-ko", destFile.readText())
        assertEquals("0.13.9", stampFile.readText())
    }

    @Test
    fun `legacy stamp cannot override per-file stamp on a downgrade`() {
        val destFile = dest("android14-6.1_kernelpatch.ko")
        val stampFile = stamp("android14-6.1_kernelpatch.ko.version")
        val legacyStamp = stamp("jailbreak_ko.version")
        destFile.parentFile.mkdirs()
        destFile.writeText("new-release-ko")
        stampFile.parentFile.mkdirs()
        stampFile.writeText("0.13.10")
        legacyStamp.writeText("0.13.9")

        var downloadCalled = false
        StampManagedDownload.ensure(destFile, stampFile, "0.13.9", legacyStamp) { f ->
            downloadCalled = true
            f.writeText("0.13.9-ko")
        }

        assertEquals(true, downloadCalled)
        assertEquals("0.13.9-ko", destFile.readText())
        assertEquals("0.13.9", stampFile.readText())
    }

    @Test
    fun `invalid publication stamp prevents legacy reuse of a partial artifact`() {
        val destFile = dest("android14-6.1_kernelpatch.ko")
        val stampFile = stamp("android14-6.1_kernelpatch.ko.version")
        val legacyStamp = stamp("jailbreak_ko.version")
        destFile.parentFile.mkdirs()
        destFile.writeText("partial-copy")
        stampFile.parentFile.mkdirs()
        stampFile.writeText("")
        legacyStamp.writeText("0.13.9")

        var downloadCalled = false
        StampManagedDownload.ensure(destFile, stampFile, "0.13.9", legacyStamp) { f ->
            downloadCalled = true
            f.writeText("complete-ko")
        }

        assertEquals(true, downloadCalled)
        assertEquals("complete-ko", destFile.readText())
        assertEquals("0.13.9", stampFile.readText())
    }

    @Test
    fun `mismatching legacy stamp does not prevent re-download`() {
        val destFile = dest()
        val stampFile = stamp()
        val legacyStamp = stamp("jailbreak_ko.version")
        destFile.parentFile.mkdirs()
        destFile.writeText("old-release-ko")
        legacyStamp.parentFile.mkdirs()
        legacyStamp.writeText("0.13.8")

        StampManagedDownload.ensure(destFile, stampFile, "0.13.9", legacyStamp) { f ->
            f.parentFile.mkdirs()
            f.writeText("0.13.9-ko")
        }

        assertEquals("0.13.9-ko", destFile.readText())
        assertEquals("0.13.9", stampFile.readText())
    }
}
