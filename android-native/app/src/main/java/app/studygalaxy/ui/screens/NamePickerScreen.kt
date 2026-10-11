package app.studygalaxy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.studygalaxy.ui.components.Avatar
import app.studygalaxy.ui.theme.*
import kotlinx.coroutines.launch

private val SUGGESTIONS = listOf(
    "NebulaNinja", "QuasarQueen", "AstroArjun", "StellarRider",
    "CometHunter", "NovaSpark", "OrbitSage", "LunarPilot"
)

@Composable
fun NamePickerScreen(
    initialName: String,
    avatarSeed: String,
    onSave: suspend (String) -> Boolean,
    onDone: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to", color = Violet300.copy(alpha = 0.7f), letterSpacing = 6.sp, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        Text("StudyGalaxy", style = MaterialTheme.typography.displayMedium.copy(brush = gradientText()))
        Spacer(Modifier.height(16.dp))
        Text(
            "Pick your cosmic name. This is who your friends will see when you focus.",
            color = White70, fontSize = 13.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Avatar(seed = avatarSeed, name = name.ifBlank { "Star" }, size = 96.dp)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it.filter { c -> c.isLetterOrDigit() || c == ' ' }.take(24) },
            placeholder = { Text("Your star name", color = White30) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White90, unfocusedTextColor = White90,
                focusedBorderColor = Violet500, unfocusedBorderColor = White10,
                cursorColor = Violet500,
                focusedContainerColor = Color(0x0DFFFFFF), unfocusedContainerColor = Color(0x0DFFFFFF)
            )
        )

        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SUGGESTIONS.forEach { s ->
                SuggestionChip(onClick = { name = s }, label = { Text(s, fontSize = 11.sp) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = White5, labelColor = White90
                    )
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                saving = true
                scope.launch { if (onSave(name.trim())) onDone(); saving = false }
            },
            enabled = !saving && name.trim().length >= 2,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(listOf(Violet500, Fuchsia500)),
                    RoundedCornerShape(16.dp)
                ),
                contentAlignment = Alignment.Center
            ) { Text(if (saving) "Charting course…" else "Enter the galaxy", color = White90) }
        }
    }
}

// FlowRow for compose material3 wraps availability
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    horizontalArrangement: Arrangement.Horizontal,
    verticalArrangement: Arrangement.Vertical,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) { content() }
}
