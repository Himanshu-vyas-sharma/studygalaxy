package app.studygalaxy.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import app.studygalaxy.util.PlanetStage
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
    // Float-specialised and read only inside the Canvas draw lambda, so each
    // frame invalidates drawing rather than recomposing the screen.
    val tState = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) { data = loadGalaxy() }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (isActive) withFrameNanos { tState.floatValue = (it - start) / 1_000_000_000f }
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
            Spacer(Modifier.height(10.dp))

            // Terraforming progress — what makes week three look unlike day one.
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                d.planets.sortedByDescending { it.totalMin }.take(4).forEach { p ->
                    val stage = PlanetStage.forMinutes(p.totalMin)
                    val toNext = PlanetStage.minutesToNext(p.totalMin)
                    val next = PlanetStage.next(p.totalMin)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(p.name, color = White90, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text(stage.label, color = parseHex(p.color), fontSize = 11.sp)
                    }
                    Text(
                        if (toNext != null && next != null)
                            "$toNext min to ${next.label}"
                        else "Fully terraformed 🌍",
                        color = White50, fontSize = 10.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Canvas(modifier = Modifier.fillMaxSize()) {
                val t = tState.floatValue
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
                    val stage = PlanetStage.forMinutes(p.totalMin)
                    val c = Offset(px, py)

                    // Rings sit behind the planet body.
                    if (stage.hasRings) {
                        val rx = r * 2.1f
                        val ry = r * 0.6f
                        drawOval(
                            color = color.copy(alpha = 0.45f),
                            topLeft = Offset(px - rx, py - ry),
                            size = Size(rx * 2, ry * 2),
                            style = Stroke(width = 2.5f)
                        )
                        drawOval(
                            color = Color.White.copy(alpha = 0.18f),
                            topLeft = Offset(px - rx * 0.82f, py - ry * 0.82f),
                            size = Size(rx * 1.64f, ry * 1.64f),
                            style = Stroke(width = 1.5f)
                        )
                    }

                    // Atmosphere halo.
                    if (stage.hasAtmosphere) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color.Transparent, color.copy(alpha = 0.55f)),
                                center = c, radius = r * 1.45f
                            ),
                            radius = r * 1.45f, center = c
                        )
                    }

                    // Ambient glow + body.
                    drawCircle(
                        brush = Brush.radialGradient(listOf(color.copy(alpha = 0.6f), Color.Transparent),
                            center = c, radius = r * 1.8f),
                        radius = r * 1.8f, center = c
                    )
                    drawCircle(color = color, radius = r, center = c)

                    // Oceans: darker blue caps across the surface.
                    if (stage.hasWater) {
                        drawCircle(
                            color = Color(0xFF2B6CB0).copy(alpha = 0.55f),
                            radius = r * 0.42f,
                            center = Offset(px - r * 0.33f, py - r * 0.22f)
                        )
                        drawCircle(
                            color = Color(0xFF2B6CB0).copy(alpha = 0.40f),
                            radius = r * 0.30f,
                            center = Offset(px + r * 0.36f, py + r * 0.30f)
                        )
                    }

                    // Life: green continents that pulse gently.
                    if (stage.hasLife) {
                        val pulse = 0.75f + 0.25f * sin(t * 1.4f + i)
                        drawCircle(
                            color = Color(0xFF34D399).copy(alpha = 0.6f * pulse),
                            radius = r * 0.26f,
                            center = Offset(px + r * 0.10f, py - r * 0.38f)
                        )
                        drawCircle(
                            color = Color(0xFF34D399).copy(alpha = 0.45f * pulse),
                            radius = r * 0.20f,
                            center = Offset(px - r * 0.40f, py + r * 0.34f)
                        )
                    }

                    // Moons orbit at their own rate.
                    repeat(stage.moons) { m ->
                        val ma = t * (0.6f + m * 0.25f) + m * 2.3f
                        val md = r * (1.9f + m * 0.45f)
                        drawCircle(
                            color = Color(0xFFCBD5E1),
                            radius = 2.6f + m * 0.4f,
                            center = Offset(px + cos(ma) * md, py + sin(ma) * md * 0.55f)
                        )
                    }
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
