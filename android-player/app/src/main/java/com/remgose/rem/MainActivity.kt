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
                    // Chon THU MUC game Ren'Py (da giai nen san) bang SAF
                    val folderPicker = rememberLauncherForActivityResult(
                        ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
                        if (uri != null) importFolder(uri)
                    }
                    var showImportChooser by remember { mutableStateOf(false) }
                    if (showImportChooser) {
                        val ls = LocalStrings.current
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showImportChooser = false },
                            title = { androidx.compose.material3.Text(ls.importChooserTitle) },
                            text = {
                                androidx.compose.foundation.layout.Column {
                                    androidx.compose.material3.TextButton(onClick = {
                                        showImportChooser = false
                                        picker.launch(arrayOf("application/zip", "application/x-zip-compressed"))
                                    }) { androidx.compose.material3.Text("🗜  " + ls.importFromZip) }
                                    androidx.compose.material3.TextButton(onClick = {
                                        showImportChooser = false
                                        folderPicker.launch(null)
                                    }) { androidx.compose.material3.Text("📁  " + ls.importFromFolder) }
                                }
                            },
                            confirmButton = {
                                androidx.compose.material3.TextButton(onClick = { showImportChooser = false }) {
                                    androidx.compose.material3.Text(ls.cancel)
                                }
                            })
                    }
                    LibraryScreen(
                        games = games,
                        onImportClick = { showImportChooser = true },
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

    /** Nhap game tu thu muc nguoi dung chon (SAF tree): giu quyen doc ben vung roi giao worker copy. */
    private fun importFolder(uri: Uri) {
        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val rawName = androidx.documentfile.provider.DocumentFile.fromTreeUri(this, uri)?.name ?: "game"
        val name = rawName.replace(Regex("[^A-Za-z0-9_.-]"), "_")
        WorkManager.getInstance(this).enqueue(
            OneTimeWorkRequestBuilder<GameImportWorker>()
                .setInputData(workDataOf(
                    "source_uri" to uri.toString(), "tree_uri" to uri.toString(), "name" to name)).build())
        Toast.makeText(this, "Đang nhập game $name…", Toast.LENGTH_SHORT).show()
    }
}
