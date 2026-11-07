package app.studygalaxy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.studygalaxy.data.model.User
import app.studygalaxy.ui.components.Avatar
import app.studygalaxy.ui.theme.*

/**
 * Account and settings. Exists mainly because there was nowhere to sign out
 * from, and nowhere the privacy policy could be reached — Play requires the
 * latter to be reachable in-app.
 */
@Composable
fun AccountScreen(
    user: User,
    onOpenPrivacy: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    var confirming by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Text("← Back", color = White70, fontSize = 13.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("Account", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f)); Spacer(Modifier.width(48.dp))
        }

        Spacer(Modifier.height(28.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(seed = user.avatarSeed, name = user.cosmicName, size = 58.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(user.cosmicName, color = White90, fontSize = 18.sp)
                Text("Stargazer Lv.${user.level}", color = Violet300.copy(alpha = 0.85f), fontSize = 12.sp)
                user.phoneNumber?.let { Text(maskPhone(it), color = White50, fontSize = 12.sp) }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth()) {
            Stat("Focused", "${user.totalFocusMin / 60}h ${user.totalFocusMin % 60}m", Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            Stat("Streak", "${user.streakDays}d", Modifier.weight(1f))
        }

        Spacer(Modifier.height(28.dp))

        RowItem("Privacy", "What we store and what we never touch", onOpenPrivacy)

        Spacer(Modifier.height(10.dp))

        if (!confirming) {
            RowItem("Log out", "You'll need your number and password to get back in") {
                confirming = true
            }
        } else {
            Column(
                Modifier.fillMaxWidth()
                    .background(Color(0x22FF4D5A), RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0x55FF4D5A), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text("Log out of StudyGalaxy?", color = White90, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Your galaxy stays safe on the server. Log back in with the same " +
                        "number and it all comes back.",
                    color = White70, fontSize = 12.sp, lineHeight = 17.sp
                )
                Spacer(Modifier.height(12.dp))
                Row {
                    Button(
                        onClick = { confirming = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = White10)
                    ) { Text("Cancel", color = White90, fontSize = 13.sp) }
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB4253A))
                    ) { Text("Log out", color = Color.White, fontSize = 13.sp) }
                }
            }
        }
    }
}

/** Shows enough of the number to recognise, not enough to read over a shoulder. */
private fun maskPhone(p: String): String =
    if (p.length <= 4) p else p.take(p.length - 7).padEnd(p.length - 4, '•') + p.takeLast(4)

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(White5, RoundedCornerShape(14.dp))
            .border(1.dp, White10, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(label.uppercase(), color = White50, fontSize = 9.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = White90, fontSize = 18.sp)
    }
}

@Composable
private fun RowItem(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(White5, RoundedCornerShape(14.dp))
            .border(1.dp, White10, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = White90, fontSize = 14.sp)
            Text(subtitle, color = White50, fontSize = 11.sp)
        }
        Text("→", color = White50, fontSize = 15.sp)
    }
}
