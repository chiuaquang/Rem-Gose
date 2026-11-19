package com.remgose.rem.runner

import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.util.Log
import com.remgose.rem.core.RuntimeStore
import org.renpy.android.PythonSDLActivity

/**
 * Activity chay game that: ke thua PythonSDLActivity chinh thuc cua Ren'Py
 * (ma nguon mo, lay tu goi RAPT 8.5.2 o renpy.org).
 *
 * Ho tro 2 nguon engine:
 * - "bundled": librenpython.so 8.5.2 trong APK + private.mp3 (mac dinh).
 * - Runtime tai rieng <version>: System.load() dung file .so da tai ve
 *   filesDir/runtimes/<version>/lib/<abi>/, private bundle do GameRunnerActivity
 *   stage san vao filesDir. getLibraries() tra rong de SDL khong nap ban bundle.
 *
 * Mot process chi nap duoc MOT librenpython.so (theo SONAME). Neu process
 * :game0 da nap .so cua runtime khac, activity tu khoi dong lai process
 * de nap dung ban truoc khi SDL khoi dong.
 */
class GameEngineActivity : PythonSDLActivity() {

    private var useDownloaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val target = intent.getStringExtra(EXTRA_RUNTIME) ?: "bundled"
        if (loadedRuntime != null && loadedRuntime != target) {
            // Da nap .so cua runtime khac trong process nay -> khoi dong lai process
            startActivity(Intent(intent))
            Process.killProcess(Process.myPid())
            return
        }
        if (target != "bundled" && loadedRuntime == null) {
            val so = RuntimeStore.soFile(filesDir, target, RuntimeStore.deviceAbi())
            try {
                System.load(so.absolutePath)
                loadedRuntime = target
                useDownloaded = true
            } catch (e: Throwable) {
                // .so tai ve loi: lui ve engine bundle (game 8.x van chay dang tuong thich).
                // loadedRuntime giu null de lan mo sau thu nap lai dung cach.
                Log.e("RemGose", "Khong nap duoc runtime $target: ${e.message}")
                useDownloaded = false
            }
        } else if (target != "bundled") {
            useDownloaded = true
        }
        if (target == "bundled" && loadedRuntime == null) loadedRuntime = "bundled"
        super.onCreate(savedInstanceState)
    }

    override fun getLibraries(): Array<String> =
        if (useDownloaded) emptyArray() else super.getLibraries()

    companion object {
        const val EXTRA_RUNTIME = "runtime"

        /** Runtime ma process nay da nap .so (null = chua nap). */
        @Volatile
        var loadedRuntime: String? = null
    }
}
