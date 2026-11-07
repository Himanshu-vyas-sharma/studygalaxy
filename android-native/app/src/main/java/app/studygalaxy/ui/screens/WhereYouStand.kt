package app.studygalaxy.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.studygalaxy.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The turn of the onboarding: instead of promising what the app will do, it
 * shows the student their own arithmetic. Hours a day on a screen becomes days
 * a year, drawn as a ring of worlds that go dark one by one — then a second
 * figure showing what two reclaimed hours a day would give back.
 *
 * Deliberately not a pep talk. The numbers are the user's own; the app only
 * multiplies them.
 */

/** Daily screen hours -> the sentence underneath the big number. */
private fun verdictFor(daysLost: Int): String = when {
    daysLost >= 120 -> "That is a third of your year, spent looking down."
    daysLost >= 90 -> "That is one season of your life, every year."
    daysLost >= 60 -> "Two months a year, gone without a decision."
    daysLost >= 30 -> "A full month a year, in pieces you never noticed."
    else -> "Less than most. There is still room to take back."
}

@Composable
fun WhereYouStandScreen(
    screenHoursPerDay: Float,
    examLabel: String?,
    distractionLabel: String?,
    onSwell: () -> Unit = {},
    onContinue: () -> Unit
) {
    val hoursPerYear = (screenHoursPerDay * 365f).roundToInt()
    val daysLost = (hoursPerYear / 24f).roundToInt()
    // Reclaiming two hours a day is a concrete, survivable ask.
    val reclaimable = (minOf(screenHoursPerDay, 2f) * 365f).roundToInt()
    val reclaimDays = (reclaimable / 24f).roundToInt()

    var phase by remember { mutableIntStateOf(0) }

    val counter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        onSwell()
        delay(900)
        phase = 1
        counter.animateTo(
            hoursPerYear.toFloat(),
            animationSpec = tween(2200, easing = EaseOutCubic)
        )
        phase = 2
        delay(2600)
        phase = 3
        delay(900)
        delay(1400)
        phase = 4
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "WHERE YOU STAND",
            color = White50, fontSize = 10.sp, letterSpacing = 4.sp
        )
        Spacer(Modifier.height(22.dp))

        // --- The dial: worlds going dark ---
        Box(Modifier.size(248.dp), contentAlignment = Alignment.Center) {
            val sweep by animateFloatAsState(
                targetValue = if (phase >= 1) (screenHoursPerDay / 24f).coerceIn(0f, 1f) else 0f,
                animationSpec = tween(2200, easing = EaseOutCubic),
                label = "sweep"
            )
            val pulse = rememberInfiniteTransition(label = "pulse")
            val glow by pulse.animateFloat(
                0.75f, 1f,
                infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
                label = "glow"
            )

            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f - 26f
                val c = Offset(size.width / 2f, size.height / 2f)

                // Track
                drawCircle(color = White10, radius = r, center = c, style = Stroke(width = 14f))

                // The portion of the day the phone takes
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(Color(0xFFF43F5E), Color(0xFFEC4899), Color(0xFFF59E0B), Color(0xFFF43F5E))
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * sweep,
                    useCenter = false,
                    topLeft = Offset(c.x - r, c.y - r),
                    size = Size(r * 2, r * 2),
                    style = Stroke(width = 14f)
                )

                // Twelve worlds around the ring; the lost share goes dark.
                val lit = 12 - (12 * sweep).roundToInt()
                repeat(12) { i ->
                    val a = (-90f + i * 30f) * (Math.PI / 180f).toFloat()
                    val p = Offset(c.x + cos(a) * (r + 26f), c.y + sin(a) * (r + 26f))
                    val isLit = i < lit
                    drawCircle(
                        color = if (isLit) Violet300.copy(alpha = glow) else Color(0xFF2A2140),
                        radius = if (isLit) 5.5f else 4f,
                        center = p
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${counter.value.roundToInt()}",
                    color = Color.White,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black
                )
                Text("HOURS A YEAR", color = White50, fontSize = 9.sp, letterSpacing = 3.sp)
                Spacer(Modifier.height(6.dp))
                AnimatedVisibility(phase >= 2, enter = fadeIn(tween(500))) {
                    Text(
                        "= $daysLost days",
                        color = Color(0xFFF59E0B), fontSize = 15.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        AnimatedVisibility(
            phase >= 2,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 4 }
        ) {
            Text(
                verdictFor(daysLost),
                color = White90, fontSize = 16.sp, textAlign = TextAlign.Center, lineHeight = 24.sp
            )
        }

        Spacer(Modifier.height(18.dp))

        AnimatedVisibility(
            phase >= 3,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 4 }
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(Color(0x3317C964), Color(0x1A8B5CF6))),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("TAKE BACK 2 HOURS A DAY", color = White50, fontSize = 9.sp, letterSpacing = 3.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "+$reclaimable hours",
                    color = Color(0xFF34D399), fontSize = 30.sp, fontWeight = FontWeight.Black
                )
                Text("$reclaimDays whole days, every year", color = White70, fontSize = 13.sp)
                if (examLabel != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Enough to change how $examLabel ends.",
                        color = White70, fontSize = 12.sp, textAlign = TextAlign.Center
                    )
                }
                if (distractionLabel != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Starting with $distractionLabel.",
                        color = White50, fontSize = 11.sp, textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(26.dp))

        AnimatedVisibility(phase >= 4, enter = fadeIn(tween(500))) {
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(listOf(Violet500, Fuchsia500)),
                        RoundedCornerShape(16.dp)
                    ),
                    contentAlignment = Alignment.Center
                ) { Text("Build my galaxy", color = White90, fontSize = 16.sp) }
            }
        }
    }
}
