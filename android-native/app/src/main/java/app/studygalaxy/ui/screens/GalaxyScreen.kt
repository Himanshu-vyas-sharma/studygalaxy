package app.studygalaxy.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.withFrameNanos
import app.studygalaxy.data.model.Galaxy
import app.studygalaxy.ui.theme.*
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun GalaxyScreen(
    loadGalaxy: suspend () -> Galaxy?,
    onBack: () -> Unit
) {
    var data by remember { mutableStateOf<Galaxy?>(null) }
    var t by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) { data = loadGalaxy() }
    LaunchedEffect(Unit) {
        val start = System.nanoTime()
        while (isActive) withFrameNanos { t = ((it - start) / 1_000_000_000f) }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back", color = White70, fontSize = 13.sp) }
            Spacer(Modifier.weight(1f))
            Text("Your Galaxy", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(48.dp))
        }
        val d = data
        if (d == null) {
            Text("Loading…", color = White50, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Text(
                "${d.planets.size} planets · ${d.stars.size} stars · ${d.blackHoles.size} black holes",
                color = White50, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                val cx = w / 2; val cy = h / 2
                val orbitR = min(w, h) * 0.32f
                val pc = d.planets.size.coerceAtLeast(1)
                d.planets.forEachIndexed { i, p ->
                    val angle = (i.toFloat() / pc) * 2f * Math.PI.toFloat() + t * 0.05f
                    val px = cx + cos(angle) * orbitR
                    val py = cy + sin(angle) * orbitR
                    val r = 14f + min(p.totalMin / 20f, 32f)
                    val color = parseHex(p.color)
                    drawCircle(
                        brush = Brush.radialGradient(listOf(color.copy(alpha = 0.6f), Color.Transparent),
                            center = Offset(px, py), radius = r * 1.8f),
                        radius = r * 1.8f, center = Offset(px, py)
                    )
                    drawCircle(color = color, radius = r, center = Offset(px, py))
                }
                d.stars.forEachIndexed { idx, s ->
                    val pIdx = d.planets.indexOfFirst { it.id == s.subjectId }
                    val baseAngle = if (pIdx >= 0)
                        (pIdx.toFloat() / pc) * 2f * Math.PI.toFloat() + t * 0.05f
                    else (idx / 10f) * 2f * Math.PI.toFloat()
                    val px = cx + cos(baseAngle) * orbitR
                    val py = cy + sin(baseAngle) * orbitR
                    val off = (idx * 1.37f) % (2f * Math.PI.toFloat())
                    val dist = 40f + ((idx * 17) % 60)
                    val sx = px + cos(off) * dist
                    val sy = py + sin(off) * dist
                    val a = 0.4f + (s.rating / 5f) * 0.6f
                    val tw = 0.7f + 0.3f * sin(t * 2f + idx)
                    drawCircle(color = Color(1f, 0.96f, 0.78f, (a * tw).coerceIn(0f, 1f)),
                        radius = 2.5f, center = Offset(sx, sy))
                }
                d.blackHoles.forEachIndexed { idx, b ->
                    val pIdx = d.planets.indexOfFirst { it.id == b.subjectId }
                    val baseAngle = if (pIdx >= 0)
                        (pIdx.toFloat() / pc) * 2f * Math.PI.toFloat() + t * 0.05f
                    else 0f
                    val px = cx + cos(baseAngle) * orbitR
                    val py = cy + sin(baseAngle) * orbitR
                    val off = (idx * 2.1f) % (2f * Math.PI.toFloat())
                    val bx = px + cos(off) * 70f
                    val by = py + sin(off) * 70f
                    drawCircle(color = Color.Black, radius = 5f, center = Offset(bx, by))
                    drawCircle(color = Violet500.copy(alpha = 0.5f), radius = 7f,
                        center = Offset(bx, by), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
                }
            }
        }
    }
}

private fun parseHex(s: String): Color {
    val h = s.removePrefix("#").let { if (it.length == 6) "FF$it" else it }
    return Color(h.toLong(16))
}
