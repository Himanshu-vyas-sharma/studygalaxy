package app.studygalaxy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.isActive
import kotlin.math.*
import androidx.compose.runtime.withFrameNanos

private data class StarDot(var x: Float, var y: Float, val r: Float, val twinkle: Float, val speed: Float, val hue: Float)

@Composable
fun GalaxyBackground(modifier: Modifier = Modifier) {
    var t by remember { mutableStateOf(0f) }
    var stars by remember { mutableStateOf<List<StarDot>>(emptyList()) }

    LaunchedEffect(Unit) {
        val startNs = System.nanoTime()
        while (isActive) {
            withFrameNanos { nowNs ->
                t = ((nowNs - startNs) / 1_000_000_000f)
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (stars.isEmpty() || stars.size.toFloat() != ((w * h) / 4500f).toInt().toFloat()) {
            val count = ((w * h) / 4500f).toInt().coerceAtLeast(60)
            stars = List(count) {
                StarDot(
                    x = (Math.random() * w).toFloat(),
                    y = (Math.random() * h).toFloat(),
                    r = (Math.random() * 1.4f + 0.3f).toFloat(),
                    twinkle = (Math.random() * 2 * PI).toFloat(),
                    speed = (0.4f + Math.random() * 1.5f).toFloat(),
                    hue = (220f + Math.random() * 80f).toFloat()
                )
            }
        }

        // Deep space base gradient
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF140833), Color(0xFF0A0520), Color(0xFF05020D)),
                center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.4f),
                radius = max(w, h)
            )
        )
        // Nebula clouds (lighter blend)
        nebula(w * 0.3f + sin(t * 0.1f) * 20f, h * 0.3f, max(w, h) * 0.4f, Color(0xFF8B5CF6).copy(alpha = 0.18f))
        nebula(w * 0.75f + cos(t * 0.08f) * 20f, h * 0.7f, max(w, h) * 0.45f, Color(0xFFEC4899).copy(alpha = 0.14f))
        nebula(w * 0.6f, h * 0.2f, max(w, h) * 0.3f, Color(0xFF3B82F6).copy(alpha = 0.10f))

        // Stars (twinkle)
        stars.forEach { s ->
            val alpha = (0.4f + 0.6f * (0.5f + 0.5f * sin(t * s.speed + s.twinkle))).coerceIn(0f, 1f)
            drawCircle(color = hslColor(s.hue, 0.9f, 0.85f).copy(alpha = alpha), radius = s.r,
                center = androidx.compose.ui.geometry.Offset(s.x, s.y))
        }
    }
}

private fun DrawScope.nebula(cx: Float, cy: Float, r: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = androidx.compose.ui.geometry.Offset(cx, cy),
            radius = r
        ),
        radius = r,
        center = androidx.compose.ui.geometry.Offset(cx, cy),
        blendMode = BlendMode.Plus
    )
}

private fun hslColor(h: Float, s: Float, l: Float): Color {
    val c = (1 - abs(2 * l - 1)) * s
    val x = c * (1 - abs(((h / 60f) % 2) - 1))
    val m = l - c / 2
    val triple = when {
        h < 60 -> Triple(c, x, 0f)
        h < 120 -> Triple(x, c, 0f)
        h < 180 -> Triple(0f, c, x)
        h < 240 -> Triple(0f, x, c)
        h < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(triple.first + m, triple.second + m, triple.third + m)
}
