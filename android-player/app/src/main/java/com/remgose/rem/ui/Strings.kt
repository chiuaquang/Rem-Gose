package com.remgose.rem.ui

import androidx.compose.runtime.compositionLocalOf
import java.util.Locale

/**
 * He thong ngon ngu cua Rem Gose: moi chu tren giao dien deu lay tu day,
 * doi theo lua chon trong Settings > Language (system/en/vi/id/th/sg/zh).
 * Ban dich goc la English (mac dinh trong data class), cac ngon ngu khac
 * ghi de toan bo. Singapore dung tieng Anh nhu chinh thuc o Singapore.
 */
data class Strings(
    // Thu vien
    val appTagline: String = "Your Visual Novel library",
    val settings: String = "Settings",
    val searchGames: String = "Search games…",
    val noResultsFor: String = "No games match \"%s\"",
    val importLabel: String = "Import",
    val importFirstGame: String = "Import your first game",
    val libraryEmptyTitle: String = "No games yet",
    val libraryEmptySub: String = "Import a Ren'Py game from a ZIP file to start playing.",
    val statusReady: String = "Ready",
    val statusImporting: String = "Importing",
    val statusError: String = "Error",
    val statusRunning: String = "Playing",
    val runtimeAuto: String = "auto runtime",
    val play: String = "Play",
    // Chi tiet game
    val runtimeVersion: String = "Runtime Version",
    val compatible: String = "Compatible: %s (detected %s)",
    val statusLabel: String = "Status: %s",
    val unknownVersionHint: String = "Ren'Py version not detected yet - open this page again or pick a runtime manually",
    val changeRuntime: String = "Change runtime manually",
    val chooseRuntime: String = "Choose runtime",
    val specBundled: String = " · built-in",
    val specDownload: String = " · download",
    val playNow: String = "▶  Play now",
    val deleteGame: String = "Delete game",
    val deleteGameTitle: String = "Delete this game?",
    val deleteGameMsg: String = "The game and all its files and saves will be deleted. This cannot be undone.",
    val cancel: String = "Cancel",
    val delete: String = "Delete",
    val gameManage: String = "Game Management",
    val dataManage: String = "🗂  Data Management",
    val dataItems: String = "%d items",
    val exportData: String = "Export Data",
    val importData: String = "Import Data",
    val exportOk: String = "Data exported",
    val importOk: String = "Data imported",
    val importFailed: String = "Import failed: %s",
    val fileManage: String = "📁  File Management",
    val storageUsed: String = "Storage used: %s",
    val modManage: String = "🧩  Mod Management",
    val noModsYet: String = "No Mods Yet",
    val modOn: String = "on",
    val modOff: String = "off",
    val importMod: String = "Import Mod (ZIP)",
    val uninstallMod: String = "Uninstall",
    val modImportOk: String = "Mod imported (disabled by default)",
    val modConflictTitle: String = "Mod conflict",
    val modConflictMsg: String = "\"%s\" conflicts with files from other enabled mods. Disable those mods first.",
    val enableAnyway: String = "Enable Anyway",
    val clearData: String = "Clear Data",
    val clearSaves: String = "Clear Saves",
    val clearSavesSub: String = "Delete all save files (.save)",
    val clearPersistent: String = "Clear Persistent Data",
    val clearPersistentSub: String = "Delete persistent game data (achievements, settings, etc.)",
    val clearCache: String = "Clear Cache",
    val clearCacheSub: String = "Delete cached files and temporary data",
    val dangerZone: String = "Danger Zone",
    val resetFresh: String = "Reset to Fresh Install",
    val resetFreshSub: String = "Delete ALL game data (saves, persistent, cache)",
    val confirmSavesTitle: String = "Clear Saves?",
    val confirmSavesMsg: String = "All save files (.save) of this game will be deleted. This cannot be undone.",
    val confirmPersistentTitle: String = "Clear Persistent Data?",
    val confirmPersistentMsg: String = "Are you sure? Game settings and achievements will be lost.",
    val confirmCacheTitle: String = "Clear Cache?",
    val confirmCacheMsg: String = "Cached files and temporary data will be deleted.",
    val confirmResetTitle: String = "Reset to Fresh Install?",
    val confirmResetMsg: String = "Delete all saves, persistent data and cache - like a fresh install.",
    val savesCleared: String = "Saves cleared (%d files)",
    val persistentCleared: String = "Persistent data cleared",
    val cacheClearedMsg: String = "Cache cleared (%s)",
    val resetDone: String = "Reset to a fresh install",
    val clearBtn: String = "Clear",
    val gameNotReady: String = "The game is still importing, so there is no data to manage yet.",
    val close: String = "Close",
    // Cai dat
    val appearance: String = "Appearance",
    val theme: String = "Theme",
    val themeSystem: String = "System Default",
    val themeDark: String = "Dark",
    val themeLight: String = "Light",
    val dynamicColor: String = "Dynamic Color",
    val dynamicColorSub: String = "Use system wallpaper colors (Android 12+)",
    val language: String = "Language",
    val preferLandscape: String = "Prefer landscape",
    val preferLandscapeSub: String = "Start games in landscape. Turn off to follow system rotation settings.",
    val textSize: String = "Text size: %d%%",
    val keepScreenOn: String = "Keep screen on while playing",
    val vibration: String = "Vibrate on interactions",
    val management: String = "Management",
    val runtimeManage: String = "🔧  Runtime Management",
    val runtimeManageSub: String = "Installed runtimes, cleanup & download",
    val autoAdapt: String = "Auto-select runtime by game version",
    val cleanUnused: String = "Clean Unused Runtimes",
    val cleanedToast: String = "Cleaned %d unused runtimes",
    val storageManage: String = "🗄  Storage Management",
    val storageManageSub: String = "Storage usage & cache cleanup",
    val clearCacheBtn: String = "Clear cache",
    val cacheClearedToast: String = "Cache cleared",
    val deleteZipAfter: String = "Delete ZIP file after import",
    val confirmDelete: String = "Ask before deleting a game",
    val about: String = "About",
    val aboutRow: String = "ℹ️  About",
    val aboutSub: String = "Version info & licenses",
    val versionLine: String = "Version %s · Made for Anh Quảng",
    val poweredBy: String = "Powered by the Ren'Py engine (renpy.org)",
    val licensesBtn: String = "Version info & licenses",
    val licensesTitle: String = "Licenses",
    val licensesBody: String = "Rem Gose %s\n\n• Ren'Py engine - MIT License (renpy.org)\n• Kotlin & Jetpack Compose - Apache 2.0\n• Room / DataStore / WorkManager - Apache 2.0\n• SDL2 - zlib license\n\nMade for Anh Quảng, signed with the chiuaquang key.",
    val advanced: String = "〈〉  Advanced Settings",
    val advancedSub: String = "Debug mode & log export",
    val devModeOn: String = "Developer Mode: ON",
    val debugMode: String = "Debug mode",
    val debugOff: String = "Off",
    val debugTroubleshooting: String = "Troubleshooting",
    val debugCreative: String = "Creative Preview",
    val logViewer: String = "Log Viewer",
    val exportLog: String = "Export Log File",
    val disableDevMode: String = "Turn off Developer Mode",
    val devEnabled: String = "Developer Mode enabled",
    val devTapsLeft: String = "%d more taps to enable Developer Mode",
    val resetSettings: String = "Reset all settings",
    val resetSettingsTitle: String = "Reset settings?",
    val resetSettingsMsg: String = "All options will return to defaults. Games and saves are not deleted.",
    val resetBtn: String = "Reset",
    val backToLibrary: String = "Back to library",
    val selectTheme: String = "Select Theme",
    val selectLanguage: String = "Select Language",
    val logLoading: String = "Loading log…",
    val logUnavailable: String = "Cannot read the log on this device.",
    val rtReady: String = "Ready",
    val rtInstalling: String = "Installing…",
    val rtError: String = "Error",
    val rtNotInstalled: String = "Not installed",
    val rtBundled: String = "Built-in",
    val rtUsesBundled: String = "Uses built-in %s",
    val storageGames: String = "Games",
    val storageRuntimes: String = "Runtimes",
    val storageSaves: String = "Saves",
    val storageCache: String = "Cache",
    val gamesFolder: String = "Games folder: %s",
    val importingToast: String = "Importing game %s…",
    val importReadError: String = "Cannot read the selected file",
)

val StringsEn = Strings()
val StringsSg = Strings()

val LocalStrings = compositionLocalOf { StringsEn }

/** Anh xa ma ngon ngu trong Settings -> bo chu tuong ung. */
fun stringsFor(code: String?): Strings = when (code) {
    "vi" -> StringsVi
    "id" -> StringsId
    "th" -> StringsTh
    "zh" -> StringsZh
    "sg" -> StringsSg
    "en" -> StringsEn
    else -> when (Locale.getDefault().language) {
        "vi" -> StringsVi
        "in", "id" -> StringsId
        "th" -> StringsTh
        "zh" -> StringsZh
        else -> StringsEn
    }
}
