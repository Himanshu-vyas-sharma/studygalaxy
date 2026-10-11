package app.studygalaxy.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.studygalaxy.data.model.StrokeDto
import app.studygalaxy.ui.theme.*
import kotlinx.coroutines.launch

private data class LocalStroke(
    val color: String,
    val width: Float,
    val erase: Boolean,
    val points: MutableList<Offset>
)

private val PALETTE = listOf(
    "#FFFFFF", "#FBBF24", "#F472B6", "#A78BFA",
    "#60A5FA", "#34D399", "#F97316", "#F43F5E"
)
private val WIDTHS = listOf(3f, 7f, 14f)

@Composable
fun NoteCanvasScreen(
    initialStrokes: List<StrokeDto>,
    initialTitle: String,
    onSave: suspend (title: String, strokes: List<StrokeDto>) -> Boolean,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val strokes = remember {
        mutableStateListOf<LocalStroke>().also {
            it.addAll(initialStrokes.map { s ->
                LocalStroke(s.color, s.width, s.erase, s.points.map { p -> Offset(p[0], p[1]) }.toMutableList())
            })
        }
    }
    val undone = remember { mutableStateListOf<LocalStroke>() }
    var color by remember { mutableStateOf(PALETTE[0]) }
    var width by remember { mutableStateOf(WIDTHS[0]) }
    var erase by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(initialTitle) }
    var current by remember { mutableStateOf<LocalStroke?>(null) }
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text("← Back", color = White70, fontSize = 13.sp) }
            Spacer(Modifier.weight(1f))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(80) },
                placeholder = { Text("Title", color = White30) },
                singleLine = true,
                modifier = Modifier.weight(3f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = White90, unfocusedTextColor = White90,
                    focusedBorderColor = Violet500, unfocusedBorderColor = White10,
                    cursorColor = Violet500,
                    focusedContainerColor = Color(0x0DFFFFFF), unfocusedContainerColor = Color(0x0DFFFFFF)
                )
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    saving = true
                    scope.launch {
                        val payload = strokes.map { s ->
                            StrokeDto(s.color, s.width, s.erase, s.points.map { listOf(it.x, it.y) })
                        }
                        if (onSave(title, payload)) onBack()
                        saving = false
                    }
                },
                enabled = !saving,
                colors = ButtonDefaults.buttonColors(containerColor = Violet500)
            ) { Text(if (saving) "Saving…" else "Save", color = White90, fontSize = 13.sp) }
        }

        // Canvas
        Box(
            Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF140833), Color(0xFF05020D))))
                .border(1.dp, White10, RoundedCornerShape(14.dp))
                .pointerInput(erase, color, width) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            val s = LocalStroke(color, width, erase, mutableListOf(pos))
                            current = s
                            strokes.add(s)
                            undone.clear()
                        },
                        onDrag = { change, _ ->
                            current?.points?.add(change.position)
                            change.consume()
                        },
                        onDragEnd = { current = null },
                        onDragCancel = { current = null }
                    )
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                strokes.forEach { s ->
                    if (s.points.size < 2) return@forEach
                    val path = Path().apply {
                        val p0 = s.points.first()
                        moveTo(p0.x, p0.y)
                        for (i in 1 until s.points.size) {
                            val p = s.points[i]
                            lineTo(p.x, p.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = if (s.erase) Color.Black else parseHex(s.color),
                        style = Stroke(width = s.width, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }

        // Toolbar
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Color row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PALETTE.forEach { hex ->
                    val selected = !erase && color == hex
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(parseHex(hex))
                            .border(if (selected) 3.dp else 1.dp, if (selected) White90 else White30, CircleShape)
                            .clickable { color = hex; erase = false; haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            // Width + tools row
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WIDTHS.forEach { w ->
                    val selected = width == w
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) White10 else White5)
                            .border(1.dp, if (selected) Violet500 else White10, RoundedCornerShape(10.dp))
                            .clickable { width = w },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(w.dp).clip(CircleShape).background(White90))
                    }
                }
                Spacer(Modifier.weight(1f))
                ToolBtn(selected = erase, label = "Erase") { erase = !erase }
                ToolBtn(selected = false, label = "Undo") {
                    if (strokes.isNotEmpty()) undone.add(strokes.removeAt(strokes.lastIndex))
                }
                ToolBtn(selected = false, label = "Redo") {
                    if (undone.isNotEmpty()) strokes.add(undone.removeAt(undone.lastIndex))
                }
                ToolBtn(selected = false, label = "Clear") { strokes.clear(); undone.clear() }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ToolBtn(selected: Boolean, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .height(40.dp)
            .background(if (selected) Violet500.copy(alpha = 0.3f) else White5, RoundedCornerShape(10.dp))
            .border(1.dp, if (selected) Violet500 else White10, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = White90, fontSize = 11.sp) }
}

private fun parseHex(hex: String): Color {
    val clean = hex.removePrefix("#").let { if (it.length == 6) "FF$it" else it }
    return Color(clean.toLong(16))
}

