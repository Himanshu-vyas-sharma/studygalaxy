package app.studygalaxy.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Void = Color(0xFF05020D)
val Space = Color(0xFF0B0620)
val Nebula = Color(0xFF1A0F3B)

val Violet500 = Color(0xFF8B5CF6)
val Violet600 = Color(0xFF7C3AED)
val Violet700 = Color(0xFF6D28D9)
val Violet900 = Color(0xFF4C1D95)
val Violet300 = Color(0xFFC4B5FD)
val Fuchsia500 = Color(0xFFEC4899)
val Fuchsia400 = Color(0xFFF472B6)
val Amber300 = Color(0xFFFBBF24)
val Indigo700 = Color(0xFF4338CA)

val White90 = Color(0xE6FFFFFF)
val White70 = Color(0xB3FFFFFF)
val White50 = Color(0x80FFFFFF)
val White30 = Color(0x4DFFFFFF)
val White10 = Color(0x1AFFFFFF)
val White5  = Color(0x0DFFFFFF)

fun gradientText(): Brush = Brush.linearGradient(
    colors = listOf(Violet300, Fuchsia400, Amber300),
    tileMode = androidx.compose.ui.graphics.TileMode.Clamp
)

fun portalGradient(): Brush = Brush.linearGradient(
    colors = listOf(Violet600, Fuchsia500, Indigo700)
)
