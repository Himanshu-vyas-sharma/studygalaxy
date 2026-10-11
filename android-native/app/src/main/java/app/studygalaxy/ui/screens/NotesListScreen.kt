package app.studygalaxy.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.studygalaxy.data.model.Note
import app.studygalaxy.data.model.StrokeDto
import app.studygalaxy.ui.theme.*

@Composable
fun NotesListScreen(
    load: suspend () -> List<Note>,
    onOpen: (Note) -> Unit,
    onCreate: () -> Unit,
    onBack: () -> Unit
) {
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
    LaunchedEffect(Unit) { notes = load() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Back", color = White70, fontSize = 13.sp) }
                Spacer(Modifier.weight(1f))
                Text("Notes", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }
            Spacer(Modifier.height(10.dp))
            if (notes.isEmpty()) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("No notes yet.", color = White70)
                    Spacer(Modifier.height(4.dp))
                    Text("Tap + to sketch your first one.", color = White50, fontSize = 12.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notes) { n -> NoteThumb(n) { onOpen(n) } }
                }
            }
        }
        // FAB
        FloatingActionButton(
            onClick = onCreate,
            containerColor = Violet500,
            contentColor = White90,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) { Text("+", fontSize = 24.sp) }
    }
}

@Composable
private fun NoteThumb(note: Note, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(White5)
            .border(1.dp, White10, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF140833), Color(0xFF05020D))))
        ) {
            MiniStrokes(note.strokes)
        }
        Spacer(Modifier.height(8.dp))
        Text(if (note.title.isBlank()) "Untitled note" else note.title,
            color = White90, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
private fun MiniStrokes(strokes: List<StrokeDto>) {
    if (strokes.isEmpty()) return
    val all = strokes.flatMap { it.points }
    if (all.isEmpty()) return
    val minX = all.minOf { it[0] }; val maxX = all.maxOf { it[0] }
    val minY = all.minOf { it[1] }; val maxY = all.maxOf { it[1] }
    val dx = (maxX - minX).coerceAtLeast(1f); val dy = (maxY - minY).coerceAtLeast(1f)

    Canvas(Modifier.fillMaxSize()) {
        strokes.forEach { s ->
            val pts = s.points
            if (pts.size < 2) return@forEach
            val color = parseHex(s.color)
            for (i in 1 until pts.size) {
                val p1 = pts[i - 1]; val p2 = pts[i]
                val x1 = ((p1[0] - minX) / dx) * size.width
                val y1 = ((p1[1] - minY) / dy) * size.height
                val x2 = ((p2[0] - minX) / dx) * size.width
                val y2 = ((p2[1] - minY) / dy) * size.height
                drawLine(
                    color = if (s.erase) Color.Black else color,
                    start = Offset(x1, y1), end = Offset(x2, y2),
                    strokeWidth = (s.width * 0.3f).coerceAtLeast(1f),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun parseHex(hex: String): Color {
    val clean = hex.removePrefix("#").let { if (it.length == 6) "FF$it" else it }
    return Color(clean.toLong(16))
}
