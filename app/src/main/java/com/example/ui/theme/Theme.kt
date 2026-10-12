package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = IceCyanPrimary,
    onPrimary = IceCyanOnPrimary,
    primaryContainer = IceCyanContainer,
    onPrimaryContainer = IceCyanOnContainer,
    secondary = SteelBlueSecondary,
    onSecondary = ObsidianDark,
    secondaryContainer = SteelBlueContainer,
    onSecondaryContainer = SteelBlueOnContainer,
    tertiary = WarningAmber,
    onTertiary = Color(0xFF241405),
    tertiaryContainer = WarningAmberContainer,
    onTertiaryContainer = WarningAmberLight,
    background = ObsidianDark,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = ObsidianSurfaceContainer,
    surfaceContainerHigh = Color(0xFF151C21),
    surfaceContainerHighest = ObsidianSurfaceVariant,
    outline = ObsidianBorder,
    outlineVariant = ObsidianBorderStrong,
    error = ErrorRed,
    onError = Color(0xFF2A0B0D),
    errorContainer = ErrorRedContainer,
    onErrorContainer = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = IceCyanLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5F5F0),
    onPrimaryContainer = Color(0xFF04201C),
    secondary = Color(0xFF56697A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8EE),
    onSecondaryContainer = Color(0xFF172430),
    tertiary = Color(0xFFB45F1C),
    background = PaperLight,
    onBackground = TextPrimaryLight,
    surface = PaperSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = Color(0xFFEFF3F5),
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

/** True when the active APEX color scheme is the dark one (theme can be overridden in-app). */
@Composable
@ReadOnlyComposable
fun apexIsDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
