package com.remgose.rem.runner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.remgose.rem.core.RenPyVersion
import com.remgose.rem.core.RuntimeCatalog
import com.remgose.rem.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Cong kiem soat truoc khi chay game - chay o process :game0 (xem
 * AndroidManifest), tach khoi process thu vien nen game crash khong keo
 * theo app chinh.
 *
 * Nhiem vu: tim game, chon engine (RuntimeCatalog.engineFor), ghi file
 * filesDir/remgose_game.txt (dong 1 = thu muc game/, dong 2 = thu muc save
 * rieng cua game) de main.py cua runtime doc, roi mo GameEngineActivity.
 * Game khong co engine phu hop (7.x) thi bao ro tai day, khong crash.
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
            val gameVersion = RenPyVersion.parse(game.renPyVersion)
            val engine = RuntimeCatalog.engineFor(gameVersion)
            if (engine == null) {
                tv.text = "Game này dùng Ren'Py ${game.renPyVersion} (dòng 7.x).\n" +
                    "Runtime 7.x chưa có trong app - sẽ được tải thêm ở bản cập nhật runtime tiếp theo."
                return@launch
            }
            val gameDir = File(root, "game")
            if (!gameDir.isDirectory) {
                tv.text = "Không thấy thư mục game/ trong dữ liệu game."
                return@launch
            }
            val saveDir = File(gameDir, "saves").apply { mkdirs() }
            // main.py cua runtime (trong private.mp3) doc file nay de biet
            // phai chay game nao + luu save o dau (xem tools-build/make_private.py)
            File(filesDir, "remgose_game.txt").writeText(
                gameDir.absolutePath + "\n" + saveDir.absolutePath + "\n")
            tv.text = "Đang khởi động ${game.displayName ?: gameName} (Ren'Py ${game.renPyVersion})…"
            startActivity(Intent(this@GameRunnerActivity, GameEngineActivity::class.java))
            finish()
        }
    }

    companion object {
        const val EXTRA_GAME = "game_name"
        fun intent(context: Context, gameName: String) =
            Intent(context, GameRunnerActivity::class.java).putExtra(EXTRA_GAME, gameName)
    }
}
