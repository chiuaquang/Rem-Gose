package com.remgose.rem.core

import java.io.File
import java.util.zip.ZipFile

/**
 * Quan ly Mod - hoc tu RenDroid ModManager/ModRepository:
 * - ModFileAction: ADD (file moi) / REPLACE (ghi de file goc)
 * - Preview truoc khi nhap: dem ADD/REPLACE, liet ke file
 * - Mod nhap xong o trang thai TAT, bat moi ap dung
 * - File goc bi REPLACE duoc sao luu vao _mods/<mod>/backup/ -
 *   tat/go mod thi khoi phuc lai nguyen trang
 * - Xung dot: 2 mod dang BAT cung cham 1 file -> chan bat, bao ro mod nao
 *
 * Bo cuc: gameRoot/_mods/<tenMod>/ { meta.txt, backup/<duong dan goc>, files/ }
 * Ban than file mod khi BAT nam thang trong gameRoot (de Ren'Py doc duoc).
 */
data class ModFileEntry(val path: String, val action: String) // ADD | REPLACE
data class ModMeta(val name: String, val enabled: Boolean, val files: List<ModFileEntry>)
data class ModPreview(val name: String, val files: List<ModFileEntry>) {
    val addCount get() = files.count { it.action == "ADD" }
    val replaceCount get() = files.count { it.action == "REPLACE" }
}

class ModManager(private val gameRoot: File) {
    private val modsRoot = File(gameRoot, "_mods").apply { mkdirs() }

    fun preview(zip: File, modName: String): ModPreview {
        val entries = mutableListOf<ModFileEntry>()
        ZipFile(zip).use { z ->
            z.entries().asSequence().filter { !it.isDirectory }.forEach { e ->
                val exists = File(gameRoot, e.name).exists()
                entries += ModFileEntry(e.name, if (exists) "REPLACE" else "ADD")
            }
        }
        return ModPreview(modName, entries)
    }

    /** Nhap mod (trang thai TAT): giai nen vao _mods/<mod>/files, chua cham vao game. */
    fun importMod(zip: File, modName: String): ModMeta {
        require(modName.isNotBlank()) { "Mod name cannot be empty" }
        val dir = File(modsRoot, modName).apply { mkdirs() }
        SafeUnzip.unzip(zip, File(dir, "files"))
        val preview = preview(zip, modName)
        writeMeta(ModMeta(modName, enabled = false, files = preview.files))
        return ModMeta(modName, false, preview.files)
    }

    fun listMods(): List<ModMeta> = modsRoot.listFiles()?.filter { it.isDirectory }
        ?.mapNotNull { readMeta(it.name) } ?: emptyList()

    /** Kiem tra xung dot voi cac mod dang BAT khac. Tra ve ten file xung dot. */
    fun conflictsFor(mod: ModMeta): List<String> {
        val others = listMods().filter { it.enabled && it.name != mod.name }
        val otherPaths = others.flatMap { it.files }.map { it.path }.toSet()
        return mod.files.map { it.path }.filter { it in otherPaths }
    }

    fun setEnabled(modName: String, enabled: Boolean) {
        val meta = readMeta(modName) ?: return
        val modDir = File(modsRoot, modName)
        val filesDir = File(modDir, "files")
        val backupDir = File(modDir, "backup")
        if (enabled) {
            val conflicts = conflictsFor(meta)
            require(conflicts.isEmpty()) {
                "\"${meta.name}\" conflicts with files from other enabled mods. Disable those mods first."
            }
            meta.files.forEach { entry ->
                val src = File(filesDir, entry.path); val dest = File(gameRoot, entry.path)
                if (entry.action == "REPLACE" && dest.exists()) {
                    val bak = File(backupDir, entry.path); bak.parentFile?.mkdirs(); dest.copyTo(bak, overwrite = true)
                }
                dest.parentFile?.mkdirs(); if (src.exists()) src.copyTo(dest, overwrite = true)
            }
        } else {
            // Tat mod: xoa file ADD, khoi phuc file REPLACE tu backup
            meta.files.forEach { entry ->
                val dest = File(gameRoot, entry.path); val bak = File(backupDir, entry.path)
                when {
                    bak.exists() -> { bak.copyTo(dest, overwrite = true) }
                    entry.action == "ADD" -> dest.delete()
                }
            }
        }
        writeMeta(meta.copy(enabled = enabled))
    }

    /** Go mod: tat (khoi phuc file goc) roi xoa thu muc mod. */
    fun uninstall(modName: String) {
        val meta = readMeta(modName) ?: return
        if (meta.enabled) setEnabled(modName, false)
        File(modsRoot, modName).deleteRecursively()
    }

    private fun metaFile(name: String) = File(File(modsRoot, name), "meta.txt")
    private fun writeMeta(meta: ModMeta) {
        val dir = File(modsRoot, meta.name).apply { mkdirs() }
        metaFile(meta.name).writeText(buildString {
            appendLine("name=${meta.name}"); appendLine("enabled=${meta.enabled}")
            meta.files.forEach { appendLine("${it.action}\t${it.path}") }
        })
        // files/ phai ton tai de setEnabled doc
        File(dir, "files").mkdirs()
    }
    private fun readMeta(name: String): ModMeta? {
        val f = metaFile(name); if (!f.isFile) return null
        val lines = f.readLines(); var enabled = false; val files = mutableListOf<ModFileEntry>()
        lines.forEach { line ->
            when {
                line.startsWith("enabled=") -> enabled = line.removePrefix("enabled=").toBoolean()
                line.contains('\t') -> { val (a, p) = line.split('\t', limit = 2); files += ModFileEntry(p, a) }
            }
        }
        return ModMeta(name, enabled, files)
    }
}
