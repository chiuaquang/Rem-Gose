package com.remgose.rem.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.remgose.rem.core.RuntimeCatalog
import com.remgose.rem.data.AppDatabase
import com.remgose.rem.data.AppSettings
import com.remgose.rem.data.RuntimeEntity
import com.remgose.rem.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Cai dat Rem Gose - cau truc hoc theo RenDroid (anh chup gui 19/11/2026):
 * Appearance: Theme (System Default/Dark/Light) · Dynamic Color · Language
 *             (System/English/Vietnamese/Indonesian/Thai/Singapore/Chinese)
 *             · Prefer landscape
 * Management: Runtime Management (installed, cleanup & download)
 *             · Storage Management (usage & cache cleanup)
 * About:      About (version info & licenses, cham 7 lan mo Developer Mode)
 *             · Advanced Settings (debug mode & log export, Log Viewer)
 * Nguyen tac cua Anh Quang: bam vao cai gi cung phai hoat dong, khong nut chet.
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val store = remember { SettingsStore(this) }
            val s by store.settings.collectAsState(initial = AppSettings())
            RemGoseTheme(theme = s.theme, dynamicColor = s.dynamicColor) {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}

/** Danh sach ngon ngu Anh Quang yeu cau (kem System Default nhu RenDroid). */
val AppLanguages = listOf(
    "system" to "System Default",
    "en" to "English",
    "vi" to "Vietnamese",
    "id" to "Indonesian",
    "th" to "Thailand",
    "sg" to "Singapore",
    "zh" to "Chinese",
)

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }
    val settings by store.settings.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.get(context) }
    val runtimes by db.runtimeDao().observeAll().collectAsState(initial = emptyList())
    val games by db.gameDao().observeAll().collectAsState(initial = emptyList())

    var themeDialog by remember { mutableStateOf(false) }
    var languageDialog by remember { mutableStateOf(false) }
    var openSection by remember { mutableStateOf<String?>(null) }
    var showReset by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }
    var aboutTaps by remember { mutableIntStateOf(0) }

    fun toggle(id: String) { openSection = if (openSection == id) null else id }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxWidth().background(RemGoseColors.BrandGradient).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RemGoseLogo(52)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Rem Gose · Ren'Py Player", color = Color(0xE6FFFFFF), fontSize = 13.sp)
                }
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {

            // ---------- Appearance (RenDroid) ----------
            SettingsSection("Appearance") {
                ValueRow("Theme", when (settings.theme) {
                    "dark" -> "Dark"; "light" -> "Light"; else -> "System Default"
                }) { themeDialog = true }
                SwitchRow("Dynamic Color", settings.dynamicColor,
                    "Use system wallpaper colors (Android 12+)") { scope.launch { store.setDynamicColor(it) } }
                ValueRow("Language", AppLanguages.firstOrNull { it.first == settings.language }?.second
                    ?: "System Default") { languageDialog = true }
                SwitchRow("Prefer landscape", settings.preferLandscape,
                    "Start games in landscape. Turn off to follow system rotation settings.") { v ->
                    scope.launch { store.setPreferLandscape(v); store.setOrientation(if (v) "landscape" else "auto") }
                }
                Column(Modifier.padding(vertical = 6.dp)) {
                    Text("Cỡ chữ: ${(settings.textScale * 100).toInt()}%", color = RemGoseColors.TextPrimary)
                    Slider(value = settings.textScale,
                        onValueChange = { v -> scope.launch { store.setTextScale(v) } }, valueRange = 0.85f..1.3f)
                }
                SwitchRow("Giữ màn hình sáng khi chơi", settings.keepScreenOn) { scope.launch { store.setKeepScreenOn(it) } }
                SwitchRow("Rung khi tương tác", settings.vibration) { scope.launch { store.setVibration(it) } }
            }

            // ---------- Management (RenDroid) ----------
            SettingsSection("Management") {
                ExpandRow("🔧  Runtime Management", "Installed runtimes, cleanup & download", openSection == "runtime") { toggle("runtime") }
                if (openSection == "runtime") {
                    SwitchRow("Tự động chọn runtime theo phiên bản game", settings.autoAdaptRuntime) {
                        scope.launch { store.setAutoAdapt(it) }
                    }
                    RuntimeList(runtimes)
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = {
                        scope.launch {
                            val used = games.mapNotNull { it.runtimeVersionOverride ?: it.renPyVersion }.toSet()
                            var cleaned = 0
                            withContext(Dispatchers.IO) {
                                RuntimeCatalog.specs.forEach { spec ->
                                    val dir = File(context.filesDir, "runtimes/${spec.version}")
                                    if (dir.exists() && spec.version.toString() !in used) {
                                        if (dir.deleteRecursively()) cleaned++
                                    }
                                }
                            }
                            Toast.makeText(context, "Đã dọn $cleaned runtime không dùng", Toast.LENGTH_SHORT).show()
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Clean Unused Runtimes") }
                }
                ExpandRow("🗄  Storage Management", "Storage usage & cache cleanup", openSection == "storage") { toggle("storage") }
                if (openSection == "storage") {
                    StorageStats()
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        context.cacheDir.deleteRecursively(); context.cacheDir.mkdirs()
                        Toast.makeText(context, "Đã xoá cache", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.fillMaxWidth()) { Text("Xoá cache") }
                    SwitchRow("Xoá file ZIP sau khi nhập xong", settings.deleteZipAfterImport) {
                        scope.launch { store.setDeleteZip(it) }
                    }
                    SwitchRow("Hỏi trước khi xoá game", settings.confirmBeforeDelete) {
                        scope.launch { store.setConfirmDelete(it) }
                    }
                }
            }

            // ---------- About (RenDroid) ----------
            SettingsSection("About") {
                ExpandRow("ℹ️  About", "Version info & licenses", openSection == "about") { toggle("about") }
                if (openSection == "about") {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            if (!settings.developerMode) {
                                aboutTaps++
                                when {
                                    aboutTaps >= 7 -> {
                                        aboutTaps = 0
                                        scope.launch { store.setDeveloperMode(true) }
                                        Toast.makeText(context, "Developer Mode đã bật", Toast.LENGTH_SHORT).show()
                                    }
                                    aboutTaps >= 4 -> Toast.makeText(context,
                                        "Còn ${7 - aboutTaps} lần chạm nữa để bật Developer Mode", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }.padding(vertical = 6.dp)) {
                        RemGoseLogo(56)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Rem Gose", fontWeight = FontWeight.Bold, color = RemGoseColors.TextPrimary, fontSize = 17.sp)
                            Text("Version 0.1.0 · Cho Anh Quảng", color = RemGoseColors.TextMuted, fontSize = 13.sp)
                            Text("Chạy bằng Ren'Py engine (renpy.org)", color = RemGoseColors.TextMuted, fontSize = 13.sp)
                        }
                    }
                    OutlinedButton(onClick = { showLicenses = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Version info & licenses")
                    }
                }
                ExpandRow("〈〉  Advanced Settings", "Debug mode & log export", openSection == "advanced") { toggle("advanced") }
                if (openSection == "advanced") {
                    if (settings.developerMode) {
                        Text("Developer Mode: ĐANG BẬT", color = RemGoseColors.Success, fontSize = 13.sp)
                    }
                    ChoiceRow("Debug mode", settings.debugMode,
                        listOf("off" to "Off", "troubleshooting" to "Troubleshooting", "creative" to "Creative Preview")) {
                        scope.launch { store.setDebugMode(it) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showLog = true }, modifier = Modifier.weight(1f)) { Text("Log Viewer") }
                        OutlinedButton(onClick = {
                            scope.launch {
                                val file = withContext(Dispatchers.IO) { exportLog(context) }
                                val uri = FileProvider.getUriForFile(context, "com.remgose.rem.fileprovider", file)
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"; putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }, "Export Log File"))
                            }
                        }, modifier = Modifier.weight(1f)) { Text("Export Log File") }
                    }
                    if (settings.developerMode) {
                        TextButton(onClick = { scope.launch { store.setDeveloperMode(false) } }) {
                            Text("Tắt Developer Mode", color = RemGoseColors.Error)
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            OutlinedButton(onClick = { showReset = true }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RemGoseColors.Error)) {
                Text("Đặt lại toàn bộ cài đặt")
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RemGoseColors.AccentViolet)) { Text("Quay lại thư viện") }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (themeDialog) {
        RadioDialog("Select Theme", settings.theme,
            listOf("system" to "System Default", "dark" to "Dark", "light" to "Light"),
            onPick = { themeDialog = false; scope.launch { store.setTheme(it) } },
            onClose = { themeDialog = false })
    }
    if (languageDialog) {
        RadioDialog("Select Language", settings.language, AppLanguages,
            onPick = { languageDialog = false; scope.launch { store.setLanguage(it) } },
            onClose = { languageDialog = false })
    }
    if (showLicenses) {
        AlertDialog(onDismissRequest = { showLicenses = false }, title = { Text("Licenses") },
            text = { Text("Rem Gose 0.1.0\n\n• Ren'Py engine — MIT License (renpy.org)\n• Kotlin & Jetpack Compose — Apache 2.0\n• Room / DataStore / WorkManager — Apache 2.0\n• SDL2 — zlib license\n\nỨng dụng cho Anh Quảng, ký bằng chữ ký chiuaquang.") },
            confirmButton = { TextButton(onClick = { showLicenses = false }) { Text("Close") } })
    }
    if (showLog) {
        var logText by remember { mutableStateOf("Đang đọc log…") }
        LaunchedEffect(Unit) {
            logText = withContext(Dispatchers.IO) {
                runCatching {
                    Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "300")).inputStream
                        .bufferedReader().readText()
                }.getOrDefault("Không đọc được log trên máy này.")
            }
        }
        AlertDialog(onDismissRequest = { showLog = false }, title = { Text("Log Viewer") },
            text = {
                Text(logText.takeLast(4000), fontSize = 11.sp,
                    modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()))
            },
            confirmButton = { TextButton(onClick = { showLog = false }) { Text("Close") } })
    }
    if (showReset) {
        AlertDialog(onDismissRequest = { showReset = false }, title = { Text("Đặt lại cài đặt?") },
            text = { Text("Mọi tuỳ chọn sẽ về mặc định. Game và save không bị xoá.") },
            confirmButton = { TextButton(onClick = { showReset = false; scope.launch { store.resetAll() } }) { Text("Đặt lại") } },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("Huỷ") } })
    }
}

/** Xuat log ra file trong cache de chia se (Advanced Settings > Export Log File). */
private fun exportLog(context: android.content.Context): File {
    val out = File(context.cacheDir, "remgose-log.txt")
    val text = runCatching {
        Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "1000")).inputStream.bufferedReader().readText()
    }.getOrDefault("Rem Gose log (empty)")
    out.writeText(text)
    return out
}

@Composable
private fun RadioDialog(title: String, value: String, options: List<Pair<String, String>>,
                        onPick: (String) -> Unit, onClose: () -> Unit) {
    AlertDialog(onDismissRequest = onClose, title = { Text(title) },
        text = {
            Column {
                options.forEach { (k, label) ->
                    Row(Modifier.fillMaxWidth().clickable { onPick(k) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = value == k, onClick = { onPick(k) })
                        Spacer(Modifier.width(8.dp))
                        Text(label, color = RemGoseColors.TextPrimary)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Close") } })
}

@Composable
private fun ExpandRow(title: String, subtitle: String, expanded: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = RemGoseColors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(subtitle, color = RemGoseColors.TextMuted, fontSize = 13.sp)
        }
        Text(if (expanded) "▾" else "▸", color = RemGoseColors.Accent, fontSize = 18.sp)
    }
}

@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = RemGoseColors.TextPrimary, fontSize = 16.sp)
        Text(value, color = RemGoseColors.Accent)
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RemGoseColors.Accent,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
    Card(shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RemGoseColors.Surface)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), content = content)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = RemGoseColors.TextPrimary)
            if (subtitle != null) Text(subtitle, color = RemGoseColors.TextMuted, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ChoiceRow(label: String, value: String, options: List<Pair<String, String>>, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = RemGoseColors.TextPrimary)
        Box {
            Text(options.firstOrNull { it.first == value }?.second ?: value,
                color = RemGoseColors.Accent, modifier = Modifier.clickable { expanded = true }.padding(6.dp))
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (k, labelOpt) ->
                    DropdownMenuItem(text = { Text(labelOpt) }, onClick = { expanded = false; onChange(k) })
                }
            }
        }
    }
}

@Composable
private fun RuntimeList(installed: List<RuntimeEntity>) {
    val byVersion = installed.associateBy { it.version }
    Column {
        RuntimeCatalog.specs.forEach { spec ->
            val state = byVersion[spec.version.toString()]?.statusPhase
                ?: if (spec.inBundle) "READY" else "NOT_INSTALLED"
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ren'Py ${spec.version}", color = RemGoseColors.TextPrimary, fontSize = 14.sp)
                Text(when (state) {
                    "READY" -> "Đã sẵn sàng"; "INSTALLING" -> "Đang cài…"; "ERROR" -> "Lỗi"
                    else -> if (spec.inBundle) "Có sẵn" else "Chưa cài"
                }, fontSize = 13.sp, color = when (state) {
                    "READY" -> RemGoseColors.Success; "ERROR" -> RemGoseColors.Error; else -> RemGoseColors.TextMuted
                })
            }
        }
    }
}

@Composable
private fun StorageStats() {
    val context = LocalContext.current
    val stats = remember {
        fun sizeOf(dir: File): Long = if (!dir.exists()) 0 else dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        listOf(
            "Game" to sizeOf(File(context.filesDir, "games")),
            "Runtime" to sizeOf(File(context.filesDir, "runtimes")),
            "Save" to sizeOf(File(context.filesDir, "saves")),
            "Cache" to sizeOf(context.cacheDir),
        )
    }
    Column {
        stats.forEach { (label, bytes) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = RemGoseColors.TextPrimary, fontSize = 14.sp)
                Text(formatSize(bytes), color = RemGoseColors.TextMuted, fontSize = 14.sp)
            }
        }
        Text("Thư mục game: ${File(context.filesDir, "games").absolutePath}",
            color = RemGoseColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1e9)
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1e6)
    bytes >= 1_000 -> "%.1f KB".format(bytes / 1e3)
    else -> "$bytes B"
}
