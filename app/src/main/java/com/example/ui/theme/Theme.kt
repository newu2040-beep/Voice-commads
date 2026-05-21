package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
    isDarkMode: Boolean = isSystemInDarkTheme(),
    themeIndex: Int = 0,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val primaryColor = when (themeIndex) {
        1 -> EmeraldPrimary
        2 -> VioletPrimary
        3 -> RosePrimary
        4 -> SunsetPrimary
        5 -> SlatePrimary
        6 -> MidnightPrimary
        7 -> NeonPrimary
        8 -> MintPrimary
        else -> OceanPrimary
    }
    
    val bgLightColor = when (themeIndex) {
        1 -> EmeraldBackgroundLight
        2 -> VioletBackgroundLight
        3 -> RoseBackgroundLight
        4 -> SunsetBackgroundLight
        5 -> SlateBackgroundLight
        6 -> MidnightBackgroundLight
        7 -> NeonBackgroundLight
        8 -> MintBackgroundLight
        else -> OceanBackgroundLight
    }

    val borderLightColor = when (themeIndex) {
        1 -> EmeraldBorderLight
        2 -> VioletBorderLight
        3 -> RoseBorderLight
        4 -> SunsetBorderLight
        5 -> SlateBorderLight
        6 -> MidnightBorderLight
        7 -> NeonBorderLight
        8 -> MintBorderLight
        else -> OceanBorderLight
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDarkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        isDarkMode -> darkColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            background = Color(0xFF121212),
            onBackground = TextDarkInverse,
            surface = Color(0xFF1E1E1E),
            onSurface = TextDarkInverse,
            surfaceVariant = Color(0xFF2C2C2C),
            onSurfaceVariant = Color(0xFFAAAAAA),
            primaryContainer = primaryColor.copy(alpha = 0.3f),
            onPrimaryContainer = primaryColor
        )
        else -> lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            background = bgLightColor,
            onBackground = TextDark,
            surface = Color.White,
            onSurface = TextDark,
            surfaceVariant = Color.White,
            onSurfaceVariant = TextSecondary,
            primaryContainer = borderLightColor,
            onPrimaryContainer = primaryColor
        )
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
