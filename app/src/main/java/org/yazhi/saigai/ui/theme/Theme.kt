package org.yazhi.saigai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * High-contrast, large-type theme (design doc §4: "large, high-contrast Tamil and
 * English output text"; §8 requires ≥28sp captions).
 *
 * Fixed light palette on purpose: a counter-side phone in a bright hospital
 * corridor is the target scene, and a single tested palette beats two unverified ones.
 */
private val SaigaiColors = lightColorScheme(
    primary = Color(0xFF0B3D91),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E2FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF00696E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB4EBEF),
    onSecondaryContainer = Color(0xFF002022),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0B0B0C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0B0B0C),
    surfaceVariant = Color(0xFFE7E8EC),
    onSurfaceVariant = Color(0xFF1B1C1F),
    outline = Color(0xFF3A3C40),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
)

private val SaigaiType = Typography(
    // Caption surface: the single most important text on the screen.
    displaySmall = TextStyle(
        fontSize = 34.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleLarge = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 30.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 17.sp,
        lineHeight = 26.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
    ),
)

@Composable
fun SaigaiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SaigaiColors,
        typography = SaigaiType,
        content = content,
    )
}
