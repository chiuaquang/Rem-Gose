package com.remgose.rem.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "remgose_settings")

/** Toan bo tuy chon trong man hinh Cai dat, luu bang DataStore. */
data class AppSettings(
    val theme: String = "system",             // system / dark / light (RenDroid: Theme - System Default)
    val dynamicColor: Boolean = true,         // RenDroid: Dynamic Color (Android 12+)
    val language: String = "system",          // system / en / vi / id / th / sg / zh (Anh Quang yeu cau)
    val preferLandscape: Boolean = true,      // RenDroid: Prefer landscape
    val orientation: String = "landscape",    // landscape / portrait / auto
    val keepScreenOn: Boolean = true,         // giu man hinh sang khi choi
    val vibration: Boolean = true,
    val autoAdaptRuntime: Boolean = true,     // tu map runtime theo version game
    val deleteZipAfterImport: Boolean = false,
    val confirmBeforeDelete: Boolean = true,
    val developerMode: Boolean = false,       // RenDroid: cham 7 lan vao muc About
    val debugMode: String = "off",            // off / troubleshooting / creative (Advanced Settings)
    val textScale: Float = 1.0f,              // co chu trong game 0.85 - 1.3
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
        val ORIENTATION = stringPreferencesKey("orientation")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val VIBRATION = booleanPreferencesKey("vibration")
        val AUTO_ADAPT = booleanPreferencesKey("auto_adapt_runtime")
        val DELETE_ZIP = booleanPreferencesKey("delete_zip_after_import")
        val CONFIRM_DELETE = booleanPreferencesKey("confirm_before_delete")
        val TEXT_SCALE = floatPreferencesKey("text_scale")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val PREFER_LANDSCAPE = booleanPreferencesKey("prefer_landscape")
        val DEVELOPER_MODE = booleanPreferencesKey("developer_mode")
        val DEBUG_MODE = stringPreferencesKey("debug_mode")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            theme = p[Keys.THEME] ?: "dark",
            language = p[Keys.LANGUAGE] ?: "vi",
            orientation = p[Keys.ORIENTATION] ?: "landscape",
            keepScreenOn = p[Keys.KEEP_SCREEN_ON] ?: true,
            vibration = p[Keys.VIBRATION] ?: true,
            autoAdaptRuntime = p[Keys.AUTO_ADAPT] ?: true,
            deleteZipAfterImport = p[Keys.DELETE_ZIP] ?: false,
            confirmBeforeDelete = p[Keys.CONFIRM_DELETE] ?: true,
            textScale = p[Keys.TEXT_SCALE] ?: 1.0f,
            dynamicColor = p[Keys.DYNAMIC_COLOR] ?: true,
            preferLandscape = p[Keys.PREFER_LANDSCAPE] ?: true,
            developerMode = p[Keys.DEVELOPER_MODE] ?: false,
            debugMode = p[Keys.DEBUG_MODE] ?: "off",
        )
    }

    suspend fun setTheme(v: String) = context.dataStore.edit { it[Keys.THEME] = v }
    suspend fun setLanguage(v: String) = context.dataStore.edit { it[Keys.LANGUAGE] = v }
    suspend fun setOrientation(v: String) = context.dataStore.edit { it[Keys.ORIENTATION] = v }
    suspend fun setKeepScreenOn(v: Boolean) = context.dataStore.edit { it[Keys.KEEP_SCREEN_ON] = v }
    suspend fun setVibration(v: Boolean) = context.dataStore.edit { it[Keys.VIBRATION] = v }
    suspend fun setAutoAdapt(v: Boolean) = context.dataStore.edit { it[Keys.AUTO_ADAPT] = v }
    suspend fun setDeleteZip(v: Boolean) = context.dataStore.edit { it[Keys.DELETE_ZIP] = v }
    suspend fun setConfirmDelete(v: Boolean) = context.dataStore.edit { it[Keys.CONFIRM_DELETE] = v }
    suspend fun setTextScale(v: Float) = context.dataStore.edit { it[Keys.TEXT_SCALE] = v }
    suspend fun setDynamicColor(v: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = v }
    suspend fun setPreferLandscape(v: Boolean) = context.dataStore.edit { it[Keys.PREFER_LANDSCAPE] = v }
    suspend fun setDeveloperMode(v: Boolean) = context.dataStore.edit { it[Keys.DEVELOPER_MODE] = v }
    suspend fun setDebugMode(v: String) = context.dataStore.edit { it[Keys.DEBUG_MODE] = v }
    suspend fun resetAll() = context.dataStore.edit { it.clear() }
}
