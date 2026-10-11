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

@Composable
fun AuthLandingScreen(
    onCreateAccount: () -> Unit,
    onLogin: () -> Unit,
    onGuest: () -> Unit
) {
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

            Button(
                onClick = onCreateAccount,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Violet500, Fuchsia500)), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("Create account", color = White90, fontSize = 16.sp) }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth().height(56.dp)
                    .border(1.dp, White10, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = White5)
            ) { Text("Log in", color = White90, fontSize = 16.sp) }

            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onGuest) { Text("Continue as guest", color = White50, fontSize = 13.sp) }
        }
    }
}
