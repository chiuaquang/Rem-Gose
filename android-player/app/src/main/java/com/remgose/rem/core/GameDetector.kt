package com.remgose.rem.core

import java.io.File

data class DetectedGame(
    val root: File,
    val gameDir: File,
    val renPyVersion: RenPyVersion?,
    val hasRenPyDir: Boolean,
    val iconFile: File?,
    val displayName: String,
)

/**
 * Kiem tra 1 thu muc co phai game Ren'Py khong - hoc tu RenDroid:
 * - Bat buoc co thu muc game/
 * - Thieu renpy/ chi canh bao mem (game thuan Python / dung thu vien trong)
 * - Version doc tu renpy/__init__.py, du phong bang game/script_version.txt
 * - Icon lay tu game/gui/window_icon.png
 */
object GameDetector {
    fun detect(root: File): Result<DetectedGame> {
        // Mot so game dong goi thua 1 lop thu muc: root/<ten>/game
        val effectiveRoot = when {
            File(root, "game").isDirectory -> root
            else -> root.listFiles()?.firstOrNull { File(it, "game").isDirectory } ?: root
        }
        val gameDir = File(effectiveRoot, "game")
        if (!gameDir.isDirectory) {
            val found = effectiveRoot.list()?.joinToString(", ")?.take(200) ?: "(empty)"
            return Result.failure(IllegalArgumentException(
                "Missing 'game/' directory in game root. Found: $found"))
        }
        val renpyDir = File(effectiveRoot, "renpy")
        val version = detectVersion(renpyDir, gameDir)
        val icon = listOf("gui/window_icon.png", "gui/window_icon.jpg", "icon.png")
            .map { File(gameDir, it) }.firstOrNull { it.isFile }
        return Result.success(DetectedGame(
            root = effectiveRoot, gameDir = gameDir, renPyVersion = version,
            hasRenPyDir = renpyDir.isDirectory, iconFile = icon,
            displayName = effectiveRoot.name,
        ))
    }

    private fun detectVersion(renpyDir: File, gameDir: File): RenPyVersion? {
        // renpy/__init__.py chua: version_tuple = (8, 3, 7, ...) hoac version = "8.3.7"
        val initPy = File(renpyDir, "__init__.py")
        if (initPy.isFile) {
            val text = runCatching { initPy.readText() }.getOrDefault("")
            Regex("""version_tuple\s*=\s*\(\s*(\d+),\s*(\d+),\s*(\d+)""").find(text)?.let {
                return RenPyVersion(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt())
            }
            RenPyVersion.parse(text.lineSequence().firstOrNull { it.contains("version") })?.let { return it }
        }
        val sv = File(gameDir, "script_version.txt")
        if (sv.isFile) return RenPyVersion.parse(runCatching { sv.readText() }.getOrNull())
        return null
    }
}
