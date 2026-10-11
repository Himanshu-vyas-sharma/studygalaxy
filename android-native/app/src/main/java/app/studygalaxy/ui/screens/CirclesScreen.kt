package app.studygalaxy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    onBack: () -> Unit
) {
    var circles by remember { mutableStateOf<List<Circle>>(emptyList()) }
    var newName by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { circles = load() }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back", color = White70, fontSize = 13.sp) }
            Spacer(Modifier.weight(1f))
            Text("Study Circles", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f)); Spacer(Modifier.width(48.dp))
        }

        Spacer(Modifier.height(24.dp))

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
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .background(White5, RoundedCornerShape(12.dp))
                    .border(1.dp, White10, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(c.name, color = White90, fontSize = 14.sp)
                    Text("Code: ${c.inviteCode ?: "—"}", color = White50, fontSize = 11.sp, letterSpacing = 2.sp)
                }
            }
        }
    }
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
