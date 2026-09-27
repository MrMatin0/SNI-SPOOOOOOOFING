package com.example.snispoofing.ui.theme

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

val PrimarySky = Color(0xFF0284C7)
val PrimaryLightSky = Color(0xFF38BDF8)
val SecondaryIndigo = Color(0xFF6366F1)
val AccentEmerald = Color(0xFF10B981)
val WarningAmber = Color(0xFFF59E0B)
val ErrorRose = Color(0xFFEF4444)

val SlateDarkBackground = Color(0xFF0F172A)
val SlateDarkSurface = Color(0xFF1E293B)
val SlateDarkCard = Color(0xFF334155)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLightSky,
    secondary = SecondaryIndigo,
    tertiary = AccentEmerald,
    background = SlateDarkBackground,
    surface = SlateDarkSurface,
    surfaceVariant = SlateDarkCard,
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    error = ErrorRose
)

private val LightColorScheme = lightColorScheme(
    primary = PrimarySky,
    secondary = SecondaryIndigo,
    tertiary = AccentEmerald,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    onPrimary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    error = ErrorRose
)

@Composable
fun SNISpoofingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
        content = content
    )
}
