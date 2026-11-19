package com.remgose.rem.core

import java.io.File
import java.util.zip.ZipInputStream

/** Giai nen ZIP an toan: chong Zip Slip, bao tien do theo byte. */
object SafeUnzip {
    fun unzip(zip: File, destDir: File, onProgress: (Long, Long) -> Unit = { _, _ -> }) {
        val total = zip.length()
        var done = 0L
        ZipInputStream(zip.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val out = File(destDir, entry.name)
                val canonicalDest = destDir.canonicalPath + File.separator
                require(out.canonicalPath.startsWith(canonicalDest) || out.canonicalPath == destDir.canonicalPath) {
                    "Zip entry escapes destination: ${entry.name}"
                }
                if (entry.isDirectory) out.mkdirs() else {
                    out.parentFile?.mkdirs()
                    out.outputStream().buffered().use { zis.copyTo(it) }
                }
                done += entry.compressedSize.coerceAtLeast(0)
                onProgress(done, total)
                zis.closeEntry(); entry = zis.nextEntry
            }
        }
    }
}
