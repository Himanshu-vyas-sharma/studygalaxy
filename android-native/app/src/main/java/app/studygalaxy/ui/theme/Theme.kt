package app.studygalaxy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Violet500,
    onPrimary = White90,
    secondary = Fuchsia400,
    onSecondary = White90,
    tertiary = Amber300,
    background = Void,
    onBackground = White90,
    surface = Nebula,
    onSurface = White90
)

@Composable
fun StudyGalaxyTheme(
    darkTheme: Boolean = true, // Galaxy is always dark
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content
    )
}
