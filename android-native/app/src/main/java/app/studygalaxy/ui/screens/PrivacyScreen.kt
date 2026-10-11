package app.studygalaxy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.studygalaxy.ui.theme.*

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back", color = White70, fontSize = 13.sp) }
            Spacer(Modifier.weight(1f))
            Text("Privacy", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f)); Spacer(Modifier.width(48.dp))
        }
        Spacer(Modifier.height(16.dp))

        val body = listOf(
            "StudyGalaxy is built so students can focus and study together. We keep what we collect to the minimum.",
            "What we store:",
            "• Your phone number (if you created an account) — used only as your login ID. We never send SMS.",
            "• Your password, stored as a one-way scrypt hash. We cannot read it.",
            "• A random device ID generated on your phone.",
            "• Your cosmic name, sessions, subjects, and circles.",
            "What we do NOT collect:",
            "• No email, no address, no real name.",
            "• No contacts, no location, no camera or mic.",
            "• No ad trackers, no third-party analytics.",
            "You can delete your account anytime from Settings → Delete account.",
            "Last updated: 2026-10-11"
        )
        body.forEach {
            Text(it, color = White70, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}
