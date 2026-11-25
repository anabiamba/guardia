package br.com.guardia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF9D285C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFBD6E6),
    onPrimaryContainer = Color(0xFF651B3C),
    secondary = Color(0xFF855368),
    secondaryContainer = Color(0xFFF3DEE6),
    onSecondaryContainer = Color(0xFF502D3E),
    background = Color(0xFFFFF5F8),
    onBackground = Color(0xFF3D2030),
    surface = Color(0xFFFFF8FA),
    onSurface = Color(0xFF3D2030),
    surfaceVariant = Color(0xFFF4E4EC),
    onSurfaceVariant = Color(0xFF5D3F4F),
    outline = Color(0xFF9A7285),
    error = Color(0xFFAA214B),
    onError = Color.White,
    errorContainer = Color(0xFFFFD9E3),
    onErrorContainer = Color(0xFF720C31)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFA7CA),
    onPrimary = Color(0xFF53102E),
    primaryContainer = Color(0xFF70284B),
    onPrimaryContainer = Color(0xFFFFD9E7),
    secondary = Color(0xFFF3B6CE),
    secondaryContainer = Color(0xFF512C40),
    onSecondaryContainer = Color(0xFFFFD9E7),
    background = Color(0xFF21131C),
    onBackground = Color(0xFFF9DFEB),
    surface = Color(0xFF291822),
    onSurface = Color(0xFFF9DFEB),
    surfaceVariant = Color(0xFF47303E),
    onSurfaceVariant = Color(0xFFE5BECF),
    outline = Color(0xFFC093A7),
    error = Color(0xFFFF8FB0),
    onError = Color(0xFF590822),
    errorContainer = Color(0xFF7A1C3E),
    onErrorContainer = Color(0xFFFFDBE6)
)

@Composable
fun GuardiaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colors, typography = Typography) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.background,
            contentColor = colors.onBackground,
            content = content
        )
    }
}
