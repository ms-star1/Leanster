package com.beispiel.ridetracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = MutedCyan,
    tertiary = MutedGrey,
    background = DeepCarbon,
    surface = SurfaceCard,
    onPrimary = DeepCarbon,
    onSecondary = DeepCarbon,
    onTertiary = DeepCarbon,
    onBackground = PureWhite,
    onSurface = PureWhite,
    error = AlertRed,
    outline = BorderDivider
)

private val LightColorScheme = DarkColorScheme // Always use Dark Theme for high-contrast "Carbon & Neon"

@Composable
fun LeansterTheme(
    darkTheme: Boolean = true, // Force dark theme for the aesthetic
    dynamicColor: Boolean = false, // Disable dynamic color to maintain brand identity
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}