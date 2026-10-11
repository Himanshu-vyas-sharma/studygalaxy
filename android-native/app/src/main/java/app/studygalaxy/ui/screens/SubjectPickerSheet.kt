package app.studygalaxy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.studygalaxy.data.model.Planet
import app.studygalaxy.ui.theme.*
import kotlinx.coroutines.launch

private val PRESETS = listOf(
    "Physics" to "#60A5FA",
    "Chemistry" to "#34D399",
    "Math" to "#F472B6",
    "Biology" to "#A78BFA",
    "English" to "#FBBF24",
    "History" to "#F97316",
    "Computer Science" to "#22D3EE",
    "Economics" to "#F43F5E"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectPickerScreen(
    subjects: List<Planet>,
    onPick: (String) -> Unit,
    onCreate: suspend (name: String, color: String) -> Planet?,
    onBack: () -> Unit
) {
    var customName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(24.dp).padding(top = 16.dp)) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text("← Back", color = White70, fontSize = 13.sp) }
        Spacer(Modifier.height(14.dp))
        Text("Pick a subject", style = MaterialTheme.typography.displaySmall.copy(brush = gradientText()))
        Spacer(Modifier.height(8.dp))
        Text("Your focus session will grow this planet.", color = White70, fontSize = 13.sp)

        Spacer(Modifier.height(20.dp))

        if (subjects.isNotEmpty()) {
            Text("YOUR PLANETS", color = White50, fontSize = 10.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            subjects.forEach { s ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .background(White5, RoundedCornerShape(14.dp))
                        .border(1.dp, White10, RoundedCornerShape(14.dp))
                        .clickable { onPick(s.id) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(parseHex(s.color)))
                    Spacer(Modifier.width(10.dp))
                    Text(s.name, color = White90, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text("${s.totalMin}m logged", color = White50, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Text("CREATE A NEW PLANET", color = White50, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customName,
                onValueChange = { customName = it.take(32) },
                placeholder = { Text("Custom (e.g. SAT prep)", color = White30) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = White90, unfocusedTextColor = White90,
                    focusedBorderColor = Violet500, unfocusedBorderColor = White10,
                    cursorColor = Violet500,
                    focusedContainerColor = Color(0x0DFFFFFF), unfocusedContainerColor = Color(0x0DFFFFFF)
                )
            )
            Button(
                onClick = {
                    val name = customName.trim(); if (name.isBlank()) return@Button
                    scope.launch {
                        val color = PRESETS[name.hashCode().mod(PRESETS.size)].second
                        val created = onCreate(name, color)
                        if (created != null) onPick(created.id)
                    }
                },
                enabled = customName.trim().isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Violet500)
            ) { Text("Add", color = White90) }
        }

        Text("Or pick a preset:", color = White50, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        @OptIn(ExperimentalLayoutApi::class)
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PRESETS.forEach { (name, color) ->
                SuggestionChip(
                    onClick = {
                        scope.launch {
                            val created = onCreate(name, color)
                            if (created != null) onPick(created.id)
                        }
                    },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(parseHex(color)))
                            Spacer(Modifier.width(6.dp))
                            Text(name, color = White90, fontSize = 12.sp)
                        }
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = White5)
                )
            }
        }
    }
}

private fun parseHex(hex: String): Color {
    val clean = hex.removePrefix("#").let { if (it.length == 6) "FF$it" else it }
    return Color(clean.toLong(16))
}

