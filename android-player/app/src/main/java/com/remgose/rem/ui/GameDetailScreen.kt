package com.remgose.rem.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.compose.AsyncImage
import com.remgose.rem.core.RuntimeCatalog
import com.remgose.rem.data.AppDatabase
import com.remgose.rem.data.GameEntity
import com.remgose.rem.runner.GameRunnerActivity
import com.remgose.rem.worker.GameDeleteWorker
import com.remgose.rem.worker.RuntimeInstallWorker
import kotlinx.coroutines.launch
import java.io.File

/** Chi tiet game: bia lon, thong tin runtime, doi runtime thu cong, Choi / Xoa. */
class GameDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val name = intent.getStringExtra("game_name") ?: return finish()
        setContent {
            RemGoseTheme {
                val scope = rememberCoroutineScope()
                var game by remember { mutableStateOf<GameEntity?>(null) }
                LaunchedEffect(name) { game = AppDatabase.get(this@GameDetailActivity).gameDao().get(name) }
                game?.let { g ->
                    GameDetailContent(g,
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

@Composable
private fun GameDetailContent(game: GameEntity, onPlay: () -> Unit,
                              onRuntimeChange: (String) -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val effectiveRuntime = game.runtimeVersionOverride
        ?: RuntimeCatalog.autoAdapt(com.remgose.rem.core.RenPyVersion.parse(game.renPyVersion))?.version?.toString()
    Column(Modifier.fillMaxSize().background(RemGoseColors.Background)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(RemGoseColors.SurfaceVariant)) {
            game.icon?.let { AsyncImage(model = File(it), contentDescription = null,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        }
        Column(Modifier.padding(20.dp)) {
            Text(game.displayName ?: game.name, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                color = RemGoseColors.TextPrimary)
            Spacer(Modifier.height(6.dp))
            // Giong RenDroid: Compatible: 8.3.7 (detected 8.3.4)
            Text("Runtime Version", fontWeight = FontWeight.SemiBold, color = RemGoseColors.TextPrimary)
            Text("Compatible: ${effectiveRuntime ?: "—"} (detected ${game.renPyVersion ?: "?"})",
                color = RemGoseColors.TextMuted)
            Text("Trạng thái: ${game.statusPhase}", color = RemGoseColors.TextMuted)
            game.statusMessage?.let { Text(it, color = RemGoseColors.TextMuted, fontSize = 13.sp) }
            Spacer(Modifier.height(16.dp))
            Text("Đổi runtime thủ công", fontWeight = FontWeight.SemiBold, color = RemGoseColors.TextPrimary)
            Box {
                OutlinedButton(onClick = { expanded = true }) { Text(effectiveRuntime ?: "Chọn runtime") }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    RuntimeCatalog.specs.forEach { spec ->
                        DropdownMenuItem(
                            text = { Text("Ren'Py ${spec.version}" + if (spec.inBundle) " · có sẵn" else " · tải thêm") },
                            onClick = { expanded = false; onRuntimeChange(spec.version.toString()) })
                    }
                }
            }
            // Quan ly du lieu nhu RenDroid: Data / File / Mod / Clear / Danger Zone
            Spacer(Modifier.height(20.dp))
            ManageGameSection(game)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RemGoseColors.AccentViolet)) {
                Text("▶  Chơi ngay", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RemGoseColors.Error)) {
                Text("Xoá game")
            }
        }
    }
}
