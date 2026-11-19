package com.remgose.rem.worker

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.remgose.rem.core.GameDetector
import com.remgose.rem.core.RuntimeCatalog
import com.remgose.rem.core.SafeUnzip
import com.remgose.rem.core.WrapperGenerator
import com.remgose.rem.data.AppDatabase
import com.remgose.rem.data.GameEntity
import com.remgose.rem.data.RuntimeEntity
import java.io.File

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
            val src = File(applicationContext.cacheDir, "import/$name.zip")
            if (src.isFile) SafeUnzip.unzip(src, dest) { done, total ->
                // WorkManager progress: UI doc bang Flow tu DB + progress nay
                setProgressAsync(workDataOf("done" to done, "total" to total))
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
}

/** Cai runtime on-demand: V1 ghi nhan trang thai + cho tai tu renpy.org (buoc tich hop native o V1.1). */
class RuntimeInstallWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val version = inputData.getString("version") ?: return Result.failure()
        val db = AppDatabase.get(applicationContext)
        val spec = RuntimeCatalog.byVersion(version) ?: return Result.failure()
        db.runtimeDao().setStatus(version, "INSTALLING", "Đang chuẩn bị runtime $version")
        // Engine bundle san 8.5.2 chay duoc moi game 8.x (tuong thich tien cung major).
        // Runtime 7.x se tai them tu renpy.org o ban cap nhat runtime tiep theo.
        val playable = RuntimeCatalog.engineFor(spec.version) != null
        db.runtimeDao().setStatus(version,
            if (playable) "READY" else "ERROR",
            when {
                spec.inBundle -> null
                playable -> "Chạy bằng engine ${RuntimeCatalog.BUNDLED_VERSION} tích hợp sẵn"
                else -> "Runtime $version chưa có trong app - sẽ tải thêm ở bản cập nhật runtime tiếp theo"
            })
        return if (playable) Result.success() else Result.failure()
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
