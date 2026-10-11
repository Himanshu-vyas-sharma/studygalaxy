package app.studygalaxy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Fallback to system serif / sans-serif.
// Design intent: Fraunces (serif, heavy) for display numbers; Inter-like sans for UI.
val DisplaySerif = FontFamily.Serif
val UiSans = FontFamily.SansSerif

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = DisplaySerif, fontWeight = FontWeight.Black, fontSize = 60.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = DisplaySerif, fontWeight = FontWeight.Black, fontSize = 44.sp, lineHeight = 48.sp),
    displaySmall = TextStyle(fontFamily = DisplaySerif, fontWeight = FontWeight.Black, fontSize = 36.sp, lineHeight = 40.sp),
    headlineLarge = TextStyle(fontFamily = DisplaySerif, fontWeight = FontWeight.Black, fontSize = 30.sp),
    titleLarge = TextStyle(fontFamily = UiSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = UiSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = UiSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = UiSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = UiSans, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = UiSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = UiSans, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = UiSans, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 2.sp)
)
