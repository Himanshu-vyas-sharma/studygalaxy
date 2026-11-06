package app.studygalaxy.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.studygalaxy.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AuthLandingScreen(
    onCreateAccount: () -> Unit,
    onLogin: () -> Unit,
    onGuest: suspend () -> String?
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val infinite = rememberInfiniteTransition(label = "orbFloat")
    val float by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "float"
    )
    val titleAlpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(900), label = "titleA")

    Box(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(120.dp)
                    .graphicsLayer { translationY = -float * 10f; rotationZ = float * 4f }
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Violet500, Fuchsia500, Indigo700)))
                    .shadow(40.dp, CircleShape, ambientColor = Violet500, spotColor = Violet500)
            )
            Spacer(Modifier.height(32.dp))
            Text(
                "Welcome to",
                color = Violet300.copy(alpha = 0.7f),
                letterSpacing = 6.sp,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "StudyGalaxy",
                style = androidx.compose.material3.MaterialTheme.typography.displayLarge.copy(
                    brush = gradientText()
                ),
                modifier = Modifier.graphicsLayer { alpha = titleAlpha }
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Focus. Grow your galaxy. Study with friends.",
                color = White70,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(40.dp))

            // Primary path is no-account entry: no phone, no email, no password.
            Button(
                onClick = {
                    busy = true; err = null
                    scope.launch {
                        val e = onGuest()
                        busy = false
                        if (e != null) err = e
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Violet500, Fuchsia500)), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (busy) "Opening your galaxy…" else "Start focusing",
                        color = White90, fontSize = 16.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "No email. No phone. No password.",
                color = White50, fontSize = 11.sp, textAlign = TextAlign.Center
            )

            err?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = Color(0xFFFCA5A5), fontSize = 12.sp, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(22.dp))
            Text(
                "Want your galaxy on more than one phone?",
                color = White50, fontSize = 11.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                Button(
                    onClick = onCreateAccount,
                    modifier = Modifier.weight(1f).height(50.dp)
                        .border(1.dp, White10, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = White5)
                ) { Text("Create account", color = White90, fontSize = 13.sp) }
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = onLogin,
                    modifier = Modifier.weight(1f).height(50.dp)
                        .border(1.dp, White10, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = White5)
                ) { Text("Log in", color = White90, fontSize = 13.sp) }
            }
        }
    }
}
