package com.remgose.rem

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.remgose.rem.data.AppDatabase
import com.remgose.rem.data.AppSettings
import com.remgose.rem.data.GameEntity
import com.remgose.rem.data.SettingsStore
import com.remgose.rem.runner.GameRunnerActivity
import com.remgose.rem.ui.GameDetailActivity
import com.remgose.rem.ui.LibraryScreen
import com.remgose.rem.ui.LocalStrings
import com.remgose.rem.ui.RemGoseTheme
import com.remgose.rem.ui.stringsFor
import com.remgose.rem.worker.GameImportWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val store = remember { SettingsStore(this) }
            val settings by store.settings.collectAsState(initial = AppSettings())
            RemGoseTheme(theme = settings.theme, dynamicColor = settings.dynamicColor) {
                CompositionLocalProvider(LocalStrings provides stringsFor(settings.language)) {
                    val db = remember { AppDatabase.get(this) }
                    val games by db.gameDao().observeAll().collectAsState(initial = emptyList())
                    val scope = rememberCoroutineScope()

                    // Chon file ZIP game Ren'Py bang bo chon file cua Android (SAF)
                    val picker = rememberLauncherForActivityResult(
                        ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                        if (uri != null) scope.launch { importZip(uri) }
                    }
                    LibraryScreen(
                        games = games,
                        onImportClick = { picker.launch(arrayOf("application/zip", "application/x-zip-compressed")) },
                        onGameClick = { g -> startActivity(Intent(this, GameDetailActivity::class.java).putExtra("game_name", g.name)) },
                        onPlayClick = { g -> startActivity(GameRunnerActivity.intent(this, g.name)) },
                        onSettingsClick = { startActivity(Intent(this, com.remgose.rem.ui.SettingsActivity::class.java)) },
                    )
                }
            }
        }
    }

    /** Copy ZIP vao cache/import roi giao GameImportWorker xu ly nen. */
    private suspend fun importZip(uri: Uri) {
        val strings = stringsFor(SettingsStore(this).settings.first().language)
        val name = (uri.lastPathSegment?.substringAfterLast('/')?.removeSuffix(".zip")
            ?.replace(Regex("[^A-Za-z0-9_.-]"), "_") ?: "game_${System.currentTimeMillis()}")
        val dir = File(cacheDir, "import").apply { mkdirs() }
        val tmp = File(dir, "$name.zip")
        contentResolver.openInputStream(uri)?.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            ?: run { Toast.makeText(this, strings.importReadError, Toast.LENGTH_LONG).show(); return }
        WorkManager.getInstance(this).enqueue(
            OneTimeWorkRequestBuilder<GameImportWorker>()
                .setInputData(workDataOf("source_uri" to uri.toString(), "name" to name)).build())
        Toast.makeText(this, strings.importingToast.format(name), Toast.LENGTH_SHORT).show()
    }
}
