package app.studygalaxy.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.studygalaxy.data.model.Planet
import app.studygalaxy.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSetupScreen(
    subject: Planet?,
    onPickSubject: () -> Unit,
    onBack: () -> Unit,
    onStart: (plannedMin: Int, intention: String, todos: List<String>) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val durations = listOf(25, 45, 90)
    var chosen by remember { mutableStateOf(25) }
    var intention by remember { mutableStateOf("") }
    val todos = remember { mutableStateListOf("", "", "") }
    val readyTodos = todos.map { it.trim() }.filter { it.isNotEmpty() }
    val canStart = subject != null && readyTodos.isNotEmpty()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).padding(top = 16.dp)
    ) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text("← Back", color = White70, fontSize = 13.sp) }

        Spacer(Modifier.height(16.dp))
        Text("Ready to focus?", style = MaterialTheme.typography.displaySmall.copy(brush = gradientText()))
        Spacer(Modifier.height(8.dp))
        Text("Set your subject, duration, and the handful of things you'll crush this session.",
            color = White70, fontSize = 13.sp, lineHeight = 19.sp)

        Spacer(Modifier.height(24.dp))

        // Subject
        Column {
            Text("SUBJECT", color = White50, fontSize = 10.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(White5, RoundedCornerShape(14.dp))
                    .border(1.dp, if (subject != null) Violet500 else Color(0x55FF4D5A), RoundedCornerShape(14.dp))
                    .clickable(onClick = onPickSubject)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (subject != null) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(parseHex(subject.color)))
                    Spacer(Modifier.width(10.dp))
                    Text(subject.name, color = White90, fontSize = 15.sp, modifier = Modifier.weight(1f))
                } else {
                    Text("No subject chosen — tap to pick", color = Color(0xFFFCA5A5), fontSize = 13.sp, modifier = Modifier.weight(1f))
                }
                Text("›", color = White50, fontSize = 20.sp)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Duration
        Column {
            Text("DURATION", color = White50, fontSize = 10.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                durations.forEach { m ->
                    val sel = m == chosen
                    val scale by animateFloatAsState(if (sel) 1f else 0.95f, label = "ps")
                    Button(
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); chosen = m },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sel) Color.Transparent else White5
                        ),
                        border = BorderStroke(1.dp, if (sel) Violet500 else White10),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        if (sel) {
                            Box(
                                Modifier.fillMaxSize().background(
                                    Brush.horizontalGradient(listOf(Violet500.copy(alpha = 0.35f), Fuchsia500.copy(alpha = 0.2f))),
                                    RoundedCornerShape(14.dp)
                                ),
                                contentAlignment = Alignment.Center
                            ) { Text("${m}m", color = White90, fontSize = 16.sp) }
                        } else {
                            Text("${m}m", color = White90, fontSize = 15.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Intention
        Column {
            Text("INTENTION (OPTIONAL)", color = White50, fontSize = 10.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = intention,
                onValueChange = { intention = it.take(140) },
                placeholder = { Text("Finish chapter 4 derivations", color = White30) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )
        }

        Spacer(Modifier.height(20.dp))

        // To-do list
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("WHAT WILL YOU CRUSH?", color = White50, fontSize = 10.sp, letterSpacing = 2.sp)
                Spacer(Modifier.weight(1f))
                if (todos.size < 5) {
                    TextButton(onClick = { todos.add("") }) { Text("+ add", color = Violet300, fontSize = 11.sp) }
                }
            }
            Spacer(Modifier.height(4.dp))
            todos.forEachIndexed { i, t ->
                OutlinedTextField(
                    value = t,
                    onValueChange = { todos[i] = it.take(100) },
                    placeholder = { Text("todo ${i + 1}", color = White30) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = fieldColors(),
                    leadingIcon = {
                        Box(Modifier.size(16.dp).clip(CircleShape).border(1.5.dp, White30, CircleShape))
                    },
                    trailingIcon = if (todos.size > 1) {{
                        TextButton(onClick = { todos.removeAt(i) }) { Text("×", color = White50) }
                    }} else null
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = {
                if (canStart) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStart(chosen, intention, readyTodos)
                }
            },
            enabled = canStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                Modifier.fillMaxSize().background(
                    if (canStart) Brush.horizontalGradient(listOf(Violet500, Fuchsia500))
                    else Brush.horizontalGradient(listOf(White10, White10)),
                    RoundedCornerShape(16.dp)
                ),
                contentAlignment = Alignment.Center
            ) { Text(if (canStart) "Enter the portal" else "Pick a subject and 1 todo", color = White90) }
        }
        Spacer(Modifier.height(16.dp))
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

private fun parseHex(hex: String): Color {
    val clean = hex.removePrefix("#").let { if (it.length == 6) "FF$it" else it }
    return Color(clean.toLong(16))
}
