package me.tagavari.airmessage.compose.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import me.tagavari.airmessage.helper.ThemeHelper

private val DarkColorScheme = darkColorScheme(
	primary = AMColor.md_theme_dark_primary,
	onPrimary = AMColor.md_theme_dark_onPrimary,
	primaryContainer = AMColor.md_theme_dark_primaryContainer,
	onPrimaryContainer = AMColor.md_theme_dark_onPrimaryContainer,
	secondary = AMColor.md_theme_dark_secondary,
	onSecondary = AMColor.md_theme_dark_onSecondary,
	secondaryContainer = AMColor.md_theme_dark_secondaryContainer,
	onSecondaryContainer = AMColor.md_theme_dark_onSecondaryContainer,
	tertiary = AMColor.md_theme_dark_tertiary,
	onTertiary = AMColor.md_theme_dark_onTertiary,
	tertiaryContainer = AMColor.md_theme_dark_tertiaryContainer,
	onTertiaryContainer = AMColor.md_theme_dark_onTertiaryContainer,
	error = AMColor.md_theme_dark_error,
	errorContainer = AMColor.md_theme_dark_errorContainer,
	onError = AMColor.md_theme_dark_onError,
	onErrorContainer = AMColor.md_theme_dark_onErrorContainer,
	background = AMColor.md_theme_dark_background,
	onBackground = AMColor.md_theme_dark_onBackground,
	surface = AMColor.md_theme_dark_surface,
	onSurface = AMColor.md_theme_dark_onSurface,
	surfaceVariant = AMColor.md_theme_dark_surfaceVariant,
	onSurfaceVariant = AMColor.md_theme_dark_onSurfaceVariant,
	outline = AMColor.md_theme_dark_outline,
	inverseOnSurface = AMColor.md_theme_dark_inverseOnSurface,
	inverseSurface = AMColor.md_theme_dark_inverseSurface,
	inversePrimary = AMColor.md_theme_dark_inversePrimary,
	surfaceTint = AMColor.md_theme_dark_surfaceTint,
)

private val LightColorScheme = lightColorScheme(
	primary = AMColor.md_theme_light_primary,
	onPrimary = AMColor.md_theme_light_onPrimary,
	primaryContainer = AMColor.md_theme_light_primaryContainer,
	onPrimaryContainer = AMColor.md_theme_light_onPrimaryContainer,
	secondary = AMColor.md_theme_light_secondary,
	onSecondary = AMColor.md_theme_light_onSecondary,
	secondaryContainer = AMColor.md_theme_light_secondaryContainer,
	onSecondaryContainer = AMColor.md_theme_light_onSecondaryContainer,
	tertiary = AMColor.md_theme_light_tertiary,
	onTertiary = AMColor.md_theme_light_onTertiary,
	tertiaryContainer = AMColor.md_theme_light_tertiaryContainer,
	onTertiaryContainer = AMColor.md_theme_light_onTertiaryContainer,
	error = AMColor.md_theme_light_error,
	errorContainer = AMColor.md_theme_light_errorContainer,
	onError = AMColor.md_theme_light_onError,
	onErrorContainer = AMColor.md_theme_light_onErrorContainer,
	background = AMColor.md_theme_light_background,
	onBackground = AMColor.md_theme_light_onBackground,
	surface = AMColor.md_theme_light_surface,
	onSurface = AMColor.md_theme_light_onSurface,
	surfaceVariant = AMColor.md_theme_light_surfaceVariant,
	onSurfaceVariant = AMColor.md_theme_light_onSurfaceVariant,
	outline = AMColor.md_theme_light_outline,
	inverseOnSurface = AMColor.md_theme_light_inverseOnSurface,
	inverseSurface = AMColor.md_theme_light_inverseSurface,
	inversePrimary = AMColor.md_theme_light_inversePrimary,
	surfaceTint = AMColor.md_theme_light_surfaceTint
)

@Composable
fun AirMessageAndroidTheme(
	darkTheme: Boolean = ThemeHelper.darkModeOverride ?: isSystemInDarkTheme(),
	// Dynamic color is available on Android 12+
	dynamicColor: Boolean = true,
	content: @Composable () -> Unit
) {
	val colorScheme = when {
		dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
			val context = LocalContext.current
			if(darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
		}
		darkTheme -> DarkColorScheme
		else -> LightColorScheme
	}
	val view = LocalView.current
	if(!view.isInEditMode) {
		SideEffect {
			val activity = view.context as Activity
			WindowCompat.getInsetsController(activity.window, view).isAppearanceLightStatusBars = !darkTheme
		}
	}
	
	MaterialTheme(
		colorScheme = colorScheme,
		typography = Typography,
		content = content
	)
}

@Composable
@ReadOnlyComposable
fun isAppDarkTheme() = ThemeHelper.darkModeOverride ?: isSystemInDarkTheme()
