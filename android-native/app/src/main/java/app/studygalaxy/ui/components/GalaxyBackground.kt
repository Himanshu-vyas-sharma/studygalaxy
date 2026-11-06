package app.studygalaxy.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * Star field drawn behind every screen.
 *
 * Performance notes — this runs on every frame of every screen, so the hot
 * path must allocate nothing:
 *  - Colors and positions are computed once per size, not per frame. Rebuilding
 *    an HSL colour for every star on every frame was the single biggest cost.
 *  - Gradient brushes are built once in drawWithCache. Allocating a
 *    radialGradient per frame forces the renderer to rebuild its shader each
 *    time.
 *  - Star count is capped. Scaling by screen area put ~780 individual draw
 *    calls on a 1080p+ phone, which no amount of micro-tuning can rescue.
 *  - Twinkle alpha uses drawCircle's alpha parameter instead of Color.copy(),
 *    which would allocate a Color per star per frame.
 *  - Nebulae are static. They drifted by 20px over ~60 seconds, which nobody
 *    can perceive, and it forced the gradients to be rebuilt constantly.
 */

private const val MAX_STARS = 170
private const val PX_PER_STAR = 14_000f

private class StarDot(
    val center: Offset,
    val radius: Float,
    val color: Color,
    val twinklePhase: Float,
    val twinkleSpeed: Float
)

private class Nebula(val brush: Brush, val center: Offset, val radius: Float)

@Composable
fun GalaxyBackground(modifier: Modifier = Modifier) {
    // Float-specialised so the per-frame write does not box.
    val time = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val startNs = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { nowNs -> time.floatValue = (nowNs - startNs) / 1_000_000_000f }
        }
    }

    Spacer(
        modifier
            .fillMaxSize()
            .drawWithCache {
                // ---- Built once per size change, not per frame ----
                val w = size.width
                val h = size.height
                val maxDim = max(w, h)

                val base = Brush.radialGradient(
                    colors = listOf(Color(0xFF140833), Color(0xFF0A0520), Color(0xFF05020D)),
                    center = Offset(w * 0.5f, h * 0.4f),
                    radius = maxDim
                )

                val nebulae = listOf(
                    nebulaAt(w * 0.30f, h * 0.30f, maxDim * 0.40f, Color(0xFF8B5CF6), 0.18f),
                    nebulaAt(w * 0.75f, h * 0.70f, maxDim * 0.45f, Color(0xFFEC4899), 0.14f),
                    nebulaAt(w * 0.60f, h * 0.20f, maxDim * 0.30f, Color(0xFF3B82F6), 0.10f),
                )

                val stars = buildStars(size)

                onDrawBehind {
                    val t = time.floatValue

                    drawRect(brush = base)

                    // Plain source-over: BlendMode.Plus forced an offscreen
                    // composite of the whole screen every frame.
                    nebulae.forEach { n ->
                        drawCircle(brush = n.brush, radius = n.radius, center = n.center)
                    }

                    stars.forEach { s ->
                        val alpha = 0.40f + 0.60f * (0.5f + 0.5f * sin(t * s.twinkleSpeed + s.twinklePhase))
                        drawCircle(
                            color = s.color,
                            radius = s.radius,
                            center = s.center,
                            alpha = if (alpha < 0f) 0f else if (alpha > 1f) 1f else alpha
                        )
                    }
                }
            }
    )
}

private fun nebulaAt(cx: Float, cy: Float, r: Float, color: Color, alpha: Float): Nebula {
    val c = Offset(cx, cy)
    return Nebula(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = c,
            radius = r
        ),
        center = c,
        radius = r
    )
}

private fun buildStars(size: Size): List<StarDot> {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return emptyList()
    val count = ((w * h) / PX_PER_STAR).toInt().coerceIn(60, MAX_STARS)
    // Fixed seed: the sky should not reshuffle on every rotation or resize.
    val rnd = Random(0x5747)
    return List(count) {
        StarDot(
            center = Offset(rnd.nextFloat() * w, rnd.nextFloat() * h),
            radius = rnd.nextFloat() * 1.4f + 0.3f,
            color = hslColor(220f + rnd.nextFloat() * 80f, 0.9f, 0.85f),
            twinklePhase = rnd.nextFloat() * 2f * PI.toFloat(),
            twinkleSpeed = 0.4f + rnd.nextFloat() * 1.5f
        )
    }
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
