package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

data class StrokePath(
    val points: List<Offset>,
    val strokeWidth: Float
)

@Composable
fun BrushCanvas(
    baseBitmap: Bitmap,
    brushRadius: Float,
    strokes: List<StrokePath>,
    onAddStroke: (StrokePath) -> Unit,
    modifier: Modifier = Modifier,
    onMaskUpdated: ((Bitmap) -> Unit)? = null
) {
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val imageBitmap = remember(baseBitmap) { baseBitmap.asImageBitmap() }

        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(brushRadius) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPoints = listOf(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPoints = currentPoints + change.position
                        },
                        onDragEnd = {
                            if (currentPoints.isNotEmpty()) {
                                onAddStroke(StrokePath(currentPoints, brushRadius))
                                currentPoints = emptyList()
                            }
                        },
                        onDragCancel = {
                            currentPoints = emptyList()
                        }
                    )
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Draw base photo
            drawImage(
                image = imageBitmap,
                dstSize = IntSize(canvasW.roundToInt(), canvasH.roundToInt())
            )

            // 2. Draw all finalized strokes
            for (stroke in strokes) {
                if (stroke.points.size < 2) {
                    if (stroke.points.isNotEmpty()) {
                        drawCircle(
                            color = Color(0x99FF0055),
                            radius = stroke.strokeWidth / 2f,
                            center = stroke.points[0]
                        )
                    }
                    continue
                }
                for (i in 0 until stroke.points.size - 1) {
                    drawLine(
                        color = Color(0x99FF0055),
                        start = stroke.points[i],
                        end = stroke.points[i + 1],
                        strokeWidth = stroke.strokeWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }

            // 3. Draw active stroke
            if (currentPoints.isNotEmpty()) {
                for (i in 0 until currentPoints.size - 1) {
                    drawLine(
                        color = Color(0xBBFF0055),
                        start = currentPoints[i],
                        end = currentPoints[i + 1],
                        strokeWidth = brushRadius,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }
    }
}

fun generateMaskBitmap(
    width: Int,
    height: Int,
    viewWidth: Float,
    viewHeight: Float,
    strokes: List<StrokePath>
): Bitmap {
    val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(mask)
    canvas.drawColor(android.graphics.Color.TRANSPARENT)

    val scaleX = width / viewWidth.coerceAtLeast(1f)
    val scaleY = height / viewHeight.coerceAtLeast(1f)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    for (stroke in strokes) {
        paint.strokeWidth = stroke.strokeWidth * ((scaleX + scaleY) / 2f)
        val path = Path()
        if (stroke.points.isNotEmpty()) {
            path.moveTo(stroke.points[0].x * scaleX, stroke.points[0].y * scaleY)
            for (i in 1 until stroke.points.size) {
                path.lineTo(stroke.points[i].x * scaleX, stroke.points[i].y * scaleY)
            }
            canvas.drawPath(path, paint)
        }
    }

    return mask
}
