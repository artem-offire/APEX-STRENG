package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = IceCyanPrimary,
    onPrimary = IceCyanOnPrimary,
    primaryContainer = IceCyanContainer,
    onPrimaryContainer = IceCyanOnContainer,
    secondary = SteelBlueSecondary,
    onSecondary = Color(0xFF0B0E11),
    secondaryContainer = SteelBlueContainer,
    onSecondaryContainer = SteelBlueOnContainer,
    tertiary = WarningAmber,
    background = ObsidianDark,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = ObsidianSurfaceContainer,
    outline = ObsidianBorder,
    outlineVariant = Color(0xFF1B242E),
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = IceCyanLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE5EC),
    onPrimaryContainer = Color(0xFF1A2A36),
    secondary = Color(0xFF56697A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8EE),
    onSecondaryContainer = Color(0xFF172430),
    tertiary = WarningAmber,
    background = PaperLight,
    onBackground = TextPrimaryLight,
    surface = PaperSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = Color(0xFFEFF2F6),
    outline = PaperBorder,
    outlineVariant = Color(0xFFDDE2E8),
    error = Color(0xFFC04B4B),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
