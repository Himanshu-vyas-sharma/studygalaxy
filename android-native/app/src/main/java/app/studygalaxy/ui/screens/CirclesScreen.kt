package app.studygalaxy.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import app.studygalaxy.BuildConfig
import app.studygalaxy.data.api.CirclePresenceState
import app.studygalaxy.data.api.CircleSocket
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.studygalaxy.data.model.Circle
import app.studygalaxy.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CirclesScreen(
    load: suspend () -> List<Circle>,
    create: suspend (String) -> Circle?,
    join: suspend (String) -> String?, // null = ok, string = error
    openSocket: (String, (CircleSocket?) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    var circles by remember { mutableStateOf<List<Circle>>(emptyList()) }
    var newName by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var watching by remember { mutableStateOf<String?>(null) }
    var socket by remember { mutableStateOf<CircleSocket?>(null) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { circles = load() }

    // Watch the first circle automatically, so presence is visible without a tap.
    LaunchedEffect(circles.firstOrNull()?.id) {
        val first = circles.firstOrNull()?.id
        if (first != null && watching != first) {
            socket?.close()
            watching = first
            openSocket(first) { socket = it }
        }
    }
    DisposableEffect(Unit) { onDispose { socket?.close() } }

    val presence by (socket?.state ?: remember { MutableStateFlow(CirclePresenceState()) })
        .collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back", color = White70, fontSize = 13.sp) }
            Spacer(Modifier.weight(1f))
            Text("Study Circles", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f)); Spacer(Modifier.width(48.dp))
        }

        Spacer(Modifier.height(16.dp))

        // ---- Live presence: the reason this app exists ----
        if (watching != null) {
            val live = presence.focusing
            Column(
                Modifier.fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(Color(0x3317C964), Color(0x1A8B5CF6))),
                        RoundedCornerShape(16.dp)
                    )
                    .border(1.dp, if (live.isEmpty()) White10 else Color(0x5517C964), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(8.dp).background(
                            if (presence.connected) Color(0xFF17C964) else Color(0xFF8A8A8A),
                            RoundedCornerShape(4.dp)
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            !presence.connected -> "Connecting…"
                            live.isEmpty() -> "Nobody studying right now"
                            live.size == 1 -> "1 person studying right now"
                            else -> "${live.size} people studying right now"
                        },
                        color = White90, fontSize = 14.sp
                    )
                }

                if (live.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    live.forEach { m ->
                        val leftMin = ((m.endsAt - System.currentTimeMillis()) / 60000L)
                            .coerceAtLeast(0L)
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔭", fontSize = 13.sp)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(m.cosmicName, color = White90, fontSize = 13.sp)
                                m.intention?.takeIf { it.isNotBlank() }?.let {
                                    Text("\"$it\"", color = White50, fontSize = 11.sp)
                                }
                            }
                            Text("${leftMin}m left", color = Color(0xFF17C964), fontSize = 11.sp)
                        }
                    }
                }

                if (presence.feed.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("RECENT", color = White50, fontSize = 9.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(4.dp))
                    presence.feed.take(4).forEach {
                        Text(it, color = White70, fontSize = 11.sp, modifier = Modifier.padding(vertical = 1.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Card("Join a circle") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = joinCode,
                    onValueChange = { joinCode = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(6) },
                    placeholder = { Text("6-char code", color = White30) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = fieldColors()
                )
                Spacer(Modifier.width(8.dp))
                GradientBtn("Join") {
                    scope.launch {
                        val err = join(joinCode)
                        msg = err ?: "Joined"
                        if (err == null) circles = load()
                        joinCode = ""
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card("Create a circle") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it.take(32) },
                    placeholder = { Text("e.g. IIT Prep Squad", color = White30) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = fieldColors()
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            if (newName.isNotBlank()) {
                                create(newName.trim())
                                newName = ""
                                circles = load()
                                msg = "Circle created"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = White10)
                ) { Text("Create", color = White90) }
            }
        }

        msg?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = White70, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        Spacer(Modifier.height(24.dp))
        Text("YOUR CIRCLES", color = White50, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        if (circles.isEmpty()) {
            Text("You haven't joined any circles yet.", color = White50, fontSize = 13.sp)
        }
        circles.forEach { c ->
            val isWatched = watching == c.id
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .background(White5, RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (isWatched) Violet500.copy(alpha = 0.6f) else White10,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        if (!isWatched) {
                            socket?.close(); socket = null
                            watching = c.id
                            openSocket(c.id) { socket = it }
                        }
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(c.name, color = White90, fontSize = 14.sp)
                    Text(
                        "Code: ${c.inviteCode ?: "—"} · ${c.memberCount} member" +
                            if (c.memberCount == 1) "" else "s",
                        color = White50, fontSize = 11.sp, letterSpacing = 1.sp
                    )
                }
                c.inviteCode?.let { code ->
                    Button(
                        onClick = { shareInvite(ctx, c.name, code) },
                        colors = ButtonDefaults.buttonColors(containerColor = White10),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) { Text("Invite", color = White90, fontSize = 12.sp) }
                }
            }
        }
    }
}

/** One-tap invite: a link friends can open from WhatsApp straight into the circle. */
private fun shareInvite(ctx: Context, circleName: String, code: String) {
    val link = "${BuildConfig.API_BASE.trimEnd('/')}/join/$code"
    val text = "Join my StudyGalaxy circle \"$circleName\" 🔭\n\n" +
        "We study together and you can see who's focusing live.\n" +
        "Tap to join: $link\n\n(or enter code $code in the app)"
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, "Join my StudyGalaxy circle")
    }
    ctx.startActivity(Intent.createChooser(send, "Invite friends").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(White5, RoundedCornerShape(16.dp))
            .border(1.dp, White10, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(title, color = White90, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun GradientBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .height(48.dp)
            .background(Brush.horizontalGradient(listOf(Violet500, Fuchsia500)), RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) {
            Text(label, color = White90, fontSize = 14.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = White90, unfocusedTextColor = White90,
    focusedBorderColor = Violet500, unfocusedBorderColor = White10,
    cursorColor = Violet500,
    focusedContainerColor = Color(0x0DFFFFFF), unfocusedContainerColor = Color(0x0DFFFFFF)
)
