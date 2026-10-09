package me.bmax.apatch.buildlogic

import java.io.File

/**
 * Lifecycle for this fork's stamp-managed downloaded build artifacts
 * (kpimg, kptools, compat kpatch, jailbreak .ko files).
 *
 * An artifact is only treated as reusable when BOTH hold:
 * - the artifact file exists, and
 * - its stamp file exists and records exactly [stampContent] (the source URL
 *   or KernelPatch version the artifact was fetched for),
 * and, because of [ensure], "exists" can only mean a fully downloaded file:
 * bytes always land in a temporary sibling first and are moved into place only
 * after the download succeeded, so an interrupted or failed download can never
 * leave a partial artifact that a later build would silently reuse and ship.
 */
object StampManagedDownload {

    /** Reads a stamp file, mapping missing or unreadable files to null. */
    private fun readStamp(stampFile: File): String? =
        if (stampFile.isFile) runCatching { stampFile.readText() }.getOrNull() else null

    private fun writeStamp(stampFile: File, content: String) {
        stampFile.parentFile?.mkdirs()
        stampFile.writeText(content)
    }

    /**
     * Makes sure [destFile] holds a complete download matching [stampContent].
     *
     * @param download invoked with the temporary file to fill; must either
     *   write it fully or throw (any partial content is discarded and the
     *   previously published artifact, if any, stays untouched).
     * @param legacyStamp optional stamp from an earlier single-stamp scheme;
     *   when it already matches [stampContent] and the artifact is present, it
     *   is migrated to [stampFile] instead of forcing a re-download.
     */
    fun ensure(
        destFile: File,
        stampFile: File,
        stampContent: String,
        legacyStamp: File? = null,
        download: (File) -> Unit,
    ) {
        if (destFile.isFile && readStamp(stampFile) == stampContent) {
            println(" - ${destFile.name} is up-to-date, skipping download.")
            return
        }
        // A legacy stamp only describes files not yet managed individually.
        // It must not override a newer per-file stamp on a version downgrade,
        // or an invalid stamp left by an interrupted publication.
        if (destFile.isFile && !stampFile.exists() && legacyStamp != null && readStamp(legacyStamp) == stampContent) {
            writeStamp(stampFile, stampContent)
            println(" - ${destFile.name} present under legacy stamp, migrated to per-artifact stamp.")
            return
        }
        println(" - Downloading ${destFile.name} ($stampContent) to ${destFile.absolutePath}")
        val tmpFile = File(destFile.parentFile, destFile.name + ".part")
        try {
            download(tmpFile)
            destFile.parentFile?.mkdirs()
            // Keep an invalid per-file stamp BEFORE touching the artifact.
            // Removing it would let an old legacy stamp certify a partial copy
            // on the next build. A failed stamp write aborts before publication.
            writeStamp(stampFile, "")
            if (!tmpFile.renameTo(destFile)) {
                tmpFile.copyTo(destFile, overwrite = true)
                tmpFile.delete()
            }
            // Only now is the new artifact officially reusable for this content.
            writeStamp(stampFile, stampContent)
            println(" - ${destFile.name} downloaded.")
        } finally {
            tmpFile.delete()
        }
    }
}
