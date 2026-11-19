package com.remgose.rem.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remgose.rem.core.GameDataManager
import com.remgose.rem.data.AppSettings
import com.remgose.rem.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Trinh duyet file cua game (File Management): duyet thu muc con,
 * xem dung luong, doi ten, xoa file/thu muc (hoi xac nhan truoc).
 * Chi thao tac trong thu muc goc cua game (kiem tra duong dan chuan hoa).
 */
class FileBrowserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootPath = intent.getStringExtra("root_path") ?: return finish()
        val gameName = intent.getStringExtra("game_name") ?: ""
        setContent {
            val store = remember { SettingsStore(this) }
            val settings by store.settings.collectAsState(initial = AppSettings())
            RemGoseTheme(theme = settings.theme, dynamicColor = settings.dynamicColor) {
                CompositionLocalProvider(LocalStrings provides stringsFor(settings.language)) {
                    FileBrowserContent(File(rootPath), gameName, onBack = { finish() })
                }
            }
        }
    }

    companion object {
        fun intent(context: Context, rootPath: String, gameName: String): Intent =
            Intent(context, FileBrowserActivity::class.java)
                .putExtra("root_path", rootPath).putExtra("game_name", gameName)
    }
}

@Composable
private fun FileBrowserContent(root: File, gameName: String, onBack: () -> Unit) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var current by remember { mutableStateOf(root) }
    var refresh by remember { mutableIntStateOf(0) }
    var renameTarget by remember { mutableStateOf<File?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<File?>(null) }
    var menuFor by remember { mutableStateOf<File?>(null) }

    val entries = remember(current, refresh) {
        current.listFiles()?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
            ?: emptyList()
    }
    val atRoot = current.canonicalPath == root.canonicalPath
    BackHandler(enabled = !atRoot) { current = current.parentFile ?: root }

    Column(Modifier.fillMaxSize().background(RemGoseColors.Background)) {
        Box(Modifier.fillMaxWidth().background(RemGoseColors.BrandGradient).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { if (atRoot) onBack() else current = current.parentFile ?: root },
                    contentPadding = PaddingValues(0.dp)) {
                    Text("←", color = androidx.compose.ui.graphics.Color.White, fontSize = 22.sp)
                }
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(gameName, color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold, fontSize = 17.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("/" + current.relativeToOrSelf(root).path,
                        color = androidx.compose.ui.graphics.Color(0xE6FFFFFF), fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(s.emptyFolder, color = RemGoseColors.TextMuted)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(entries, key = { it.absolutePath }) { f ->
                    Row(Modifier.fillMaxWidth()
                        .clickable(enabled = f.isDirectory) { if (f.isDirectory) current = f }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(if (f.isDirectory) "📁" else "📄", fontSize = 20.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(f.name, color = RemGoseColors.TextPrimary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (f.isDirectory) s.dataItems.format(f.list()?.size ?: 0)
                                else GameDataManager.formatSize(f.length()),
                                color = RemGoseColors.TextMuted, fontSize = 12.sp)
                        }
                        Box {
                            TextButton(onClick = { menuFor = f }) { Text("⋯", fontSize = 18.sp) }
                            DropdownMenu(expanded = menuFor == f, onDismissRequest = { menuFor = null }) {
                                DropdownMenuItem(text = { Text(s.rename) }, onClick = {
                                    menuFor = null; renameTarget = f; renameText = f.name
                                })
                                DropdownMenuItem(text = { Text(s.delete, color = RemGoseColors.Error) }, onClick = {
                                    menuFor = null; deleteTarget = f
                                })
                            }
                        }
                    }
                    HorizontalDivider(color = RemGoseColors.SurfaceVariant)
                }
            }
        }
    }

    renameTarget?.let { target ->
        AlertDialog(onDismissRequest = { renameTarget = null },
            title = { Text(s.renameTitle) },
            text = { OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    val newName = renameText.trim()
                    renameTarget = null
                    if (newName.isNotEmpty() && !newName.contains('/') && newName != target.name) {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                runCatching { target.renameTo(File(target.parentFile, newName)) }
                            }
                            refresh++
                        }
                    }
                }) { Text(s.rename) }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text(s.cancel) } })
    }

    deleteTarget?.let { target ->
        AlertDialog(onDismissRequest = { deleteTarget = null },
            title = { Text(s.deleteItemTitle) },
            text = { Text(s.deleteItemMsg.format(target.name)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            // Chi xoa trong thu muc game (sandbox theo duong dan chuan hoa)
                            target.canonicalPath.startsWith(root.canonicalPath) &&
                                if (target.isDirectory) target.deleteRecursively() else target.delete()
                        }
                        if (ok) refresh++
                        else Toast.makeText(context, s.importFailed.format(target.name), Toast.LENGTH_SHORT).show()
                    }
                }) { Text(s.delete, color = RemGoseColors.Error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(s.cancel) } })
    }
}
