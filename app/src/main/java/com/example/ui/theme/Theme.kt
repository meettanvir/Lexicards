package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = OnIndigoPrimaryDark,
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = OnIndigoContainerDark,
    secondary = TealSecondaryDark,
    onSecondary = OnTealSecondaryDark,
    secondaryContainer = TealContainerDark,
    onSecondaryContainer = OnTealContainerDark,
    tertiary = CoralTertiaryDark,
    onTertiary = OnCoralTertiaryDark,
    tertiaryContainer = CoralContainerDark,
    onTertiaryContainer = OnCoralContainerDark,
    background = DarkBackground,
    onBackground = OnDarkSurface,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurfaceMuted,
    outline = DarkOutline,
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = OnIndigoPrimary,
    primaryContainer = IndigoPrimaryContainer,
    onPrimaryContainer = OnIndigoPrimaryContainer,
    secondary = TealSecondary,
    onSecondary = OnTealSecondary,
    secondaryContainer = TealSecondaryContainer,
    onSecondaryContainer = OnTealSecondaryContainer,
    tertiary = CoralTertiary,
    onTertiary = OnCoralTertiary,
    tertiaryContainer = CoralTertiaryContainer,
    onTertiaryContainer = OnCoralTertiaryContainer,
    background = WarmIvoryBackground,
    onBackground = OnWarmIvory,
    surface = CrispSurface,
    onSurface = OnWarmIvory,
    surfaceVariant = SoftSandSurfaceVariant,
    onSurfaceVariant = OnSurfaceMuted,
    outline = SubtleOutline,
    error = ErrorRed,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

val LexiShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun LexiCardsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = LexiShapes,
        content = content,
    )
}
