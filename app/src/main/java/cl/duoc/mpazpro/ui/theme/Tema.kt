package cl.duoc.mpazpro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta propia: azul institucional + acentos cálidos con buen contraste (WCAG AA sobre fondo claro)
val Azul = Color(0xFF1F4E79)
val AzulClaro = Color(0xFFD6E6F5)
val Amarillo = Color(0xFFFFC93C)
val Verde = Color(0xFF2B8A3E)
val VerdeClaro = Color(0xFFD3F9D8)
val Rojo = Color(0xFFC92A2A)
val RojoClaro = Color(0xFFFFE3E3)
val Fondo = Color(0xFFFFFBF5)

private val Claro = lightColorScheme(
    primary = Azul, onPrimary = Color.White,
    primaryContainer = AzulClaro, onPrimaryContainer = Color(0xFF0B2540),
    secondary = Color(0xFF8A5A00), secondaryContainer = Color(0xFFFFE8B0), onSecondaryContainer = Color(0xFF2B1D00),
    tertiary = Verde, tertiaryContainer = VerdeClaro,
    error = Rojo, errorContainer = RojoClaro,
    background = Fondo, surface = Fondo,
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFFA8C8EC), onPrimary = Color(0xFF0B2540),
    primaryContainer = Color(0xFF173B5C), onPrimaryContainer = AzulClaro,
    secondary = Amarillo, tertiary = Color(0xFF8CE99A),
    error = Color(0xFFFFA8A8),
)

/**
 * Tipografía grande para lectores iniciales (condición 3.3 del caso):
 * el texto base es 18 sp y los títulos superan los 26 sp.
 */
private val TipografiaGrande = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 26.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 18.sp, lineHeight = 27.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 23.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp),
)

@Composable
fun MPazTema(oscuro: Boolean = isSystemInDarkTheme(), contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (oscuro) Oscuro else Claro, typography = TipografiaGrande, content = contenido)
}
