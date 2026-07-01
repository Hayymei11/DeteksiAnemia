package com.example.anemiacheck.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = MedicalBluePrimary,
    secondary = MedicalBlueLight,
    tertiary = StatusNormalGreen,
    background = TextDark,
    surface = TextDark,
    onPrimary = CardSurfaceWhite,
    onSecondary = TextDark,
    onBackground = MedicalBlueLight,
    onSurface = MedicalBlueLight
)

private val LightColorScheme = lightColorScheme(
    primary = MedicalBluePrimary,
    secondary = MedicalBluePrimary,
    tertiary = StatusNormalGreen,
    background = MedicalBlueLight, // Latar belakang biru medis super lembut
    surface = CardSurfaceWhite,    // Card berwarna putih elegan
    onPrimary = CardSurfaceWhite,
    onSecondary = CardSurfaceWhite,
    onBackground = TextDark,
    onSurface = TextDark
)

@Composable
fun AnemiaCheckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color DIMATIKAN secara default agar tema medis kita konsisten
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
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
            val window = (view.context as Activity).window
            // Mengubah warna Status Bar (bagian atas HP) menjadi biru medis kita
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}