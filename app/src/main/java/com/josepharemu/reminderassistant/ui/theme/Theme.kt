package com.josepharemu.reminderassistant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme =
    lightColorScheme(

        primary =
            BrandBlue,

        onPrimary =
            Color.White,

        primaryContainer =
            BrandBlueLight,

        onPrimaryContainer =
            BrandBlueDark,

        secondary =
            BrandBlueDark,

        background =
            AppBackground,

        onBackground =
            TextPrimary,

        surface =
            AppSurface,

        onSurface =
            TextPrimary,

        surfaceVariant =
            Color(0xFFF0F2F7),

        onSurfaceVariant =
            TextSecondary,

        error =
            CriticalRed
    )

private val DarkColorScheme =
    darkColorScheme(

        primary =
            Color(0xFF9BB2FF),

        onPrimary =
            Color(0xFF002A9C),

        primaryContainer =
            Color(0xFF123DA8),

        onPrimaryContainer =
            Color.White,

        background =
            DarkBackground,

        onBackground =
            Color(0xFFF2F4F7),

        surface =
            DarkSurface,

        onSurface =
            Color(0xFFF2F4F7),

        surfaceVariant =
            DarkSurfaceVariant,

        onSurfaceVariant =
            Color(0xFFB8C0D0),

        error =
            Color(0xFFFFB4AB)
    )

@Composable
fun PersonalReminderAssistantTheme(
    darkTheme: Boolean =
        isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    MaterialTheme(
        colorScheme =
            if (darkTheme) {
                DarkColorScheme
            } else {
                LightColorScheme
            },

        typography =
            Typography,

        content =
            content
    )
}