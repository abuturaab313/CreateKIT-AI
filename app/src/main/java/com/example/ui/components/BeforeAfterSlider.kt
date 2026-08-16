package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricCyan
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun BeforeAfterSlider(
    beforeBitmap: Bitmap,
    afterBitmap: Bitmap,
    modifier: Modifier = Modifier,
    initialSplit: Float = 0.5f,
    beforeLabel: String = "ORIGINAL",
    afterLabel: String = "ENHANCED"
) {
    var splitFraction by remember { mutableFloatStateOf(initialSplit.coerceIn(0f, 1f)) }
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clipToBounds()
            .background(DarkSurface)
            .testTag("before_after_slider_container")
    ) {
        val containerWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val containerHeight = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val splitX = (containerWidth * splitFraction).coerceIn(0f, containerWidth)

        val beforeImage = remember(beforeBitmap) { beforeBitmap.asImageBitmap() }
        val afterImage = remember(afterBitmap) { afterBitmap.asImageBitmap() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        splitFraction = (tapOffset.x / containerWidth).coerceIn(0f, 1f)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        splitFraction = (change.position.x / containerWidth).coerceIn(0f, 1f)
                    }
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height

            // Calculate unified aspect-fit rectangle so both bitmaps stay perfectly aligned
            val aspect = beforeBitmap.width.toFloat() / max(1, beforeBitmap.height).toFloat()
            val canvasAspect = canvasW / max(1f, canvasH)

            val drawW: Float
            val drawH: Float
            if (canvasAspect > aspect) {
                drawH = canvasH
                drawW = canvasH * aspect
            } else {
                drawW = canvasW
                drawH = canvasW / aspect
            }

            val drawLeft = (canvasW - drawW) / 2f
            val drawTop = (canvasH - drawH) / 2f
            val dstOffset = IntOffset(drawLeft.roundToInt(), drawTop.roundToInt())
            val dstSize = IntSize(drawW.roundToInt(), drawH.roundToInt())

            // 1. Render Processed (After) Image over full fit rect
            drawImage(
                image = afterImage,
                dstOffset = dstOffset,
                dstSize = dstSize
            )

            // 2. Render Original (Before) Image clipped to the left of the split line
            clipRect(left = 0f, top = 0f, right = splitX, bottom = canvasH) {
                drawImage(
                    image = beforeImage,
                    dstOffset = dstOffset,
                    dstSize = dstSize
                )
            }

            // 3. Render High-contrast Split Line
            drawLine(
                color = Color.White,
                start = Offset(splitX, 0f),
                end = Offset(splitX, canvasH),
                strokeWidth = 2.5.dp.toPx()
            )
        }

        // Draggable Handle
        val handleSizePx = with(density) { 36.dp.toPx() }
        val handleX = (splitX - handleSizePx / 2f).roundToInt()
        val handleY = ((containerHeight - handleSizePx) / 2f).roundToInt()

        Surface(
            shape = CircleShape,
            color = ElectricCyan,
            shadowElevation = 8.dp,
            modifier = Modifier
                .size(36.dp)
                .offset { IntOffset(handleX, handleY) }
                .testTag("before_after_slider_handle")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                    contentDescription = "Slider Handle (${(splitFraction * 100).toInt()}%)",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Left Label (Original)
        if (splitFraction > 0.15f) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.7f),
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
        }

        // Right Label (Processed / Enhanced)
        if (splitFraction < 0.85f) {
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
}
