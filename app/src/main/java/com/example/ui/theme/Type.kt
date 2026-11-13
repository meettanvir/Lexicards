package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography uses FontFamily.Default (system font fallback) so Bengali, French, English,
 * Arabic, Hebrew, Urdu, and other Unicode scripts always have full glyph coverage.
 * Every text style enforces lineHeight >= 1.5x fontSize to prevent clipping Bengali vowel signs.
 */
@Suppress("DEPRECATION")
private val BengaliSafePlatformStyle = PlatformTextStyle(includeFontPadding = true)

val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 52.sp,
        platformStyle = BengaliSafePlatformStyle,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 42.sp,
        platformStyle = BengaliSafePlatformStyle,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 36.sp,
        platformStyle = BengaliSafePlatformStyle,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 32.sp,
        platformStyle = BengaliSafePlatformStyle,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        platformStyle = BengaliSafePlatformStyle,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        platformStyle = BengaliSafePlatformStyle,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        platformStyle = BengaliSafePlatformStyle,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        platformStyle = BengaliSafePlatformStyle,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 19.sp,
        platformStyle = BengaliSafePlatformStyle,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 17.sp,
        platformStyle = BengaliSafePlatformStyle,
    ),
)
