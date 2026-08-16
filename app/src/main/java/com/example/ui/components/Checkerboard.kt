package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Renders an authentic transparency checkerboard grid behind transparent images.
 */
@Composable
fun CheckerboardBackground(
    modifier: Modifier = Modifier,
    squareSize: Dp = 12.dp,
    lightColor: Color = Color(0xFF2C303E),
    darkColor: Color = Color(0xFF1E222D),
    content: @Composable () -> Unit = {}
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sizePx = squareSize.toPx()
            val cols = (size.width / sizePx).toInt() + 1
            val rows = (size.height / sizePx).toInt() + 1

            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val color = if ((r + c) % 2 == 0) lightColor else darkColor
                    drawRect(
                        color = color,
                        topLeft = Offset(c * sizePx, r * sizePx),
                        size = Size(sizePx, sizePx)
                    )
                }
            }
        }
        content()
    }
}
