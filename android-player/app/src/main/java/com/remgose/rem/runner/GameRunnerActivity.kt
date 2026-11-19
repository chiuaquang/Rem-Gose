package com.remgose.rem.runner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.remgose.rem.R
import com.remgose.rem.core.RenPyVersion
import com.remgose.rem.core.RuntimeCatalog
import com.remgose.rem.core.RuntimeStore
import com.remgose.rem.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Cong kiem soat truoc khi chay game - chay o process CHINH (khong phai
 * :game0) de song sot khi engine process tu khoi dong lai luc doi runtime.
 *
 * Nhiem vu:
 * 1. Chon runtime cho game: uu tien ban TAI RIENG dung phien ban (override /
 *    auto-adapt) neu da tai xong; khong thi dung engine bundle 8.5.2 cho
 *    game 8.x; game 7.x chua tai runtime thi bao ro, khong crash.
 * 2. "Stage" private bundle cua runtime da chon vao filesDir (PythonSDLActivity
 *    cua Ren'Py chi doc private o filesDir): ban tai rieng thi copy private/
 *    sang + gia lap private.version cua ban bundle de engine khong giai nen
 *    de; ban bundle thi xoa ban stage cu de engine tu giai nen private.mp3.
 * 3. Ghi filesDir/remgose_game.txt (dong 1 = thu muc game/, dong 2 = save rieng)
 *    roi mo GameEngineActivity kem ten runtime de no nap dung librenpython.so.
 */
class GameRunnerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gameName = intent.getStringExtra(EXTRA_GAME) ?: return finish()
        val tv = TextView(this).apply { textSize = 18f; setPadding(48, 48, 48, 48) }
        setContentView(tv)
        tv.text = "Đang khởi động…"
        CoroutineScope(Dispatchers.Main).launch {
            val db = AppDatabase.get(this@GameRunnerActivity)
            val game = db.gameDao().get(gameName)
            val root = game?.path?.let { File(it) }
            if (game == null || root == null || !root.isDirectory) {
                tv.text = "Game chưa sẵn sàng."
                return@launch
            }
            val gameDir = File(root, "game")
            if (!gameDir.isDirectory) {
                tv.text = "Không thấy thư mục game/ trong dữ liệu game."
                return@launch
            }
            val gameVersion = RenPyVersion.parse(game.renPyVersion)
            val preferred = game.runtimeVersionOverride
                ?: RuntimeCatalog.autoAdapt(gameVersion)?.version?.toString()
            val useDownloaded = preferred != null && withContext(Dispatchers.IO) {
                RuntimeStore.isReady(filesDir, preferred)
            }
            val runtimeId = when {
                useDownloaded -> preferred!!
                RuntimeCatalog.engineFor(gameVersion) != null -> "bundled"
                preferred != null -> {
                    tv.text = "Game này cần runtime Ren'Py $preferred.\n" +
                        "Hãy mở Cài đặt → Quản lý Runtime, bấm vào Ren'Py $preferred để tải về (khoảng 200MB), rồi chơi lại."
                    return@launch
                }
                else -> {
                    tv.text = "Chưa rõ phiên bản Ren'Py của game này.\n" +
                        "Hãy mở trang chi tiết game và chọn runtime thủ công."
                    return@launch
                }
            }
            withContext(Dispatchers.IO) { stageRuntime(runtimeId) }
            val saveDir = File(gameDir, "saves").apply { mkdirs() }
            File(filesDir, "remgose_game.txt").writeText(
                gameDir.absolutePath + "\n" + saveDir.absolutePath + "\n")
            tv.text = "Đang khởi động ${game.displayName ?: gameName} (Ren'Py ${game.renPyVersion ?: "?"})…"
            startActivity(Intent(this@GameRunnerActivity, GameEngineActivity::class.java)
                .putExtra(GameEngineActivity.EXTRA_RUNTIME, runtimeId))
            finish()
        }
    }

    /**
     * Dua private bundle cua runtime [runtimeId] vao filesDir.
     * Danh dau bang remgose_runtime.txt de chi stage lai khi doi runtime.
     */
    private fun stageRuntime(runtimeId: String) {
        val marker = File(filesDir, "remgose_runtime.txt")
        val current = runCatching { marker.readText().trim() }.getOrNull()
        if (current == runtimeId) return
        // Xoa cay private dang stage (chi 3 muc cua engine, khong dung du lieu khac)
        File(filesDir, "main.py").delete()
        File(filesDir, "renpy").deleteRecursively()
        File(filesDir, "lib").deleteRecursively()
        if (runtimeId == "bundled") {
            // Xoa private.version -> PythonSDLActivity tu giai nen private.mp3 bundle
            File(filesDir, "private.version").delete()
        } else {
            RuntimeStore.privateDir(filesDir, runtimeId).listFiles()?.forEach { child ->
                child.copyRecursively(File(filesDir, child.name), overwrite = true)
            }
            // Gia lap version cua ban bundle de engine khong giai nen de len ban tai rieng
            File(filesDir, "private.version").writeText(getString(R.string.private_version))
        }
        marker.writeText(runtimeId)
    }

    companion object {
        const val EXTRA_GAME = "game_name"
        fun intent(context: Context, gameName: String) =
            Intent(context, GameRunnerActivity::class.java).putExtra(EXTRA_GAME, gameName)
    }
}
