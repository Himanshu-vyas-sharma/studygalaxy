package app.studygalaxy.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.studygalaxy.data.model.User
import app.studygalaxy.ui.components.Avatar
import app.studygalaxy.ui.theme.*

@Composable
fun HomeScreen(
    user: User,
    onStartFocus: (plannedMin: Int, intention: String) -> Unit,
    onOpenGalaxy: () -> Unit,
    onOpenCircles: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    val durations = listOf(25, 45, 90)
    var chosenMin by remember { mutableStateOf(25) }
    var intention by remember { mutableStateOf("") }
    var globalCount by remember { mutableStateOf(237) }
    val haptic = LocalHapticFeedback.current

    // Tick global ticker every 4s
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(4000)
            globalCount += if (Math.random() < 0.6) 1 else -1
        }
    }

    val infinite = rememberInfiniteTransition(label = "home")
    val portalScale by infinite.animateFloat(1f, 1.04f,
        infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse), label = "ps")
    val ringRotate by infinite.animateFloat(0f, 360f,
        infiniteRepeatable(tween(20000, easing = LinearEasing)), label = "rr")

    Column(Modifier.fillMaxSize().padding(top = 24.dp, bottom = 8.dp)) {

        // Live global ticker
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF34D399)))
            Spacer(Modifier.width(8.dp))
            Text(
                "$globalCount students focusing right now".uppercase(),
                color = Violet300.copy(alpha = 0.8f), fontSize = 10.sp, letterSpacing = 2.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        // Top strip
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(seed = user.avatarSeed, name = user.cosmicName, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(user.cosmicName, color = White90, fontSize = 14.sp)
                Text("Stargazer Lv.${user.level}", color = Violet300.copy(alpha = 0.8f), fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Streak", color = White50, fontSize = 11.sp)
                Text("🔥 ${user.streakDays}d", color = White90, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(28.dp))

        // Total hours
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TOTAL FOCUSED", color = White50, fontSize = 10.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${user.totalFocusMin / 60}",
                    style = MaterialTheme.typography.displayLarge.copy(brush = gradientText())
                )
                Text(
                    "h ${user.totalFocusMin % 60}m",
                    color = White70,
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp)
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Portal
        Box(contentAlignment = Alignment.Center, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            // orbiting dot
            Box(
                Modifier
                    .size(300.dp)
                    .graphicsLayer { rotationZ = ringRotate }
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Violet300)
                )
            }
            Box(
                Modifier
                    .size(220.dp)
                    .graphicsLayer { scaleX = portalScale; scaleY = portalScale }
                    .shadow(60.dp, CircleShape, ambientColor = Violet500, spotColor = Fuchsia500)
                    .clip(CircleShape)
                    .background(portalGradient())
                    .border(1.dp, White30, CircleShape)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TAP TO", color = White70, fontSize = 11.sp, letterSpacing = 3.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Focus", style = MaterialTheme.typography.headlineLarge, color = White90)
                    Spacer(Modifier.height(4.dp))
                    Text("$chosenMin min", color = White70, fontSize = 13.sp)
                }
            }
            // Clickable overlay (no ripple)
            val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Box(
                Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .clickable(interactionSource = interaction, indication = null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStartFocus(chosenMin, intention)
                    }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Duration picks
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            durations.forEach { m ->
                val selected = m == chosenMin
                Button(
                    onClick = { chosenMin = m },
                    modifier = Modifier.padding(horizontal = 4.dp).height(38.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) White10 else White5,
                        contentColor = White90
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) White30 else White10)
                ) { Text("${m}m", fontSize = 13.sp) }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Intention
        OutlinedTextField(
            value = intention,
            onValueChange = { intention = it.take(100) },
            placeholder = { Text("What will you crush this session?", color = White30, fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(50.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White90, unfocusedTextColor = White90,
                focusedBorderColor = Violet500, unfocusedBorderColor = White10,
                cursorColor = Violet500,
                focusedContainerColor = Color(0x0DFFFFFF), unfocusedContainerColor = Color(0x0DFFFFFF)
            )
        )

        Spacer(Modifier.height(20.dp))

        // Social feed strip
        Column {
            Text(
                "LIVE FEED",
                color = White50, fontSize = 10.sp, letterSpacing = 2.sp,
                modifier = Modifier.padding(start = 20.dp)
            )
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                feedCards().forEach { (who, what) ->
                    Column(
                        Modifier
                            .width(220.dp)
                            .background(White5, RoundedCornerShape(16.dp))
                            .border(1.dp, White10, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Text(who, color = White90, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(what, color = White70, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Nav bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavBtn("Home", true) {}
            NavBtn("Galaxy", false, onOpenGalaxy)
            NavBtn("Circles", false, onOpenCircles)
            NavBtn("Stats", false, onOpenStats)
        }

        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onOpenPrivacy, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Privacy", color = White30, fontSize = 11.sp)
        }
    }
}

@Composable
private fun NavBtn(label: String, active: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(label, color = if (active) White90 else White50, fontSize = 12.sp)
    }
}

private fun feedCards() = listOf(
    "Riya" to "just finished 90m on Physics",
    "Arjun" to "named a constellation: 'Midnight Grind'",
    "IIT Prep Squad" to "3 members focusing live",
    "Zoya" to "Chemistry planet gained an atmosphere"
)
