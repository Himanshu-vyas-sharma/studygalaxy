package app.studygalaxy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.studygalaxy.util.gradientFromSeed
import app.studygalaxy.util.initialsOf

@Composable
fun Avatar(seed: String, name: String, size: Dp = 44.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(gradientFromSeed(seed))
            .border(2.dp, Color.White.copy(alpha = 0.1f), CircleShape)
    ) {
        Text(
            initialsOf(name),
            color = Color.White.copy(alpha = 0.95f),
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.4).sp
        )
    }
}
