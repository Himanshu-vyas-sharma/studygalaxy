package app.studygalaxy.util

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private fun hash(str: String): Int {
    var h = 2166136261u
    for (c in str) {
        h = (h xor c.code.toUInt())
        h *= 16777619u
    }
    return h.toInt()
}

fun gradientFromSeed(seed: String): Brush {
    val h = hash(seed).toLong() and 0xFFFFFFFFL
    val a = (h % 360).toFloat()
    val b = ((a + 60 + ((h shr 8) % 120)) % 360).toFloat()
    return Brush.linearGradient(colors = listOf(hslToColor(a, 0.75f, 0.6f), hslToColor(b, 0.80f, 0.5f)))
}

fun initialsOf(name: String): String {
    val cleaned = name.replace(Regex("[^A-Za-z0-9 ]"), "")
    val parts = cleaned.split(Regex("(?=[A-Z])|\\s+")).filter { it.isNotEmpty() }
    val first = parts.firstOrNull()?.firstOrNull() ?: '*'
    val second = parts.getOrNull(1)?.firstOrNull() ?: parts.firstOrNull()?.getOrNull(1) ?: ' '
    return "${first.uppercaseChar()}${second.toString().uppercase()}".trim()
}

private fun hslToColor(h: Float, s: Float, l: Float): Color {
    val c = (1 - kotlin.math.abs(2 * l - 1)) * s
    val x = c * (1 - kotlin.math.abs(((h / 60f) % 2) - 1))
    val m = l - c / 2
    val (r, g, b) = when {
        h < 60 -> Triple(c, x, 0f)
        h < 120 -> Triple(x, c, 0f)
        h < 180 -> Triple(0f, c, x)
        h < 240 -> Triple(0f, x, c)
        h < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}
