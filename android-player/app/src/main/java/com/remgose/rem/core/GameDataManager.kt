package com.remgose.rem.core

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Quan ly du lieu 1 game - hoc tu RenDroid Game Details:
 * Data Management (export/import/delete), File Management (storage),
 * Clear Saves / Clear Persistent / Clear Cache, Reset to Fresh Install.
 *
 * Bo cuc du lieu cua 1 game trong Rem Gose:
 * - gameRoot/                 : file game (game/, renpy/, ...)
 * - gameRoot/game/saves/      : .save + persistent (Ren'Py ghi tai day tren PC)
 * - appSavesDir/<game>/       : save rieng tren Android (saves/, persistent, cache/)
 */
class GameDataManager(private val gameRoot: File, private val appSavesDir: File) {

    private val saveDirs = listOf(File(gameRoot, "game/saves"), File(appSavesDir, "saves"))
    private val persistentFiles = listOf(
        File(gameRoot, "game/saves/persistent"), File(appSavesDir, "persistent"),
        File(appSavesDir, "saves/persistent"))
    private val cacheDirs = listOf(
        File(gameRoot, "game/cache"), File(appSavesDir, "cache"), File(gameRoot, "cache"))

    fun storageUsed(): Long = sizeOf(gameRoot) + sizeOf(appSavesDir)

    /** So muc cho "Data Management: N items" nhu RenDroid. */
    fun dataItemCount(): Int =
        saveFiles().size + persistentFiles.count { it.exists() }

    fun saveFiles(): List<File> = saveDirs.flatMap { dir ->
        if (dir.isDirectory) dir.walkTopDown().filter { it.isFile && it.extension == "save" }.toList()
        else emptyList()
    }

    /** Phan bo dung luong theo thu muc con lon nhat (File Management). */
    fun storageBreakdown(): List<Pair<String, Long>> {
        val parts = mutableListOf<Pair<String, Long>>()
        gameRoot.listFiles()?.forEach { parts += it.name to sizeOf(it) }
        if (appSavesDir.exists()) parts += "saves (Android)" to sizeOf(appSavesDir)
        return parts.sortedByDescending { it.second }
    }

    fun clearSaves(): Int { var n = 0; saveFiles().forEach { if (it.delete()) n++ }; return n }
    fun clearPersistent(): Int { var n = 0; persistentFiles.forEach { if (it.exists() && it.delete()) n++ }; return n }
    fun clearCache(): Long {
        var freed = 0L
        cacheDirs.forEach { dir -> freed += sizeOf(dir); dir.deleteRecursively(); dir.mkdirs() }
        return freed
    }
    /** Danger Zone > Reset to Fresh Install. */
    fun resetToFreshInstall() { clearSaves(); clearPersistent(); clearCache() }

    /** Export Data: nen saves + persistent thanh 1 ZIP de sao luu/chia se. */
    fun exportData(destZip: File): File {
        destZip.parentFile?.mkdirs()
        ZipOutputStream(destZip.outputStream().buffered()).use { zos ->
            saveDirs.forEach { dir -> addToZip(zos, dir, "saves") }
            persistentFiles.forEach { f ->
                if (f.isFile) { zos.putNextEntry(ZipEntry("persistent/${f.name}")); f.inputStream().use { it.copyTo(zos) }; zos.closeEntry() }
            }
        }
        return destZip
    }

    /** Import Data: ghi de saves/persistent tu ZIP da export truoc do. */
    fun importData(zip: File) {
        val tmp = File(appSavesDir.parentFile, "import_tmp").apply { mkdirs() }
        SafeUnzip.unzip(zip, tmp)
        File(tmp, "saves").takeIf { it.exists() }?.copyRecursively(File(appSavesDir, "saves"), overwrite = true)
        File(tmp, "persistent").takeIf { it.exists() }?.copyRecursively(File(appSavesDir, "saves"), overwrite = true)
        tmp.deleteRecursively()
    }

    private fun addToZip(zos: ZipOutputStream, dir: File, prefix: String) {
        if (!dir.isDirectory) return
        dir.walkTopDown().filter { it.isFile }.forEach { f ->
            zos.putNextEntry(ZipEntry("$prefix/${f.relativeTo(dir).path}"))
            f.inputStream().use { it.copyTo(zos) }; zos.closeEntry()
        }
    }

    companion object {
        fun sizeOf(f: File): Long = when {
            !f.exists() -> 0L
            f.isFile -> f.length()
            else -> f.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
        fun formatSize(bytes: Long): String = when {
            bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1e9)
            bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1e6)
            bytes >= 1_000 -> "%.1f KB".format(bytes / 1e3)
            else -> "$bytes B"
        }
    }
}
