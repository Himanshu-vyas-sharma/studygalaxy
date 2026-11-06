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
            "• A random device ID generated on your phone. Nothing identifies you personally.",
            "• Your cosmic name, sessions, subjects, and circles.",
            "What we do NOT collect:",
            "• No phone number. No email. No password. No real name.",
            "• No contacts, no location, no camera or mic.",
            "• No ad trackers, no third-party analytics.",
            "Because there is no account to sign in to, your galaxy lives on this " +
                "device. Android's backup can carry it to a new phone, but if you " +
                "uninstall with backup turned off, it is gone for good.",
            "Last updated: 2026-11-06"
        )
        body.forEach {
            Text(it, color = White70, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}
