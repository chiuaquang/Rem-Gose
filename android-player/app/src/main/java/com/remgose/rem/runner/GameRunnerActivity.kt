package com.remgose.rem.runner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.remgose.rem.core.RuntimeCatalog
import com.remgose.rem.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Diem vao chay game - chay o process :game0 (xem AndroidManifest),
 * tach khoi process thu vien nen game crash khong keo theo app chinh.
 * 1 process chi phuc vu 1 game tai 1 thoi diem (hoc tu RenDroid).
 *
 * V1: kiem tra game + runtime, chuan bi moi truong ANDROID_PUBLIC/PRIVATE
 * va thu muc save rieng, roi chuyen sang PythonSDLActivity cua runtime.
 * PythonSDLActivity nam trong runtime module (librenpython) - khi runtime
 * chua duoc tich hop, man hinh nay bao ro trang thai thay vi crash.
 */
class GameRunnerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gameName = intent.getStringExtra(EXTRA_GAME) ?: return finish()
        val tv = TextView(this).apply { textSize = 18f; setPadding(48, 48, 48, 48) }
        setContentView(tv)
        CoroutineScope(Dispatchers.Main).launch {
            val db = AppDatabase.get(this@GameRunnerActivity)
            val game = db.gameDao().get(gameName)
            if (game?.path == null) { tv.text = "Game chưa sẵn sàng."; return@launch }
            val version = game.runtimeVersionOverride ?: RuntimeCatalog.autoAdapt(
                com.remgose.rem.core.RenPyVersion.parse(game.renPyVersion))?.version?.toString()
            val runtime = version?.let { db.runtimeDao().get(it) }
            if (runtime?.statusPhase != "READY") {
                tv.text = "Runtime ${version ?: "?"} chưa sẵn sàng.\nHãy cài runtime trong trang chi tiết game rồi quay lại."
                return@launch
            }
            // Moi game 1 thu muc save rieng
            File(filesDir, "saves/$gameName").mkdirs()
            tv.text = "Đang khởi động ${game.displayName ?: gameName} (Ren'Py ${game.renPyVersion})…"
            // TODO V1.1: startActivity(Intent().setClassName(packageName,
            //   "org.renpy.android.PythonSDLActivity") + extras: basedir=game.path,
            //   ANDROID_PUBLIC=saves dir) sau khi runtime module duoc noi vao.
        }
    }
    companion object {
        const val EXTRA_GAME = "game_name"
        fun intent(context: Context, gameName: String) =
            Intent(context, GameRunnerActivity::class.java).putExtra(EXTRA_GAME, gameName)
    }
}
