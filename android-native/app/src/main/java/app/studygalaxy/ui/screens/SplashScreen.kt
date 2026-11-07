package app.studygalaxy.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.studygalaxy.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Launch animation: scattered particles fall inward and collapse into the core,
 * the wordmark resolves out of wide letter-spacing, then the app routes on.
 *
 * Everything is drawn, not loaded — no image decode on the critical path, so
 * this costs nothing against cold-start time.
 */

private class Mote(val angle: Float, val distance: Float, val radius: Float, val hue: Color)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val motes = remember {
        val rnd = Random(4242)
        List(90) {
            Mote(
                angle = rnd.nextFloat() * 6.2831855f,
                distance = 0.45f + rnd.nextFloat() * 0.75f,
                radius = 1.2f + rnd.nextFloat() * 2.6f,
                hue = listOf(Violet300, Fuchsia500, Amber300, Color.White).random(rnd)
            )
        }
    }

    // 0 -> scattered, 1 -> collapsed into the core
    val collapse = remember { Animatable(0f) }
    val coreGlow = remember { Animatable(0f) }
    var showWord by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showWord = true
        coreGlow.animateTo(1f, tween(900, easing = EaseOutCubic))
        collapse.animateTo(1f, tween(1100, easing = EaseInOutCubic))
        delay(320)
        onFinished()
    }

    val spacing by animateFloatAsState(
        targetValue = if (showWord) 0f else 22f,
        animationSpec = tween(1500, easing = EaseOutCubic),
        label = "spacing"
    )
    val wordAlpha by animateFloatAsState(
        targetValue = if (showWord) 1f else 0f,
        animationSpec = tween(1200),
        label = "wordAlpha"
    )

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height * 0.42f)
            val reach = size.minDimension * 0.5f
            val t = collapse.value

            // Core
            val coreR = 26f + 46f * coreGlow.value
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White, Fuchsia500.copy(alpha = 0.85f), Color.Transparent),
                    center = c, radius = coreR * 2.4f
                ),
                radius = coreR * 2.4f, center = c
            )
            drawCircle(
                brush = Brush.linearGradient(listOf(Violet500, Fuchsia500)),
                radius = coreR, center = c
            )

            // Motes falling in
            motes.forEach { m ->
                val d = m.distance * reach * (1f - t)
                val swirl = m.angle + t * 1.5f
                val p = Offset(c.x + cos(swirl) * d, c.y + sin(swirl) * d)
                drawCircle(
                    color = m.hue,
                    radius = m.radius * (1f - t * 0.55f),
                    center = p,
                    alpha = (1f - t * 0.85f).coerceIn(0f, 1f)
                )
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(top = 210.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "StudyGalaxy",
                style = MaterialTheme.typography.displayMedium.copy(brush = gradientText()),
                fontSize = 38.sp,
                letterSpacing = spacing.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = wordAlpha }
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "FOCUS. BUILD WORLDS.",
                color = White50,
                fontSize = 10.sp,
                letterSpacing = 5.sp,
                modifier = Modifier.graphicsLayer { alpha = wordAlpha * 0.9f }
            )
        }
    }
}
