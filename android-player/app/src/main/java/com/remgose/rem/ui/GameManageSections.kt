package com.remgose.rem.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remgose.rem.core.GameDataManager
import com.remgose.rem.core.ModManager
import com.remgose.rem.core.ModMeta
import com.remgose.rem.data.GameEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Cac muc quan ly trong Chi tiet game - hoc theo RenDroid:
 * Data Management (Export/Import that bang SAF) · File Management (Storage used)
 * Mod Management (nhap ZIP, bat/tat, go - co canh bao xung dot)
 * Clear Saves / Persistent / Cache · Danger Zone. Moi thao tac xoa deu hoi truoc.
 */
@Composable
fun ManageGameSection(game: GameEntity) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val root = game.path?.let { File(it) }
    var refresh by remember { mutableIntStateOf(0) }
    val manager = remember(game.name, refresh) {
        root?.let { GameDataManager(it, File(context.filesDir, "saves/${game.name}")) }
    }
    val modManager = remember(game.name, refresh) { root?.let { ModManager(it) } }
    val mods = remember(game.name, refresh) { modManager?.listMods() ?: emptyList() }
    var confirm by remember { mutableStateOf<String?>(null) } // saves|persistent|cache|reset
    var conflictMod by remember { mutableStateOf<ModMeta?>(null) }
    var uninstallMod by remember { mutableStateOf<ModMeta?>(null) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    fun copyUriToCache(uri: Uri, suffix: String): File? {
        val f = File(context.cacheDir, "import_${System.currentTimeMillis()}$suffix")
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input -> f.outputStream().use { input.copyTo(it) } }
            f
        }.getOrNull()
    }

    // Xuat du lieu: tao ZIP trong cache roi ghi ra file nguoi dung chon (SAF)
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        if (uri != null && manager != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val zip = manager.exportData(File(context.cacheDir, "${game.name}-data.zip"))
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        zip.inputStream().use { it.copyTo(out) }
                    }
                }.isSuccess
            }
            if (ok) toast(s.exportOk)
        }
    }
    // Nhap du lieu tu ZIP nguoi dung chon
    val importDataLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null && manager != null) scope.launch {
            val msg = withContext(Dispatchers.IO) {
                val f = copyUriToCache(uri, ".zip")
                if (f == null) s.importFailed.format("?")
                else runCatching { manager.importData(f); s.importOk }
                    .getOrElse { s.importFailed.format(it.message ?: "?") }
            }
            refresh++; toast(msg)
        }
    }
    // Nhap mod tu ZIP (mac dinh tat nhu RenDroid)
    val importModLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null && modManager != null) scope.launch {
            val msg = withContext(Dispatchers.IO) {
                val f = copyUriToCache(uri, ".zip")
                if (f == null) s.importFailed.format("?")
                else {
                    val modName = (uri.lastPathSegment?.substringAfterLast('/')?.removeSuffix(".zip")
                        ?.replace(Regex("[^A-Za-z0-9_.-]"), "_") ?: "mod")
                    runCatching { modManager.importMod(f, modName); s.modImportOk }
                        .getOrElse { s.importFailed.format(it.message ?: "?") }
                }
            }
            refresh++; toast(msg)
        }
    }

    Column {
        Text(s.gameManage, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = RemGoseColors.TextPrimary)
        Spacer(Modifier.height(10.dp))
        if (manager == null || modManager == null) {
            Text(s.gameNotReady, color = RemGoseColors.TextMuted)
            return@Column
        }
        // ---- Data Management ----
        ManageCard {
            ManageRow(s.dataManage, s.dataItems.format(manager.dataItemCount()))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { exportLauncher.launch("${game.name}-data.zip") }) { Text(s.exportData) }
                OutlinedButton(onClick = { importDataLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed")) }) { Text(s.importData) }
            }
        }
        Spacer(Modifier.height(10.dp))
        // ---- File Management ----
        ManageCard {
            ManageRow(s.fileManage, s.storageUsed.format(GameDataManager.formatSize(manager.storageUsed())))
            manager.storageBreakdown().forEach { (name, size) ->
                Text("   $name — ${GameDataManager.formatSize(size)}", color = RemGoseColors.TextMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = {
                context.startActivity(FileBrowserActivity.intent(context, root!!.absolutePath, game.name))
            }) { Text(s.browseFiles) }
        }
        Spacer(Modifier.height(10.dp))
        // ---- Mod Management ----
        ManageCard {
            ManageRow(s.modManage, if (mods.isEmpty()) s.noModsYet
                else mods.joinToString { "${it.name} (${if (it.enabled) s.modOn else s.modOff})" })
            mods.forEach { mod ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(mod.name, color = RemGoseColors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("${mod.files.size} files · ${if (mod.enabled) s.modOn else s.modOff}",
                            color = RemGoseColors.TextMuted, fontSize = 12.sp)
                    }
                    TextButton(onClick = { uninstallMod = mod }) {
                        Text(s.uninstallMod, color = RemGoseColors.Error, fontSize = 13.sp)
                    }
                    Switch(checked = mod.enabled, onCheckedChange = { want ->
                        if (want && modManager.conflictsFor(mod).isNotEmpty()) {
                            conflictMod = mod
                        } else scope.launch {
                            withContext(Dispatchers.IO) { runCatching { modManager.setEnabled(mod.name, want) } }
                            refresh++
                        }
                    })
                }
            }
            OutlinedButton(onClick = { importModLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed")) }) {
                Text(s.importMod)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(s.clearData, fontWeight = FontWeight.Bold, color = RemGoseColors.Accent)
        ManageAction(s.clearSaves, s.clearSavesSub) { confirm = "saves" }
        ManageAction(s.clearPersistent, s.clearPersistentSub) { confirm = "persistent" }
        ManageAction(s.clearCache, s.clearCacheSub) { confirm = "cache" }

        Spacer(Modifier.height(14.dp))
        Text(s.dangerZone, fontWeight = FontWeight.Bold, color = RemGoseColors.Error)
        ManageAction(s.resetFresh, s.resetFreshSub, color = RemGoseColors.Error) { confirm = "reset" }
    }

    confirm?.let { kind ->
        val (title, desc) = when (kind) {
            "saves" -> s.confirmSavesTitle to s.confirmSavesMsg
            "persistent" -> s.confirmPersistentTitle to s.confirmPersistentMsg
            "cache" -> s.confirmCacheTitle to s.confirmCacheMsg
            else -> s.confirmResetTitle to s.confirmResetMsg
        }
        AlertDialog(onDismissRequest = { confirm = null }, title = { Text(title) }, text = { Text(desc) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    scope.launch {
                        val msg = withContext(Dispatchers.IO) {
                            when (kind) {
                                "saves" -> s.savesCleared.format(manager!!.clearSaves())
                                "persistent" -> { manager!!.clearPersistent(); s.persistentCleared }
                                "cache" -> s.cacheClearedMsg.format(GameDataManager.formatSize(manager!!.clearCache()))
                                else -> { manager!!.resetToFreshInstall(); s.resetDone }
                            }
                        }
                        refresh++; toast(msg)
                    }
                }) { Text(s.clearBtn, color = RemGoseColors.Error) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(s.cancel) } })
    }

    conflictMod?.let { mod ->
        AlertDialog(onDismissRequest = { conflictMod = null },
            title = { Text(s.modConflictTitle) }, text = { Text(s.modConflictMsg.format(mod.name)) },
            confirmButton = {
                TextButton(onClick = {
                    conflictMod = null
                    scope.launch {
                        withContext(Dispatchers.IO) { runCatching { modManager!!.setEnabled(mod.name, true) } }
                        refresh++
                    }
                }) { Text(s.enableAnyway) }
            },
            dismissButton = { TextButton(onClick = { conflictMod = null }) { Text(s.cancel) } })
    }

    uninstallMod?.let { mod ->
        AlertDialog(onDismissRequest = { uninstallMod = null },
            title = { Text(s.uninstallMod) }, text = { Text(mod.name) },
            confirmButton = {
                TextButton(onClick = {
                    uninstallMod = null
                    scope.launch {
                        withContext(Dispatchers.IO) { runCatching { modManager!!.uninstall(mod.name) } }
                        refresh++
                    }
                }) { Text(s.delete, color = RemGoseColors.Error) }
            },
            dismissButton = { TextButton(onClick = { uninstallMod = null }) { Text(s.cancel) } })
    }
}

@Composable
private fun ManageCard(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RemGoseColors.Surface)) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

@Composable
private fun ManageRow(title: String, subtitle: String) {
    Column(Modifier.padding(bottom = 6.dp)) {
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
