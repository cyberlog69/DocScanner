package com.example.docscanner.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.docscanner.data.pref.ThemeMode

// DocScanner Color Palette — deep indigo + teal
val Indigo900 = Color(0xFF1A237E)
val Indigo700 = Color(0xFF303F9F)
val Indigo500 = Color(0xFF3F51B5)
val Indigo200 = Color(0xFF9FA8DA)

val Teal400 = Color(0xFF26C6DA)
val Teal300 = Color(0xFF4DD0E1)

val SurfaceDark = Color(0xFF12131A)
val SurfaceVariantDark = Color(0xFF1E1F2E)
val OnSurfaceDark = Color(0xFFE6E8FF)

val LightBackground = Color(0xFFF5F6FF)
val LightSurface = Color(0xFFFFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = Indigo200,
    onPrimary = Indigo900,
    primaryContainer = Indigo700,
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Teal400,
    onSecondary = Color(0xFF00363D),
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceVariantDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF2A2B3D),
    outline = Color(0xFF4A4C6A),
    error = Color(0xFFFF8A80)
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo700,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Indigo900,
    secondary = Color(0xFF00838F),
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF1A1B2E),
    surface = LightSurface,
    onSurface = Color(0xFF1A1B2E),
    surfaceVariant = Color(0xFFE3E4F4),
    outline = Color(0xFF9FA8DA)
)

@Composable
fun DocScannerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            activity?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DocScannerTypography,
        content = content
    )
}

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
