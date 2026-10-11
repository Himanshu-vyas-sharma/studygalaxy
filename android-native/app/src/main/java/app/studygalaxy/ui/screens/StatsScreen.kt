package app.studygalaxy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.studygalaxy.data.model.WeeklyStats
import app.studygalaxy.ui.theme.*

@Composable
fun StatsScreen(
    load: suspend () -> WeeklyStats?,
    onBack: () -> Unit
) {
    var data by remember { mutableStateOf<WeeklyStats?>(null) }
    LaunchedEffect(Unit) { data = load() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back", color = White70, fontSize = 13.sp) }
            Spacer(Modifier.weight(1f))
            Text("Weekly Stats", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f)); Spacer(Modifier.width(48.dp))
        }

        val d = data ?: return
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Week", "${d.weekTotalMin}m", Modifier.weight(1f))
            StatTile("Streak", "${d.streakDays}d", Modifier.weight(1f))
            StatTile("Level", "${d.level}", Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))
        Text("LAST 7 DAYS", color = White50, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp))
        val peak = d.heatmap.maxOfOrNull { it.min }?.coerceAtLeast(1) ?: 1
        Row(Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            d.heatmap.forEach { day ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.weight(1f - (day.min.toFloat() / peak)))
                    Box(
                        Modifier.fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Fuchsia400, Violet700)), RoundedCornerShape(6.dp))
                            .height(((day.min.toFloat() / peak) * 110f).dp.coerceAtLeast(4.dp))
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(day.date.takeLast(5), color = White50, fontSize = 9.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Column(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Violet500.copy(alpha = 0.2f), Fuchsia500.copy(alpha = 0.2f))), RoundedCornerShape(16.dp))
                .border(1.dp, White10, RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("${d.totalFocusMin / 60}h total",
                style = MaterialTheme.typography.displaySmall.copy(brush = gradientText()))
            Text("All focus you've ever logged in this galaxy.", color = White70, fontSize = 11.sp)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(White5, RoundedCornerShape(12.dp))
            .border(1.dp, White10, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label.uppercase(), color = White50, fontSize = 9.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge.copy(brush = gradientText()))
    }
}
