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
import app.studygalaxy.data.model.Planet
import app.studygalaxy.data.model.User
import app.studygalaxy.ui.components.Avatar
import app.studygalaxy.ui.theme.*

@Composable
fun HomeScreen(
    user: User,
    subjects: List<Planet>,
    activeSubjectId: String?,
    onPickSubject: (String) -> Unit,
    onCreateSubject: () -> Unit,
    onStartFocus: () -> Unit,
    onOpenGalaxy: () -> Unit,
    onOpenCircles: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenNotes: () -> Unit,
    onSaveAccount: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val activeSubject = subjects.firstOrNull { it.id == activeSubjectId }

    val infinite = rememberInfiniteTransition(label = "home")
    val portalScale by infinite.animateFloat(1f, 1.04f,
        infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse), label = "ps")
    val ringRotate by infinite.animateFloat(0f, 360f,
        infiniteRepeatable(tween(20000, easing = LinearEasing)), label = "rr")

    Column(Modifier.fillMaxSize().padding(bottom = 8.dp)) {

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

        // A guest account exists only on this phone — there is no phone number
        // or password, so it can never be signed into anywhere else. Say so
        // plainly rather than letting them discover it on a new device.
        if (user.phoneNumber == null) {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .background(Color(0x1FFBBF24), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x55FBBF24), RoundedCornerShape(12.dp))
                    .clickable(onClick = onSaveAccount)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("This galaxy lives only on this phone", color = White90, fontSize = 12.sp)
                    Text(
                        "Add a number and password to keep it if you change device.",
                        color = White50, fontSize = 11.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("Save →", color = Color(0xFFFBBF24), fontSize = 12.sp)
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
                    Text(activeSubject?.name ?: "Pick a subject below", color = White70, fontSize = 13.sp)
                }
            }
            // Clickable overlay (no ripple)
            val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Box(
                Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .clickable(interactionSource = interaction, indication = null) {
                        if (activeSubjectId == null) {
                            // nothing — subject must be picked first (handled by UI below)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStartFocus()
                        }
                    }
            )
        }

        Spacer(Modifier.height(28.dp))

        // Subject chips row (horizontal scroll)
        Text(
            "YOUR PLANETS",
            color = White50, fontSize = 10.sp, letterSpacing = 2.sp,
            modifier = Modifier.padding(start = 20.dp)
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            subjects.forEach { s ->
                SubjectChip(
                    name = s.name,
                    color = parseHexColor(s.color),
                    selected = s.id == activeSubjectId,
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onPickSubject(s.id) }
                )
            }
            // Add-new chip
            Row(
                Modifier
                    .background(White5, RoundedCornerShape(50))
                    .border(1.dp, White10, RoundedCornerShape(50))
                    .clickable { onCreateSubject() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) { Text("+ New", color = White70, fontSize = 12.sp) }
        }

        Spacer(Modifier.weight(1f))

        // Nav bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavBtn("Home", true) {}
            NavBtn("Galaxy", false, onOpenGalaxy)
            NavBtn("Notes", false, onOpenNotes)
            NavBtn("Circles", false, onOpenCircles)
            NavBtn("Stats", false, onOpenStats)
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SubjectChip(name: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .background(
                if (selected) Brush.horizontalGradient(listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0.2f)))
                else Brush.horizontalGradient(listOf(White5, White5)),
                RoundedCornerShape(50)
            )
            .border(1.dp, if (selected) color else White10, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(name, color = White90, fontSize = 12.sp)
    }
}

private fun parseHexColor(hex: String): Color {
    val clean = hex.removePrefix("#").let { if (it.length == 6) "FF$it" else it }
    return Color(clean.toLong(16))
}

@Composable
private fun NavBtn(label: String, active: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(label, color = if (active) White90 else White50, fontSize = 12.sp)
    }
}

