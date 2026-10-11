package app.studygalaxy.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.studygalaxy.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun TimerScreen(
    endsAtMs: Long,
    onComplete: suspend () -> Unit,
    onBreak: suspend (reason: String) -> Unit,
    onDone: () -> Unit
) {
    var remaining by remember { mutableStateOf(endsAtMs - System.currentTimeMillis()) }
    var completed by remember { mutableStateOf(false) }
    var warningVisible by remember { mutableStateOf(false) }
    var brokenMessage by remember { mutableStateOf<String?>(null) }
    var leftAt by remember { mutableStateOf<Long?>(null) }

    // Observe lifecycle to detect leaving the app
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    leftAt = System.currentTimeMillis()
                    warningVisible = true
                }
                Lifecycle.Event.ON_START -> {
                    val la = leftAt
                    if (la != null) {
                        val away = System.currentTimeMillis() - la
                        leftAt = null
                        if (away > 10_000 && !completed && brokenMessage == null) {
                            brokenMessage = "You left for too long. Your star collapsed."
                        } else {
                            warningVisible = false
                        }
                    }
                }
                else -> {}
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    // Tick
    LaunchedEffect(endsAtMs, completed, brokenMessage) {
        while (!completed && brokenMessage == null) {
            remaining = endsAtMs - System.currentTimeMillis()
            if (remaining <= 0) {
                completed = true
                onComplete()
                break
            }
            delay(250)
        }
    }

    // Dispatch break
    LaunchedEffect(brokenMessage) {
        brokenMessage?.let { onBreak(it); delay(2500); onDone() }
    }
    LaunchedEffect(completed) {
        if (completed) { delay(1800); onDone() }
    }

    when {
        completed -> StarBornView()
        brokenMessage != null -> BlackHoleView(brokenMessage!!)
        else -> TimerView(remaining = remaining, warning = warningVisible, onGiveUp = {
            brokenMessage = "You gave up this session."
        })
    }
}

@Composable
private fun TimerView(remaining: Long, warning: Boolean, onGiveUp: () -> Unit) {
    val mins = (remaining.coerceAtLeast(0) / 60000).toInt()
    val secs = ((remaining.coerceAtLeast(0) / 1000) % 60).toInt()
    val text = "%02d:%02d".format(mins, secs)

    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulse by infinite.animateFloat(
        1f, if (warning) 1.08f else 1.02f,
        infiniteRepeatable(tween(if (warning) 600 else 2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "p"
    )

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("FOCUSING", color = Violet300.copy(alpha = 0.7f), fontSize = 11.sp, letterSpacing = 6.sp)
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .size(260.dp)
                .graphicsLayer { scaleX = pulse; scaleY = pulse }
                .shadow(60.dp, CircleShape, ambientColor = Violet500, spotColor = Fuchsia500)
                .clip(CircleShape)
                .background(
                    if (warning) Brush.linearGradient(listOf(Color(0xFFDC2626), Fuchsia500, Violet900))
                    else portalGradient()
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text, style = MaterialTheme.typography.displayLarge, color = White90)
        }
        if (warning) {
            Spacer(Modifier.height(16.dp))
            Text("Come back! Your star is about to collapse…", color = Color(0xFFFCA5A5), fontSize = 13.sp)
        }
        Spacer(Modifier.height(32.dp))
        TextButton(onClick = onGiveUp) { Text("Give up", color = White30, fontSize = 11.sp) }
    }
}

@Composable
private fun StarBornView() {
    val infinite = rememberInfiniteTransition(label = "star")
    val pulse by infinite.animateFloat(1f, 1.15f,
        infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse), label = "sp")

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(160.dp)
                .graphicsLayer { scaleX = pulse; scaleY = pulse }
                .shadow(80.dp, CircleShape, ambientColor = Amber300, spotColor = Fuchsia500)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Amber300, Fuchsia400, Violet500)))
        )
        Spacer(Modifier.height(32.dp))
        Text("A new star is born", style = MaterialTheme.typography.displaySmall.copy(brush = gradientText()))
        Spacer(Modifier.height(8.dp))
        Text("Your galaxy just grew.", color = White70)
    }
}

@Composable
private fun BlackHoleView(msg: String) {
    val infinite = rememberInfiniteTransition(label = "hole")
    val rot by infinite.animateFloat(0f, 360f,
        infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "r")

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(140.dp)
                .graphicsLayer { rotationZ = rot }
                .clip(CircleShape)
                .background(Color.Black)
                .border(4.dp, Violet900, CircleShape)
                .shadow(80.dp, CircleShape, ambientColor = Violet900, spotColor = Violet900)
        )
        Spacer(Modifier.height(28.dp))
        Text("Black hole formed", style = MaterialTheme.typography.headlineLarge, color = Fuchsia400)
        Spacer(Modifier.height(8.dp))
        Text(msg, color = White70, fontSize = 13.sp)
    }
}
