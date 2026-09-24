package it.socialblock.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val Forest = Color(0xFF12221B)
val ForestLight = Color(0xFF244233)
val WarmGold = Color(0xFFF1C75B)
val WarmPaper = Color(0xFFF7F3EA)
val Danger = Color(0xFFB53A32)
val SoftGreen = Color(0xFF93C6A9)

private val LightColors =
    lightColorScheme(
        primary = Forest,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD5E8DC),
        onPrimaryContainer = Forest,
        secondary = Color(0xFF6A5520),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFE08B),
        onSecondaryContainer = Color(0xFF231B00),
        tertiary = Danger,
        background = WarmPaper,
        onBackground = Forest,
        surface = Color(0xFFFFFBF3),
        onSurface = Forest,
        surfaceVariant = Color(0xFFE5E5DD),
        onSurfaceVariant = Color(0xFF444842),
        error = Danger,
    )

private val DarkColors =
    darkColorScheme(
        primary = SoftGreen,
        onPrimary = Color(0xFF003921),
        primaryContainer = ForestLight,
        onPrimaryContainer = Color(0xFFD5E8DC),
        secondary = WarmGold,
        onSecondary = Color(0xFF3A2F00),
        tertiary = Color(0xFFFFB4AB),
        background = Color(0xFF0E1512),
        onBackground = Color(0xFFE4E9E4),
        surface = Color(0xFF131C18),
        onSurface = Color(0xFFE4E9E4),
        surfaceVariant = Color(0xFF3F4943),
        onSurfaceVariant = Color(0xFFBFC9C1),
        error = Color(0xFFFFB4AB),
    )

@Composable
fun SocialBlockTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SocialBlockTypography,
        content = content,
    )
}
