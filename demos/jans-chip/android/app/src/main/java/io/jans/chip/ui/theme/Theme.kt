package io.jans.chip.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import io.jans.chip.theme.md_theme_dark_background
import io.jans.chip.theme.md_theme_dark_error
import io.jans.chip.theme.md_theme_dark_errorContainer
import io.jans.chip.theme.md_theme_dark_inverseOnSurface
import io.jans.chip.theme.md_theme_dark_inversePrimary
import io.jans.chip.theme.md_theme_dark_inverseSurface
import io.jans.chip.theme.md_theme_dark_onBackground
import io.jans.chip.theme.md_theme_dark_onError
import io.jans.chip.theme.md_theme_dark_onErrorContainer
import io.jans.chip.theme.md_theme_dark_onPrimary
import io.jans.chip.theme.md_theme_dark_onPrimaryContainer
import io.jans.chip.theme.md_theme_dark_onSecondary
import io.jans.chip.theme.md_theme_dark_onSecondaryContainer
import io.jans.chip.theme.md_theme_dark_onSurface
import io.jans.chip.theme.md_theme_dark_onSurfaceVariant
import io.jans.chip.theme.md_theme_dark_onTertiary
import io.jans.chip.theme.md_theme_dark_onTertiaryContainer
import io.jans.chip.theme.md_theme_dark_outline
import io.jans.chip.theme.md_theme_dark_primary
import io.jans.chip.theme.md_theme_dark_primaryContainer
import io.jans.chip.theme.md_theme_dark_secondary
import io.jans.chip.theme.md_theme_dark_secondaryContainer
import io.jans.chip.theme.md_theme_dark_surface
import io.jans.chip.theme.md_theme_dark_surfaceVariant
import io.jans.chip.theme.md_theme_dark_tertiary
import io.jans.chip.theme.md_theme_dark_tertiaryContainer
import io.jans.chip.theme.md_theme_light_background
import io.jans.chip.theme.md_theme_light_error
import io.jans.chip.theme.md_theme_light_errorContainer
import io.jans.chip.theme.md_theme_light_inverseOnSurface
import io.jans.chip.theme.md_theme_light_inversePrimary
import io.jans.chip.theme.md_theme_light_inverseSurface
import io.jans.chip.theme.md_theme_light_onBackground
import io.jans.chip.theme.md_theme_light_onError
import io.jans.chip.theme.md_theme_light_onErrorContainer
import io.jans.chip.theme.md_theme_light_onPrimary
import io.jans.chip.theme.md_theme_light_onPrimaryContainer
import io.jans.chip.theme.md_theme_light_onSecondary
import io.jans.chip.theme.md_theme_light_onSecondaryContainer
import io.jans.chip.theme.md_theme_light_onSurface
import io.jans.chip.theme.md_theme_light_onSurfaceVariant
import io.jans.chip.theme.md_theme_light_onTertiary
import io.jans.chip.theme.md_theme_light_onTertiaryContainer
import io.jans.chip.theme.md_theme_light_outline
import io.jans.chip.theme.md_theme_light_primary
import io.jans.chip.theme.md_theme_light_primaryContainer
import io.jans.chip.theme.md_theme_light_secondary
import io.jans.chip.theme.md_theme_light_secondaryContainer
import io.jans.chip.theme.md_theme_light_surface
import io.jans.chip.theme.md_theme_light_surfaceVariant
import io.jans.chip.theme.md_theme_light_tertiary
import io.jans.chip.theme.md_theme_light_tertiaryContainer

private val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary
)

private val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary
)

private val LocalAppDimens = staticCompositionLocalOf {
    normalDimensions
}

@Composable
fun ProvideDimens(
    dimensions: Dimensions,
    content: @Composable () -> Unit
) {
    val dimensionSet = remember { dimensions }
    CompositionLocalProvider(LocalAppDimens provides dimensionSet, content = content)
}
@Composable
fun Janschip1Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as Activity).window.statusBarColor = colorScheme.primary.toArgb()
            ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars = darkTheme
        }
    }

    //Dimensions (calculate dimens here based on screen size)
    val dimensions = normalDimensions

    ProvideDimens(dimensions = dimensions) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

object AppTheme {
    val dimens: Dimensions
        @Composable
        get() = LocalAppDimens.current
}
