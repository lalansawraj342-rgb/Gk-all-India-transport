package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = EmeraldDarkPrimary,
    onPrimary = EmeraldDarkOnPrimary,
    primaryContainer = EmeraldPrimary,
    onPrimaryContainer = PureWhite,
    secondary = Color(0xFF60A5FA),
    onSecondary = NavyDeep,
    secondaryContainer = NavySecondary,
    onSecondaryContainer = PureWhite,
    tertiary = AmberAccent,
    onTertiary = PureWhite,
    background = NavyDarkSurface,
    onBackground = PureWhite,
    surface = NavyDarkCard,
    onSurface = PureWhite,
    surfaceVariant = Color(0xFF1D3557),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = PureWhite,
    primaryContainer = EmeraldLightContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = NavyDeep,
    onSecondary = PureWhite,
    secondaryContainer = NavyContainer,
    onSecondaryContainer = NavyOnContainer,
    tertiary = AmberAccent,
    onTertiary = PureWhite,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberOnContainer,
    background = OffWhiteBg,
    onBackground = InkPrimary,
    surface = PureWhite,
    onSurface = InkPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = InkSecondary,
    outline = BorderSlate,
    error = ErrorRed,
    onError = PureWhite,
    errorContainer = ErrorContainerLight,
    onErrorContainer = ErrorOnContainerLight,
  )

@Composable
fun GkTransportTheme(
  isHindi: Boolean = false,
  darkTheme: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = appTypography(isHindi = isHindi),
    content = content,
  )
}
