package com.remgose.rem.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Rem Gose Design System - "Midnight Visual Novel"
 * Nen xanh den sau, diem nhan gradient indigo -> violet -> hong,
 * the game bo goc 20dp, anh bia la nhan vat chinh cua giao dien.
 * Ho tro Theme System/Dark/Light + Dynamic Color (Android 12+) nhu RenDroid.
 */
object RemGoseColors {
    val Background = Color(0xFF0B0B14)
    val Surface = Color(0xFF151524)
    val SurfaceVariant = Color(0xFF1E1E33)
    val Accent = Color(0xFF7AA2FF)
    val AccentViolet = Color(0xFF9D7AFF)
    val AccentPink = Color(0xFFFF7AC6)
    val TextPrimary = Color(0xFFF4F4FA)
    val TextMuted = Color(0xFF9A9AB0)
    val Success = Color(0xFF4ADE80)
    val Warning = Color(0xFFFBBF24)
    val Error = Color(0xFFF87171)
    val BrandGradient = Brush.linearGradient(listOf(Accent, AccentViolet, AccentPink))
    val CardSheen = Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x00000000)))
}

private val DarkScheme = darkColorScheme(
    primary = RemGoseColors.Accent,
    secondary = RemGoseColors.AccentViolet,
    tertiary = RemGoseColors.AccentPink,
    background = RemGoseColors.Background,
    surface = RemGoseColors.Surface,
    surfaceVariant = RemGoseColors.SurfaceVariant,
    onBackground = RemGoseColors.TextPrimary,
    onSurface = RemGoseColors.TextPrimary,
    error = RemGoseColors.Error,
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF3D5AFE),
    secondary = RemGoseColors.AccentViolet,
    tertiary = RemGoseColors.AccentPink,
    background = Color(0xFFF6F6FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8E8F4),
    onBackground = Color(0xFF14141F),
    onSurface = Color(0xFF14141F),
    error = Color(0xFFD32F2F),
)

/** theme: system | dark | light. dynamicColor: mau tu hinh nen (Android 12+). */
@Composable
fun RemGoseTheme(theme: String = "dark", dynamicColor: Boolean = false,
                 content: @Composable () -> Unit) {
    val dark = when (theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= 31 ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
