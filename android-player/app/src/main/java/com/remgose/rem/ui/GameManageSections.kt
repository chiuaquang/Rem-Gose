package com.remgose.rem.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remgose.rem.core.GameDataManager
import com.remgose.rem.core.ModManager
import com.remgose.rem.data.GameEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Cac muc quan ly trong Chi tiet game - hoc theo RenDroid:
 * Data Management (N items) · File Management (Storage used)
 * Mod Management · Clear Saves / Persistent / Cache · Danger Zone.
 * Moi thao tac xoa deu hoi xac nhan truoc.
 */
@Composable
fun ManageGameSection(game: GameEntity) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val root = game.path?.let { File(it) }
    var refresh by remember { mutableIntStateOf(0) }
    val manager = remember(game.name, refresh) {
        root?.let { GameDataManager(it, File(context.filesDir, "saves/${game.name}")) }
    }
    val mods = remember(game.name, refresh) { root?.let { ModManager(it).listMods() } ?: emptyList() }
    var confirm by remember { mutableStateOf<String?>(null) } // saves|persistent|cache|reset

    Column {
        Text("Quản lý game", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = RemGoseColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        if (manager == null) {
            Text("Game chưa nhập xong nên chưa có dữ liệu để quản lý.", color = RemGoseColors.TextMuted)
            return@Column
        }
        ManageRow("🗂  Data Management", "${manager.dataItemCount()} items · Export/Import trong bản sau")
        ManageRow("📁  File Management", "Storage used: ${GameDataManager.formatSize(manager.storageUsed())}")
        ManageRow("🧩  Mod Management",
            if (mods.isEmpty()) "No Mods Yet — nhập mod ZIP trong bản sau"
            else mods.joinToString { "${it.name} (${if (it.enabled) "đang bật" else "đang tắt"})" })
        manager.storageBreakdown().take(4).forEach { (name, size) ->
            Text("   $name — ${GameDataManager.formatSize(size)}", color = RemGoseColors.TextMuted, fontSize = 12.sp)
        }

        Spacer(Modifier.height(14.dp))
        Text("Clear Data", fontWeight = FontWeight.Bold, color = RemGoseColors.Accent)
        ManageAction("Clear Saves", "Delete all save files (.save)") { confirm = "saves" }
        ManageAction("Clear Persistent Data", "Delete persistent game data (achievements, settings, etc.)") { confirm = "persistent" }
        ManageAction("Clear Cache", "Delete cached files and temporary data") { confirm = "cache" }

        Spacer(Modifier.height(14.dp))
        Text("Danger Zone", fontWeight = FontWeight.Bold, color = RemGoseColors.Error)
        ManageAction("Reset to Fresh Install", "Delete ALL game data (saves, persistent, cache)",
            color = RemGoseColors.Error) { confirm = "reset" }
    }

    confirm?.let { kind ->
        val (title, desc) = when (kind) {
            "saves" -> "Clear Saves?" to "Toàn bộ file save (.save) của game này sẽ bị xoá, không hoàn tác được."
            "persistent" -> "Clear Persistent Data?" to "Are you sure? Game settings and achievements will be lost."
            "cache" -> "Clear Cache?" to "File cache và dữ liệu tạm sẽ bị xoá."
            else -> "Reset to Fresh Install?" to "Xoá toàn bộ saves, persistent và cache — như vừa cài game mới."
        }
        AlertDialog(onDismissRequest = { confirm = null }, title = { Text(title) }, text = { Text(desc) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    scope.launch {
                        val msg = withContext(Dispatchers.IO) {
                            when (kind) {
                                "saves" -> "Saves cleared (${manager!!.clearSaves()} files)"
                                "persistent" -> "Persistent data cleared"
                                "cache" -> "Cache cleared (${GameDataManager.formatSize(manager!!.clearCache())})"
                                else -> { manager!!.resetToFreshInstall(); "Đã reset về như mới cài" }
                            }
                        }
                        refresh++; Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Xoá", color = RemGoseColors.Error) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Huỷ") } })
    }
}

@Composable
private fun ManageRow(title: String, subtitle: String) {
    Column(Modifier.padding(vertical = 5.dp)) {
        Text(title, color = RemGoseColors.TextPrimary, fontWeight = FontWeight.Medium)
        Text(subtitle, color = RemGoseColors.TextMuted, fontSize = 13.sp)
    }
}

@Composable
private fun ManageAction(title: String, subtitle: String,
                           color: androidx.compose.ui.graphics.Color = RemGoseColors.TextPrimary,
                           onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(vertical = 2.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Text(title, color = color, fontWeight = FontWeight.Medium)
            Text(subtitle, color = RemGoseColors.TextMuted, fontSize = 12.sp)
        }
    }
}
