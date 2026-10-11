package app.studygalaxy.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import kotlin.math.max

/** Pseudo-3D tilt that follows the pointer drag and springs back on release. */
fun Modifier.tiltOnDrag(maxTiltDeg: Float = 14f): Modifier = composed {
    var rx by remember { mutableStateOf(0f) }
    var ry by remember { mutableStateOf(0f) }
    val rxAnim by animateFloatAsState(rx, animationSpec = tween(250), label = "rx")
    val ryAnim by animateFloatAsState(ry, animationSpec = tween(250), label = "ry")
    var box by remember { mutableStateOf(IntSize.Zero) }

    this
        .onGloballyPositioned { box = it.size }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragEnd = { rx = 0f; ry = 0f },
                onDragCancel = { rx = 0f; ry = 0f }
            ) { change, _ ->
                val w = max(box.width, 1).toFloat()
                val h = max(box.height, 1).toFloat()
                val dx = (change.position.x / w - 0.5f) * 2f
                val dy = (change.position.y / h - 0.5f) * 2f
                ry = (dx * maxTiltDeg).coerceIn(-maxTiltDeg, maxTiltDeg)
                rx = (-dy * maxTiltDeg).coerceIn(-maxTiltDeg, maxTiltDeg)
                change.consume()
            }
        }
        .graphicsLayer {
            rotationX = rxAnim
            rotationY = ryAnim
            cameraDistance = 12f * density
        }
}
