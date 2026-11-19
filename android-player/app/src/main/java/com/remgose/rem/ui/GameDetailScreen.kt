package com.remgose.rem.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.compose.AsyncImage
import com.remgose.rem.core.GameDetector
import com.remgose.rem.core.RenPyVersion
import com.remgose.rem.core.RuntimeCatalog
import com.remgose.rem.data.AppDatabase
import com.remgose.rem.data.AppSettings
import com.remgose.rem.data.GameEntity
import com.remgose.rem.data.SettingsStore
import com.remgose.rem.runner.GameRunnerActivity
import com.remgose.rem.worker.GameDeleteWorker
import com.remgose.rem.worker.RuntimeInstallWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Chi tiet game: header gradient + anh bia, chip phien ban/trang thai,
 * nut Choi lon, the Runtime, khu Quan ly game (Data/File/Mod/Clear/Danger).
 * Mo trang nay se tu nhan dien lai phien ban Ren'Py + icon neu luc nhap
 * game chua doc duoc (game doi moi ghi version trong renpy/vc_version.py).
 */
class GameDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val name = intent.getStringExtra("game_name") ?: return finish()
        setContent {
            val store = remember { SettingsStore(this) }
            val settings by store.settings.collectAsState(initial = AppSettings())
            RemGoseTheme(theme = settings.theme, dynamicColor = settings.dynamicColor) {
                CompositionLocalProvider(LocalStrings provides stringsFor(settings.language)) {
                    val scope = rememberCoroutineScope()
                    var game by remember { mutableStateOf<GameEntity?>(null) }
                    LaunchedEffect(name) {
                        val dao = AppDatabase.get(this@GameDetailActivity).gameDao()
                        var g = dao.get(name)
                        // Nhan dien lai neu thieu version/icon (game nhap o ban cu)
                        if (g != null && (g.renPyVersion == null || g.icon == null) && g.path != null) {
                            g = withContext(Dispatchers.IO) {
                                val detected = runCatching {
                                    GameDetector.detect(File(g!!.path!!)).getOrThrow()
                                }.getOrNull()
                                if (detected != null) {
                                    val updated = g!!.copy(
                                        renPyVersion = g!!.renPyVersion ?: detected.renPyVersion?.toString(),
                                        icon = g!!.icon ?: detected.iconFile?.absolutePath,
                                    )
                                    if (updated != g) { dao.upsert(updated); updated } else g!!
                                } else g!!
                            }
                        }
                        game = g
                    }
                    game?.let { g ->
                        GameDetailContent(g,
                            onBack = { finish() },
                            onPlay = { startActivity(GameRunnerActivity.intent(this, g.name)) },
                            onRuntimeChange = { version ->
                                scope.launch {
                                    AppDatabase.get(this@GameDetailActivity).gameDao().setRuntimeOverride(g.name, version)
                                    WorkManager.getInstance(this@GameDetailActivity).enqueue(
                                        OneTimeWorkRequestBuilder<RuntimeInstallWorker>()
                                            .setInputData(workDataOf("version" to version)).build())
                                    game = AppDatabase.get(this@GameDetailActivity).gameDao().get(name)
                                }
                            },
                            onDelete = {
                                WorkManager.getInstance(this).enqueue(
                                    OneTimeWorkRequestBuilder<GameDeleteWorker>()
                                        .setInputData(workDataOf("name" to g.name)).build())
                                finish()
                            })
                    }
                }
            }
        }
    }
}

@Composable
private fun GameDetailContent(game: GameEntity, onBack: () -> Unit, onPlay: () -> Unit,
                              onRuntimeChange: (String) -> Unit, onDelete: () -> Unit) {
    val s = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val effectiveRuntime = game.runtimeVersionOverride
        ?: RuntimeCatalog.autoAdapt(RenPyVersion.parse(game.renPyVersion))?.version?.toString()
    Column(Modifier.fillMaxSize().background(RemGoseColors.Background).verticalScroll(rememberScrollState())) {
        // ---- Header gradient + bia game ----
        Box(Modifier.fillMaxWidth().background(RemGoseColors.BrandGradient)) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                        Text("←", color = Color.White, fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("Rem Gose", color = Color(0xE6FFFFFF), fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(96.dp).clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                        if (game.icon != null) {
                            AsyncImage(model = File(game.icon!!), contentDescription = null,
                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Text((game.displayName ?: game.name).trim().firstOrNull()?.uppercase() ?: "?",
                                fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(game.displayName ?: game.name, fontSize = 21.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            InfoChip("Ren'Py ${game.renPyVersion ?: "?"}")
                            InfoChip(game.statusPhase)
                        }
                        Spacer(Modifier.height(6.dp))
                        InfoChip("Runtime ${effectiveRuntime ?: "—"}")
                    }
                }
            }
        }
        Column(Modifier.padding(20.dp)) {
            Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RemGoseColors.AccentViolet)) {
                Text(s.playNow, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(20.dp))
            // ---- The Runtime ----
            Card(shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RemGoseColors.Surface)) {
                Column(Modifier.padding(16.dp)) {
                    Text(s.runtimeVersion, fontWeight = FontWeight.SemiBold, color = RemGoseColors.TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(s.compatible.format(effectiveRuntime ?: "—", game.renPyVersion ?: "?"),
                        color = RemGoseColors.TextMuted, fontSize = 13.sp)
                    Text(s.statusLabel.format(game.statusPhase), color = RemGoseColors.TextMuted, fontSize = 13.sp)
                    game.statusMessage?.let { Text(it, color = RemGoseColors.TextMuted, fontSize = 13.sp) }
                    if (game.renPyVersion == null) {
                        Text(s.unknownVersionHint, color = RemGoseColors.Warning, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(s.changeRuntime, fontWeight = FontWeight.SemiBold, color = RemGoseColors.TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Box {
                        OutlinedButton(onClick = { expanded = true }) { Text(effectiveRuntime ?: s.chooseRuntime) }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            RuntimeCatalog.specs.forEach { spec ->
                                DropdownMenuItem(
                                    text = { Text("Ren'Py ${spec.version}" + if (spec.inBundle) s.specBundled else s.specDownload) },
                                    onClick = { expanded = false; onRuntimeChange(spec.version.toString()) })
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            ManageGameSection(game)
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RemGoseColors.Error)) {
                Text(s.deleteGame)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (confirmDelete) {
        AlertDialog(onDismissRequest = { confirmDelete = false },
            title = { Text(s.deleteGameTitle) }, text = { Text(s.deleteGameMsg) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) {
                Text(s.delete, color = RemGoseColors.Error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(s.cancel) } })
    }
}

@Composable
private fun InfoChip(text: String) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.22f))
        .padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}
