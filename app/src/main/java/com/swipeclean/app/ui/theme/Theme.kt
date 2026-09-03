package com.swipeclean.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = KeepGreen,
    onPrimary = Color(0xFF00210F),
    primaryContainer = KeepGreenDark,
    onPrimaryContainer = Color(0xFFC9F7D7),
    secondary = DarkOnSurfaceVariant,
    onSecondary = DarkBackground,
    tertiary = Color(0xFFFFB868),
    onTertiary = Color(0xFF402100),
    error = DeleteRed,
    onError = Color(0xFF2C0001),
    errorContainer = DeleteRedDark,
    onErrorContainer = Color(0xFFFFDAD5),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
)

private val LightColorScheme = lightColorScheme(
    primary = KeepGreenDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB6F2C7),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = LightOnSurfaceVariant,
    onSecondary = Color.White,
    tertiary = Color(0xFF8A5100),
    onTertiary = Color.White,
    error = DeleteRedDark,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD5),
    onErrorContainer = Color(0xFF410002),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
)

@Composable
fun SwipeCleanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // El color dinámico está disponible desde Android 12
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
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
        content = content,
    )
}
