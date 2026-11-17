package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = OnEmeraldPrimaryContainer,
    secondary = SkySecondary,
    onSecondary = Color.White,
    secondaryContainer = SkySecondaryContainer,
    onSecondaryContainer = OnSkySecondaryContainer,
    tertiary = MintTertiary,
    onTertiary = Color.White,
    tertiaryContainer = MintTertiaryContainer,
    onTertiaryContainer = OnMintTertiaryContainer,
    background = MistBackground,
    onBackground = TextPrimary,
    surface = CrispSurface,
    onSurface = TextPrimary,
    surfaceVariant = SoftMintSurface,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = SoftBlueSurface,
    surfaceContainer = SoftMintSurface,
    outline = OutlineSubtle,
    outlineVariant = OutlineSubtle
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF003732),
    primaryContainer = Color(0xFF0F4C46),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = DarkSecondary,
    onSecondary = Color(0xFF003549),
    secondaryContainer = Color(0xFF0B4A6E),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFF6EE7B7),
    onTertiary = Color(0xFF003824),
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFD1FAE5),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = DarkBackground,
    onBackground = Color(0xFFE6F4F1),
    surface = DarkSurface,
    onSurface = Color(0xFFE6F4F1),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA9C5C1),
    surfaceContainerLow = Color(0xFF0F201E),
    surfaceContainer = DarkSurfaceVariant,
    outline = Color(0xFF2B4D49)
)

val AirCareShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AirCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AirCareShapes,
        content = content
    )
}
