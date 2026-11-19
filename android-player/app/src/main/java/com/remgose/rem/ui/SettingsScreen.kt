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
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
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
                CompositionLocalProvider(LocalStrings provides stringsFor(s.language)) {
                    SettingsScreen(onBack = { finish() })
                }
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
    val s = LocalStrings.current
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
                    Text(s.settings, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Rem Gose · Ren'Py Player", color = Color(0xE6FFFFFF), fontSize = 13.sp)
                }
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {

            // ---------- Appearance (RenDroid) ----------
            SettingsSection(s.appearance) {
                ValueRow(s.theme, when (settings.theme) {
                    "dark" -> s.themeDark; "light" -> s.themeLight; else -> s.themeSystem
                }) { themeDialog = true }
                SwitchRow(s.dynamicColor, settings.dynamicColor,
                    s.dynamicColorSub) { scope.launch { store.setDynamicColor(it) } }
                ValueRow(s.language, AppLanguages.firstOrNull { it.first == settings.language }?.second
                    ?: "System Default") { languageDialog = true }
                SwitchRow(s.preferLandscape, settings.preferLandscape,
                    s.preferLandscapeSub) { v ->
                    scope.launch { store.setPreferLandscape(v); store.setOrientation(if (v) "landscape" else "auto") }
                }
                Column(Modifier.padding(vertical = 6.dp)) {
                    Text(s.textSize.format((settings.textScale * 100).toInt()), color = RemGoseColors.TextPrimary)
                    Slider(value = settings.textScale,
                        onValueChange = { v -> scope.launch { store.setTextScale(v) } }, valueRange = 0.85f..1.3f)
                }
                SwitchRow(s.keepScreenOn, settings.keepScreenOn) { scope.launch { store.setKeepScreenOn(it) } }
                SwitchRow(s.vibration, settings.vibration) { scope.launch { store.setVibration(it) } }
            }

            // ---------- Management (RenDroid) ----------
            SettingsSection(s.management) {
                ExpandRow(s.runtimeManage, s.runtimeManageSub, openSection == "runtime") { toggle("runtime") }
                if (openSection == "runtime") {
                    SwitchRow(s.autoAdapt, settings.autoAdaptRuntime) {
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
                            Toast.makeText(context, s.cleanedToast.format(cleaned), Toast.LENGTH_SHORT).show()
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text(s.cleanUnused) }
                }
                ExpandRow(s.storageManage, s.storageManageSub, openSection == "storage") { toggle("storage") }
                if (openSection == "storage") {
                    StorageStats()
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        context.cacheDir.deleteRecursively(); context.cacheDir.mkdirs()
                        Toast.makeText(context, s.cacheClearedToast, Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.fillMaxWidth()) { Text(s.clearCacheBtn) }
                    SwitchRow(s.deleteZipAfter, settings.deleteZipAfterImport) {
                        scope.launch { store.setDeleteZip(it) }
                    }
                    SwitchRow(s.confirmDelete, settings.confirmBeforeDelete) {
                        scope.launch { store.setConfirmDelete(it) }
                    }
                }
            }

            // ---------- About (RenDroid) ----------
            SettingsSection(s.about) {
                ExpandRow(s.aboutRow, s.aboutSub, openSection == "about") { toggle("about") }
                if (openSection == "about") {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            if (!settings.developerMode) {
                                aboutTaps++
                                when {
                                    aboutTaps >= 7 -> {
                                        aboutTaps = 0
                                        scope.launch { store.setDeveloperMode(true) }
                                        Toast.makeText(context, s.devEnabled, Toast.LENGTH_SHORT).show()
                                    }
                                    aboutTaps >= 4 -> Toast.makeText(context,
                                        s.devTapsLeft.format(7 - aboutTaps), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }.padding(vertical = 6.dp)) {
                        RemGoseLogo(56)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Rem Gose", fontWeight = FontWeight.Bold, color = RemGoseColors.TextPrimary, fontSize = 17.sp)
                            Text(s.versionLine.format(appVersionName(context)), color = RemGoseColors.TextMuted, fontSize = 13.sp)
                            Text(s.poweredBy, color = RemGoseColors.TextMuted, fontSize = 13.sp)
                        }
                    }
                    OutlinedButton(onClick = { showLicenses = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(s.licensesBtn)
                    }
                }
                ExpandRow(s.advanced, s.advancedSub, openSection == "advanced") { toggle("advanced") }
                if (openSection == "advanced") {
                    if (settings.developerMode) {
                        Text(s.devModeOn, color = RemGoseColors.Success, fontSize = 13.sp)
                    }
                    ChoiceRow(s.debugMode, settings.debugMode,
                        listOf("off" to s.debugOff, "troubleshooting" to s.debugTroubleshooting, "creative" to s.debugCreative)) {
                        scope.launch { store.setDebugMode(it) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showLog = true }, modifier = Modifier.weight(1f)) { Text(s.logViewer) }
                        OutlinedButton(onClick = {
                            scope.launch {
                                val file = withContext(Dispatchers.IO) { exportLog(context) }
                                val uri = FileProvider.getUriForFile(context, "com.remgose.rem.fileprovider", file)
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"; putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }, s.exportLog))
                            }
                        }, modifier = Modifier.weight(1f)) { Text(s.exportLog) }
                    }
                    if (settings.developerMode) {
                        TextButton(onClick = { scope.launch { store.setDeveloperMode(false) } }) {
                            Text(s.disableDevMode, color = RemGoseColors.Error)
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            OutlinedButton(onClick = { showReset = true }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RemGoseColors.Error)) {
                Text(s.resetSettings)
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RemGoseColors.AccentViolet)) { Text(s.backToLibrary) }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (themeDialog) {
        RadioDialog(s.selectTheme, settings.theme,
            listOf("system" to s.themeSystem, "dark" to s.themeDark, "light" to s.themeLight),
            onPick = { themeDialog = false; scope.launch { store.setTheme(it) } },
            onClose = { themeDialog = false })
    }
    if (languageDialog) {
        RadioDialog(s.selectLanguage, settings.language, AppLanguages,
            onPick = { languageDialog = false; scope.launch { store.setLanguage(it) } },
            onClose = { languageDialog = false })
    }
    if (showLicenses) {
        AlertDialog(onDismissRequest = { showLicenses = false }, title = { Text(s.licensesTitle) },
            text = { Text(s.licensesBody.format(appVersionName(context))) },
            confirmButton = { TextButton(onClick = { showLicenses = false }) { Text(s.close) } })
    }
    if (showLog) {
        var logText by remember { mutableStateOf(s.logLoading) }
        LaunchedEffect(Unit) {
            logText = withContext(Dispatchers.IO) {
                runCatching {
                    Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "300")).inputStream
                        .bufferedReader().readText()
                }.getOrDefault(s.logUnavailable)
            }
        }
        AlertDialog(onDismissRequest = { showLog = false }, title = { Text(s.logViewer) },
            text = {
                Text(logText.takeLast(4000), fontSize = 11.sp,
                    modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()))
            },
            confirmButton = { TextButton(onClick = { showLog = false }) { Text(s.close) } })
    }
    if (showReset) {
        AlertDialog(onDismissRequest = { showReset = false }, title = { Text(s.resetSettingsTitle) },
            text = { Text(s.resetSettingsMsg) },
            confirmButton = { TextButton(onClick = { showReset = false; scope.launch { store.resetAll() } }) { Text(s.resetBtn) } },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text(s.cancel) } })
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
        confirmButton = { TextButton(onClick = onClose) { Text(LocalStrings.current.close) } })
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

/** Phien ban app that, doc tu package da cai (khong ghi cung so lieu). */
fun appVersionName(context: android.content.Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
        .getOrNull() ?: "?"

@Composable
private fun RuntimeList(installed: List<RuntimeEntity>) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val byVersion = installed.associateBy { it.version }
    Column {
        RuntimeCatalog.specs.forEach { spec ->
            val row = byVersion[spec.version.toString()]
            // Runtime tai rieng da du file tren may thi la READY du DB chua kip ghi
            val downloaded = !spec.inBundle && row?.statusPhase != "INSTALLING" &&
                com.remgose.rem.core.RuntimeStore.isReady(context.filesDir, spec.version.toString())
            val state = when {
                downloaded -> "READY"
                else -> row?.statusPhase ?: if (spec.inBundle) "READY" else "NOT_INSTALLED"
            }
            // Trung thuc: ban 8.x khong bundle van choi duoc bang engine 8.5.2 tich hop
            val usesBundled = !spec.inBundle && spec.version.major ==
                RuntimeCatalog.bundledSpec().version.major
            Column(Modifier.fillMaxWidth()
                .clickable(enabled = state != "READY" && state != "INSTALLING") {
                    WorkManager.getInstance(context).enqueue(
                        OneTimeWorkRequestBuilder<com.remgose.rem.worker.RuntimeInstallWorker>()
                            .setInputData(workDataOf("version" to spec.version.toString())).build())
                }
                .padding(vertical = 4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Ren'Py ${spec.version}", color = RemGoseColors.TextPrimary, fontSize = 14.sp)
                    Text(when {
                        state == "READY" && spec.inBundle -> s.rtBundled
                        state == "READY" -> s.rtReady
                        state == "INSTALLING" -> s.rtInstalling
                        state == "ERROR" -> s.rtError
                        usesBundled -> s.rtUsesBundled.format(RuntimeCatalog.BUNDLED_VERSION)
                        else -> s.rtNotInstalled
                    }, fontSize = 13.sp, color = when {
                        state == "READY" || usesBundled -> RemGoseColors.Success
                        state == "ERROR" -> RemGoseColors.Error
                        else -> RemGoseColors.TextMuted
                    })
                }
                row?.statusMessage?.let {
                    Text(it, color = RemGoseColors.TextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun StorageStats() {
    val context = LocalContext.current
    val s = LocalStrings.current
    val stats = remember {
        fun sizeOf(dir: File): Long = if (!dir.exists()) 0 else dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        listOf(
            s.storageGames to sizeOf(File(context.filesDir, "games")),
            s.storageRuntimes to sizeOf(File(context.filesDir, "runtimes")),
            s.storageSaves to sizeOf(File(context.filesDir, "saves")),
            s.storageCache to sizeOf(context.cacheDir),
        )
    }
    Column {
        stats.forEach { (label, bytes) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = RemGoseColors.TextPrimary, fontSize = 14.sp)
                Text(formatSize(bytes), color = RemGoseColors.TextMuted, fontSize = 14.sp)
            }
        }
        Text(s.gamesFolder.format(File(context.filesDir, "games").absolutePath),
            color = RemGoseColors.TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1e9)
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1e6)
    bytes >= 1_000 -> "%.1f KB".format(bytes / 1e3)
    else -> "$bytes B"
}
