package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NexDarkColorScheme = darkColorScheme(
    primary = NexBlue,
    onPrimary = NexTextPrimary,
    primaryContainer = NexBlueDark,
    onPrimaryContainer = NexCyanLight,
    secondary = NexCyan,
    onSecondary = NexDarkBg,
    secondaryContainer = NexCardElevated,
    onSecondaryContainer = NexCyanLight,
    tertiary = NexFolderYellow,
    background = NexDarkBg,
    onBackground = NexTextPrimary,
    surface = NexCardBg,
    onSurface = NexTextPrimary,
    surfaceVariant = NexBorder,
    onSurfaceVariant = NexTextSecondary,
    outline = NexBorderLight,
    outlineVariant = NexBorder,
    error = NexDanger,
    onError = NexTextPrimary,
    errorContainer = NexDangerBg,
    onErrorContainer = NexDanger
)

@Composable
fun NexCloudTheme(
    darkTheme: Boolean = true, // NexCloud is built with dark theme as primary brand aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = NexDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var currentContext = view.context
            while (currentContext is android.content.ContextWrapper) {
                if (currentContext is Activity) {
                    val window = currentContext.window
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                    WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
                    break
                }
                currentContext = currentContext.baseContext
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
