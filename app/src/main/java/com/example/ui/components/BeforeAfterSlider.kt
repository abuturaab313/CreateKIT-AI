package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import kotlin.math.roundToInt

@Composable
fun BeforeAfterSlider(
    beforeBitmap: Bitmap,
    afterBitmap: Bitmap,
    modifier: Modifier = Modifier,
    initialSplit: Float = 0.5f,
    beforeLabel: String = "ORIGINAL",
    afterLabel: String = "AI ENHANCED"
) {
    var splitFraction by remember { mutableFloatStateOf(initialSplit) }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clipToBounds()
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val splitX = (widthPx * splitFraction).coerceIn(0f, widthPx)

        val beforeImage = remember(beforeBitmap) { beforeBitmap.asImageBitmap() }
        val afterImage = remember(afterBitmap) { afterBitmap.asImageBitmap() }

        // Render both images with clipping
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        splitFraction = ((splitX + dragAmount.x) / widthPx).coerceIn(0.05f, 0.95f)
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // 1. Draw "After" (Enhanced) Image on whole canvas
            drawImage(
                image = afterImage,
                dstSize = IntSize(canvasWidth.roundToInt(), canvasHeight.roundToInt())
            )

            // 2. Draw "Before" (Original) Image clipped to the left side
            clipRect(left = 0f, top = 0f, right = splitX, bottom = canvasHeight) {
                drawImage(
                    image = beforeImage,
                    dstSize = IntSize(canvasWidth.roundToInt(), canvasHeight.roundToInt())
                )
            }

            // 3. Draw vertical divider line
            drawLine(
                color = Color.White,
                start = androidx.compose.ui.geometry.Offset(splitX, 0f),
                end = androidx.compose.ui.geometry.Offset(splitX, canvasHeight),
                strokeWidth = 3.dp.toPx()
            )
        }

        // Circular drag handle at the divider center
        Surface(
            shape = CircleShape,
            color = ElectricCyan,
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(40.dp)
                .offset {
                    IntOffset(
                        x = (splitX - 20.dp.toPx()).roundToInt(),
                        y = (heightPx / 2f - 20.dp.toPx()).roundToInt()
                    )
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                    contentDescription = "Slide to compare",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Left Label (Original)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.65f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Text(
                text = beforeLabel,
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Right Label (Enhanced)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = ElectricCyan.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            Text(
                text = afterLabel,
                color = Color.Black,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
