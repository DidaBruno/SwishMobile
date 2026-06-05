package hr.ferit.brunodidovic.swish.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SwishColorScheme = darkColorScheme(
    primary = Orange,
    onPrimary = TextPrimary,
    primaryContainer = Surface2,
    onPrimaryContainer = TextPrimary,
    secondary = Blue,
    onSecondary = TextPrimary,
    background = Bg,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextMuted,
    outline = Border,
    error = Error,
    onError = TextPrimary,
)

@Composable
fun SwishTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SwishColorScheme,
        typography = SwishTypography,
        content = content
    )
}