package com.jiucaihua.app.presentation.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBFA),
    onPrimaryContainer = PrimaryBlueDark,
    secondary = Color(0xFF536B80),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE5EC),
    onSecondaryContainer = Color(0xFF273541),
    tertiary = AccentGold,
    onTertiary = Color(0xFF3E2E0C),
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFE8EAEC),
    onSurfaceVariant = Color(0xFF5A626A),
    outline = Color(0xFFC6CBD0),
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueLight,
    onPrimary = PrimaryBlueDark,
    primaryContainer = Color(0xFF193B5A),
    onPrimaryContainer = Color(0xFFD7E8F7),
    secondary = Color(0xFFB2C5D5),
    onSecondary = Color(0xFF22303D),
    secondaryContainer = Color(0xFF2A3945),
    onSecondaryContainer = Color(0xFFD9E6EF),
    tertiary = AccentGold,
    onTertiary = Color(0xFF3B2D0D),
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF202428),
    onSurfaceVariant = Color(0xFFBAC1C7),
    outline = Color(0xFF3C444A),
)

@Composable
fun JiucaihuaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    oledMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val colorScheme = if (darkTheme && oledMode) {
        baseColorScheme.copy(
            background = OledBlack,
            surface = OledBlack,
            surfaceVariant = Color(0xFF171A1D),
            surfaceContainer = Color(0xFF0A0C0E),
            surfaceContainerHigh = Color(0xFF121518),
            surfaceContainerHighest = Color(0xFF1C2024),
        )
    } else {
        baseColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
