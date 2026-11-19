package com.remgose.rem.worker

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.remgose.rem.core.GameDataManager
import com.remgose.rem.core.GameDetector
import com.remgose.rem.core.RuntimeCatalog
import com.remgose.rem.core.RuntimeStore
import com.remgose.rem.core.SafeUnzip
import com.remgose.rem.core.WrapperGenerator
import com.remgose.rem.data.AppDatabase
import com.remgose.rem.data.GameEntity
import com.remgose.rem.data.RuntimeEntity
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipFile

/**
 * Nhap game o nen nhu RenDroid (GameImportWorker):
 * copy/giai nen vao files/games/<name> -> detect -> wrapper -> READY,
 * moi buoc deu ghi statusPhase/statusMessage de UI bao tien do va loi.
 * Dau vao: source_uri (ZIP hoac thu muc da copy tam), name.
 */
class GameImportWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.get(applicationContext)
        val sourceUri = inputData.getString("source_uri") ?: return Result.failure()
        val name = inputData.getString("name") ?: return Result.failure()
        val gamesRoot = File(applicationContext.filesDir, "games").apply { mkdirs() }
        val dest = File(gamesRoot, name)
        return try {
            db.gameDao().upsert(GameEntity(
                name = name, displayName = name, creationTimestamp = System.currentTimeMillis(),
                sourceUri = sourceUri, path = null, renPyVersion = null, statusPhase = "IMPORTING"))
            val treeUri = inputData.getString("tree_uri")
            if (treeUri != null) {
                // Nhap tu THU MUC (SAF): copy toan bo cay thu muc vao dest
                copyTree(Uri.parse(treeUri), dest)
            } else {
                val src = File(applicationContext.cacheDir, "import/$name.zip")
                if (src.isFile) SafeUnzip.unzip(src, dest) { done, total ->
                    // WorkManager progress: UI doc bang Flow tu DB + progress nay
                    setProgressAsync(workDataOf("done" to done, "total" to total))
                }
            }
            val detected = GameDetector.detect(dest).getOrThrow()
            WrapperGenerator.prepare(detected.root)
            val runtime = RuntimeCatalog.autoAdapt(detected.renPyVersion)
            db.gameDao().upsert(GameEntity(
                name = name, displayName = detected.displayName,
                creationTimestamp = System.currentTimeMillis(), sourceUri = sourceUri,
                path = detected.root.absolutePath, renPyVersion = detected.renPyVersion?.toString(),
                icon = detected.iconFile?.absolutePath,
                statusPhase = "READY",
                statusMessage = when {
                    runtime == null -> "Chưa rõ phiên bản Ren'Py - hãy chọn runtime thủ công"
                    !detected.hasRenPyDir -> "Thiếu renpy/ - game thuần Python vẫn có thể chạy"
                    else -> "Runtime: ${runtime.version}"
                }))
            if (runtime != null && db.runtimeDao().get(runtime.version.toString()) == null) {
                db.runtimeDao().upsert(RuntimeEntity(runtime.version.toString(), System.currentTimeMillis(),
                    statusPhase = if (runtime.inBundle) "READY" else "NOT_INSTALLED"))
            }
            Result.success()
        } catch (e: Exception) {
            db.gameDao().setStatus(name, "ERROR", e.message)
            Result.failure()
        }
    }

    /** Copy toan bo cay thu muc SAF vao dest (noi dung ben trong thu muc goc). */
    private fun copyTree(treeUri: Uri, destDir: File) {
        val tree = DocumentFile.fromTreeUri(applicationContext, treeUri)
            ?: error("Cannot open the selected folder")
        destDir.mkdirs()
        fun copyNode(node: DocumentFile, target: File) {
            if (node.isDirectory) {
                target.mkdirs()
                node.listFiles().forEach { child ->
                    copyNode(child, File(target, child.name ?: "unnamed"))
                }
            } else {
                target.parentFile?.mkdirs()
                val input = applicationContext.contentResolver.openInputStream(node.uri)
                    ?: error("Cannot read ${node.name}")
                input.use { inp -> target.outputStream().use { inp.copyTo(it) } }
            }
        }
        tree.listFiles().forEach { child -> copyNode(child, File(destDir, child.name ?: "unnamed")) }
    }
}

/**
 * Cai runtime THAT: tai goi RAPT + SDK chinh thuc tu renpy.org/dl/<version>/,
 * trich librenpython.so (dung ABI cua may) va ghep private bundle
 * (main.py da va + renpy/ + lib/pythonXX/) vao filesDir/runtimes/<version>/.
 * Ban bundle san 8.5.2 nam san trong APK nen khong can tai.
 */
class RuntimeInstallWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val version = inputData.getString("version") ?: return Result.failure()
        val db = AppDatabase.get(applicationContext)
        val spec = RuntimeCatalog.byVersion(version) ?: return Result.failure()
        val filesDir = applicationContext.filesDir
        if (spec.inBundle) {
            db.runtimeDao().setStatus(version, "READY", null)
            return Result.success()
        }
        if (RuntimeStore.isReady(filesDir, version)) {
            db.runtimeDao().setStatus(version, "READY",
                "Đã tải · ${GameDataManager.formatSize(RuntimeStore.sizeBytes(filesDir, version))}")
            return Result.success()
        }
        val runtimeDir = RuntimeStore.runtimeDir(filesDir, version)
        val tmpDir = File(applicationContext.cacheDir, "runtime-$version").apply { mkdirs() }
        return try {
            db.runtimeDao().setStatus(version, "INSTALLING", "Đang tải gói RAPT $version (1/2)…")
            val raptZip = File(tmpDir, "rapt.zip")
            download("https://www.renpy.org/dl/$version/renpy-$version-rapt.zip", raptZip)
            extractNativeLib(raptZip, version)

            db.runtimeDao().setStatus(version, "INSTALLING", "Đang tải gói SDK $version (2/2)…")
            val sdkTar = File(tmpDir, "sdk.tar.bz2")
            download("https://www.renpy.org/dl/$version/renpy-$version-sdk.tar.bz2", sdkTar)

            db.runtimeDao().setStatus(version, "INSTALLING", "Đang ghép runtime $version…")
            extractPrivate(sdkTar, version)
            patchMainPy(RuntimeStore.privateDir(filesDir, version))
            check(RuntimeStore.isReady(filesDir, version)) { "Ghép runtime chưa hoàn chỉnh" }
            tmpDir.deleteRecursively()
            db.runtimeDao().setStatus(version, "READY",
                "Đã tải · ${GameDataManager.formatSize(RuntimeStore.sizeBytes(filesDir, version))}")
            Result.success()
        } catch (e: Exception) {
            runtimeDir.deleteRecursively()
            tmpDir.deleteRecursively()
            db.runtimeDao().setStatus(version, "ERROR", when (e.message) {
                "OLD_FORMAT" -> "Runtime $version dùng định dạng RAPT cũ, chưa hỗ trợ tải"
                else -> "Tải runtime $version thất bại: ${e.message}"
            })
            Result.failure()
        }
    }

    private fun download(url: String, dest: File) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 30_000
        conn.readTimeout = 120_000
        conn.setRequestProperty("User-Agent", "RemGose/0.3.0 Android")
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                error("HTTP ${conn.responseCode}")
            }
            conn.inputStream.use { inp -> dest.outputStream().use { inp.copyTo(it, 64 * 1024) } }
        } finally {
            conn.disconnect()
        }
    }

    /** Trich librenpython.so cho ABI cua may tu goi RAPT. Ban RAPT cu (khong co file nay) -> OLD_FORMAT. */
    private fun extractNativeLib(raptZip: File, version: String) {
        val abi = RuntimeStore.deviceAbi()
        val entryName = "rapt/prototype/renpyandroid/src/main/jniLibs/$abi/librenpython.so"
        ZipFile(raptZip).use { zip ->
            val entry = zip.getEntry(entryName) ?: error("OLD_FORMAT")
            val out = RuntimeStore.soFile(applicationContext.filesDir, version, abi)
            out.parentFile?.mkdirs()
            zip.getInputStream(entry).use { inp -> out.outputStream().use { inp.copyTo(it) } }
        }
    }

    /** Giai nen chon loc SDK: chi renpy.py + renpy/ + lib/pythonXX/ (bo qua cac nen tang khac). */
    private fun extractPrivate(sdkTarBz2: File, version: String) {
        val privateDir = RuntimeStore.privateDir(applicationContext.filesDir, version).apply { mkdirs() }
        val prefix = "renpy-$version-sdk/"
        val libPattern = Regex("lib/python[0-9.]+/.*")
        FileInputStream(sdkTarBz2).use { fis ->
            BZip2CompressorInputStream(BufferedInputStream(fis)).use { bz ->
                TarArchiveInputStream(bz).use { tar ->
                    var entry = tar.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (name.startsWith(prefix)) {
                            val rel = name.removePrefix(prefix)
                            val keep = rel == "renpy.py" || rel.startsWith("renpy/") || libPattern.matches(rel)
                            if (keep && !rel.contains("__pycache__") && !rel.contains("..")) {
                                val out = File(privateDir, rel)
                                if (entry.isDirectory) out.mkdirs()
                                else {
                                    out.parentFile?.mkdirs()
                                    out.outputStream().use { tar.copyTo(it) }
                                }
                            }
                        }
                        entry = tar.nextEntry
                    }
                }
            }
        }
    }

    /**
     * Va engine giong tools-build/make_private.py: doc remgose_game.txt canh main.py.
     * - 8.x: renpy.py co path_to_gamedir (neo "# A list of candidate...") + path_to_saves.
     * - 7.x: renpy.py khong co path_to_gamedir - gamedir nam o renpy/bootstrap.py nen
     *   va ca bootstrap.py; path_to_saves dung ban Python 2 (khong co exist_ok).
     */
    private fun patchMainPy(privateDir: File) {
        val renpyPy = File(privateDir, "renpy.py")
        var text = renpyPy.readText()
        val py2 = File(privateDir, "lib/python2.7").isDirectory
        val anchor2 = "    # Android.\n    if renpy.android:\n        paths = ["
        check(text.contains(anchor2)) { "renpy.py thiếu điểm vá path_to_saves" }
        text = text.replace(anchor2, if (py2) PATCH_SAVES_PY2 else PATCH_SAVES)
        val anchor1 = "    # A list of candidate game directory names."
        if (text.contains(anchor1)) {
            text = text.replace(anchor1, PATCH_GAMEDIR)
        } else {
            patchBootstrap7(privateDir)
        }
        File(privateDir, "main.py").writeText(text)
        renpyPy.delete()
    }

    /** Ren'Py 7.x: gamedir tim trong renpy/bootstrap.py (theo basedir) - chen nhanh Rem Gose len truoc. */
    private fun patchBootstrap7(privateDir: File) {
        val bootstrap = File(privateDir, "renpy/bootstrap.py")
        var text = bootstrap.readText()
        val anchor = "    for i in gamedirs:\n\n        if i == \"renpy\":\n            continue\n\n" +
            "        gamedir = basedir + \"/\" + i\n        if os.path.isdir(gamedir):\n            break\n" +
            "    else:\n        gamedir = basedir"
        check(text.contains(anchor)) { "bootstrap.py thiếu điểm vá gamedir (7.x)" }
        text = text.replace(anchor, PATCH_GAMEDIR_7)
        bootstrap.writeText(text)
    }

    companion object {
        private const val PATCH_GAMEDIR = """
    # Rem Gose: the Java side writes remgose_game.txt next to main.py,
    # line 1 is the selected game's "game" directory.
    try:
        _rg = os.path.join(os.path.dirname(os.path.abspath(__file__)), "remgose_game.txt")
        with open(_rg, "r") as _f:
            _lines = _f.read().splitlines()
        if _lines and os.path.isdir(_lines[0]):
            return _lines[0]
    except Exception:
        pass

    # A list of candidate game directory names."""

        private const val PATCH_SAVES = """    # Android.
    if renpy.android:
        # Rem Gose: per-game save directory (line 2 of remgose_game.txt).
        try:
            _rg = os.path.join(os.path.dirname(os.path.abspath(__file__)), "remgose_game.txt")
            with open(_rg, "r") as _f:
                _lines = _f.read().splitlines()
            if len(_lines) > 1 and _lines[1]:
                os.makedirs(_lines[1], exist_ok=True)
                return _lines[1]
        except Exception:
            pass

        paths = ["""

        /** Ban Python 2.7 cho Ren'Py 7.x (os.makedirs khong co exist_ok). */
        private const val PATCH_SAVES_PY2 = """    # Android.
    if renpy.android:
        # Rem Gose: per-game save directory (line 2 of remgose_game.txt).
        try:
            _rg = os.path.join(os.path.dirname(os.path.abspath(__file__)), "remgose_game.txt")
            with open(_rg, "r") as _f:
                _lines = _f.read().splitlines()
            if len(_lines) > 1 and _lines[1]:
                if not os.path.isdir(_lines[1]):
                    os.makedirs(_lines[1])
                return _lines[1]
        except Exception:
            pass

        paths = ["""

        /** Chen vao renpy/bootstrap.py cua 7.x: uu tien gamedir tu remgose_game.txt. */
        private const val PATCH_GAMEDIR_7 = """    # Rem Gose: the Java side writes remgose_game.txt into the private dir;
    # line 1 is the selected game's "game" directory.
    gamedir = None
    try:
        with open(os.path.join(basedir, "remgose_game.txt"), "r") as _rgf:
            _rg_lines = _rgf.read().splitlines()
        if _rg_lines and os.path.isdir(_rg_lines[0]):
            gamedir = _rg_lines[0]
    except Exception:
        gamedir = None

    if gamedir is None:
        for i in gamedirs:

            if i == "renpy":
                continue

            gamedir = basedir + "/" + i
            if os.path.isdir(gamedir):
                break
        else:
            gamedir = basedir"""
    }
}

/** Xoa game: soft-delete trong DB truoc, xoa file sau de UI cap nhat ngay. */
class GameDeleteWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val name = inputData.getString("name") ?: return Result.failure()
        val db = AppDatabase.get(applicationContext)
        val game = db.gameDao().get(name)
        db.gameDao().softDelete(name, System.currentTimeMillis())
        game?.path?.let { runCatching { File(it).deleteRecursively() } }
        return Result.success()
    }
}
