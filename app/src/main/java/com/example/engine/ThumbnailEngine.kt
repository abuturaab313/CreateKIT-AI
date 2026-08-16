package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream

enum class ThumbnailCategory(val label: String, val badgeColor: Long) {
    GAMING("Gaming", 0xFFFF0055),
    TECH("Tech", 0xFF00E5FF),
    AI("AI & Future", 0xFF8A2BE2),
    EDUCATION("Education", 0xFF00E676),
    VLOG("Vlog & Lifestyle", 0xFFFFB703),
    NEWS("News & Commentary", 0xFFFF5757),
    SHORTS("Shorts & Reels", 0xFFE040FB)
}

enum class ShapeType {
    RECTANGLE,
    ROUNDED_RECT,
    CIRCLE,
    BADGE_PILL,
    STAR_BURST
}

data class TextLayer(
    val id: String = java.util.UUID.randomUUID().toString(),
    var text: String = "EPIC TITLE HERE",
    var x: Float = 640f,
    var y: Float = 360f,
    var fontSize: Float = 72f,
    var textColor: Int = Color.WHITE,
    var strokeColor: Int = Color.BLACK,
    var strokeWidth: Float = 8f,
    var shadowColor: Int = Color.BLACK,
    var shadowRadius: Float = 14f,
    var isBold: Boolean = true,
    var isUppercase: Boolean = true,
    var hasGradient: Boolean = true,
    var gradientStartColor: Int = 0xFFFFD700.toInt(), // Gold
    var gradientEndColor: Int = 0xFFFF4500.toInt(),   // Orange red
    var hasBackgroundBadge: Boolean = true,
    var badgeColor: Int = 0xCC000000.toInt()
)

data class StickerLayer(
    val id: String = java.util.UUID.randomUUID().toString(),
    var emojiOrText: String = "🔥",
    var x: Float = 1000f,
    var y: Float = 200f,
    var size: Float = 120f,
    var rotationDeg: Float = -12f
)

data class ShapeLayer(
    val id: String = java.util.UUID.randomUUID().toString(),
    var shapeType: ShapeType = ShapeType.ROUNDED_RECT,
    var x: Float = 640f,
    var y: Float = 580f,
    var width: Float = 400f,
    var height: Float = 80f,
    var color: Int = 0xFFFF0055.toInt(),
    var label: String = "WATCH NOW ➔",
    var textColor: Int = Color.WHITE
)

data class ThumbnailTemplate(
    val id: String,
    val title: String,
    val category: ThumbnailCategory,
    val bgGradientStart: Int,
    val bgGradientEnd: Int,
    val defaultTexts: List<TextLayer>,
    val defaultStickers: List<StickerLayer>,
    val defaultShapes: List<ShapeLayer>
)

object ThumbnailEngine {
    const val CANVAS_WIDTH = 1280
    const val CANVAS_HEIGHT = 720

    val TEMPLATES = listOf(
        ThumbnailTemplate(
            id = "gaming_beast",
            title = "Insane Gameplay",
            category = ThumbnailCategory.GAMING,
            bgGradientStart = 0xFF0D0B2E.toInt(),
            bgGradientEnd = 0xFFFF0055.toInt(),
            defaultTexts = listOf(
                TextLayer(
                    text = "UNSTOPPABLE!",
                    x = 640f,
                    y = 260f,
                    fontSize = 88f,
                    gradientStartColor = 0xFF00FFFF.toInt(),
                    gradientEndColor = 0xFF8A2BE2.toInt()
                ),
                TextLayer(
                    text = "100 KILLS RECORD",
                    x = 640f,
                    y = 400f,
                    fontSize = 62f,
                    gradientStartColor = 0xFFFFD700.toInt(),
                    gradientEndColor = 0xFFFF3366.toInt()
                )
            ),
            defaultStickers = listOf(
                StickerLayer(emojiOrText = "👑", x = 1100f, y = 160f, size = 110f, rotationDeg = 15f),
                StickerLayer(emojiOrText = "💥", x = 180f, y = 220f, size = 120f, rotationDeg = -10f)
            ),
            defaultShapes = listOf(
                ShapeLayer(
                    shapeType = ShapeType.BADGE_PILL,
                    x = 640f,
                    y = 560f,
                    width = 460f,
                    height = 90f,
                    color = 0xFFFF0055.toInt(),
                    label = "SEASON FINALE 🏆"
                )
            )
        ),
        ThumbnailTemplate(
            id = "ai_future",
            title = "AI Revolution",
            category = ThumbnailCategory.AI,
            bgGradientStart = 0xFF030712.toInt(),
            bgGradientEnd = 0xFF1E1B4B.toInt(),
            defaultTexts = listOf(
                TextLayer(
                    text = "THE NEW AI MODEL",
                    x = 640f,
                    y = 250f,
                    fontSize = 76f,
                    gradientStartColor = 0xFF38BDF8.toInt(),
                    gradientEndColor = 0xFF818CF8.toInt()
                ),
                TextLayer(
                    text = "CHANGES EVERYTHING",
                    x = 640f,
                    y = 380f,
                    fontSize = 68f,
                    gradientStartColor = 0xFFF43F5E.toInt(),
                    gradientEndColor = 0xFFFB923C.toInt()
                )
            ),
            defaultStickers = listOf(
                StickerLayer(emojiOrText = "🤖", x = 1120f, y = 180f, size = 110f, rotationDeg = 12f),
                StickerLayer(emojiOrText = "⚡", x = 160f, y = 200f, size = 100f, rotationDeg = -15f)
            ),
            defaultShapes = listOf(
                ShapeLayer(
                    shapeType = ShapeType.ROUNDED_RECT,
                    x = 640f,
                    y = 540f,
                    width = 420f,
                    height = 80f,
                    color = 0xFF8A2BE2.toInt(),
                    label = "2026 BENCHMARK ⚡"
                )
            )
        ),
        ThumbnailTemplate(
            id = "tech_review",
            title = "Tech Review",
            category = ThumbnailCategory.TECH,
            bgGradientStart = 0xFF0A0E1A.toInt(),
            bgGradientEnd = 0xFF003B46.toInt(),
            defaultTexts = listOf(
                TextLayer(
                    text = "DON'T BUY THIS...",
                    x = 640f,
                    y = 260f,
                    fontSize = 80f,
                    gradientStartColor = 0xFF00E5FF.toInt(),
                    gradientEndColor = 0xFF00FF87.toInt()
                ),
                TextLayer(
                    text = "UNTIL YOU SEE THIS!",
                    x = 640f,
                    y = 400f,
                    fontSize = 64f,
                    gradientStartColor = 0xFFFFEE00.toInt(),
                    gradientEndColor = 0xFFFF6B00.toInt()
                )
            ),
            defaultStickers = listOf(
                StickerLayer(emojiOrText = "📱", x = 1120f, y = 200f, size = 110f, rotationDeg = 10f),
                StickerLayer(emojiOrText = "⚠️", x = 180f, y = 180f, size = 110f, rotationDeg = -12f)
            ),
            defaultShapes = listOf(
                ShapeLayer(
                    shapeType = ShapeType.BADGE_PILL,
                    x = 640f,
                    y = 550f,
                    width = 440f,
                    height = 84f,
                    color = 0xFF00E5FF.toInt(),
                    label = "HONEST VERDICT 🔍",
                    textColor = Color.BLACK
                )
            )
        ),
        ThumbnailTemplate(
            id = "vlog_lifestyle",
            title = "Vlog & Life",
            category = ThumbnailCategory.VLOG,
            bgGradientStart = 0xFF1F1135.toInt(),
            bgGradientEnd = 0xFF581C87.toInt(),
            defaultTexts = listOf(
                TextLayer(
                    text = "A DAY IN MY LIFE",
                    x = 640f,
                    y = 270f,
                    fontSize = 78f,
                    gradientStartColor = 0xFFFFA07A.toInt(),
                    gradientEndColor = 0xFFFFD700.toInt()
                ),
                TextLayer(
                    text = "TOKYO, JAPAN",
                    x = 640f,
                    y = 400f,
                    fontSize = 60f,
                    gradientStartColor = 0xFFF472B6.toInt(),
                    gradientEndColor = 0xFFC084FC.toInt()
                )
            ),
            defaultStickers = listOf(
                StickerLayer(emojiOrText = "✈️", x = 1100f, y = 180f, size = 110f, rotationDeg = 20f),
                StickerLayer(emojiOrText = "✨", x = 180f, y = 220f, size = 100f, rotationDeg = 0f)
            ),
            defaultShapes = listOf(
                ShapeLayer(
                    shapeType = ShapeType.ROUNDED_RECT,
                    x = 640f,
                    y = 550f,
                    width = 400f,
                    height = 80f,
                    color = 0xFFFFB703.toInt(),
                    label = "FULL TRAVEL VLOG 🎥",
                    textColor = Color.BLACK
                )
            )
        ),
        ThumbnailTemplate(
            id = "news_breaking",
            title = "Breaking Alert",
            category = ThumbnailCategory.NEWS,
            bgGradientStart = 0xFF1A0000.toInt(),
            bgGradientEnd = 0xFF800000.toInt(),
            defaultTexts = listOf(
                TextLayer(
                    text = "BREAKING NEWS!",
                    x = 640f,
                    y = 250f,
                    fontSize = 86f,
                    gradientStartColor = 0xFFFF3333.toInt(),
                    gradientEndColor = 0xFFFFCC00.toInt()
                ),
                TextLayer(
                    text = "IT ACTUALLY HAPPENED",
                    x = 640f,
                    y = 390f,
                    fontSize = 62f,
                    gradientStartColor = 0xFFFFFFFF.toInt(),
                    gradientEndColor = 0xFFCCCCCC.toInt()
                )
            ),
            defaultStickers = listOf(
                StickerLayer(emojiOrText = "🚨", x = 1120f, y = 180f, size = 120f, rotationDeg = 15f),
                StickerLayer(emojiOrText = "🔴", x = 180f, y = 180f, size = 90f, rotationDeg = 0f)
            ),
            defaultShapes = listOf(
                ShapeLayer(
                    shapeType = ShapeType.BADGE_PILL,
                    x = 640f,
                    y = 540f,
                    width = 440f,
                    height = 84f,
                    color = 0xFFFF0000.toInt(),
                    label = "LIVE COVERAGE 🔴"
                )
            )
        )
    )

    fun renderThumbnail(
        bgBitmap: Bitmap?,
        bgGradientStart: Int,
        bgGradientEnd: Int,
        textLayers: List<TextLayer>,
        stickers: List<StickerLayer>,
        shapes: List<ShapeLayer>,
        outputFile: File
    ): Long {
        val result = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // 1. Draw Background
        if (bgBitmap != null) {
            val srcRect = Rect(0, 0, bgBitmap.width, bgBitmap.height)
            val dstRect = Rect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(bgBitmap, srcRect, dstRect, paint)

            // Dark vignette overlay for text readability
            val vignettePaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, CANVAS_HEIGHT.toFloat(),
                    0x88000000.toInt(), 0xCC000000.toInt(),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(), vignettePaint)
        } else {
            val bgPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(),
                    bgGradientStart, bgGradientEnd,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(), bgPaint)
        }

        // 2. Draw Shapes
        for (shape in shapes) {
            val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = shape.color
                style = Paint.Style.FILL
            }
            val rectF = RectF(
                shape.x - shape.width / 2f,
                shape.y - shape.height / 2f,
                shape.x + shape.width / 2f,
                shape.y + shape.height / 2f
            )

            when (shape.shapeType) {
                ShapeType.RECTANGLE -> canvas.drawRect(rectF, shapePaint)
                ShapeType.ROUNDED_RECT, ShapeType.BADGE_PILL -> canvas.drawRoundRect(rectF, 24f, 24f, shapePaint)
                ShapeType.CIRCLE -> canvas.drawCircle(shape.x, shape.y, shape.height / 2f, shapePaint)
                ShapeType.STAR_BURST -> canvas.drawRoundRect(rectF, 16f, 16f, shapePaint)
            }

            if (shape.label.isNotEmpty()) {
                val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = shape.textColor
                    textSize = shape.height * 0.44f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val textBounds = Rect()
                labelPaint.getTextBounds(shape.label, 0, shape.label.length, textBounds)
                val textY = shape.y + textBounds.height() / 2f - 2f
                canvas.drawText(shape.label, shape.x, textY, labelPaint)
            }
        }

        // 3. Draw Stickers / Emojis
        for (sticker in stickers) {
            canvas.save()
            canvas.rotate(sticker.rotationDeg, sticker.x, sticker.y)
            val stickerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = sticker.size
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(sticker.emojiOrText, sticker.x, sticker.y + sticker.size / 3f, stickerPaint)
            canvas.restore()
        }

        // 4. Draw Text Layers
        for (layer in textLayers) {
            val textToDraw = if (layer.isUppercase) layer.text.uppercase() else layer.text
            val baseTypeface = if (layer.isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = layer.fontSize
                typeface = baseTypeface
                textAlign = Paint.Align.CENTER
            }

            val textBounds = Rect()
            textPaint.getTextBounds(textToDraw, 0, textToDraw.length, textBounds)

            // Background badge if enabled
            if (layer.hasBackgroundBadge) {
                val padX = 24f
                val padY = 16f
                val badgeRect = RectF(
                    layer.x - textBounds.width() / 2f - padX,
                    layer.y - textBounds.height() - padY,
                    layer.x + textBounds.width() / 2f + padX,
                    layer.y + padY
                )
                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = layer.badgeColor
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(badgeRect, 16f, 16f, badgePaint)
            }

            // Stroke (Outline)
            if (layer.strokeWidth > 0) {
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = layer.fontSize
                    typeface = baseTypeface
                    textAlign = Paint.Align.CENTER
                    color = layer.strokeColor
                    style = Paint.Style.STROKE
                    strokeWidth = layer.strokeWidth
                    strokeJoin = Paint.Join.ROUND
                }
                canvas.drawText(textToDraw, layer.x, layer.y, strokePaint)
            }

            // Text Fill with Gradient or Solid
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = layer.fontSize
                typeface = baseTypeface
                textAlign = Paint.Align.CENTER
                if (layer.hasGradient) {
                    shader = LinearGradient(
                        layer.x, layer.y - textBounds.height(),
                        layer.x, layer.y,
                        layer.gradientStartColor, layer.gradientEndColor,
                        Shader.TileMode.CLAMP
                    )
                } else {
                    color = layer.textColor
                }
                if (layer.shadowRadius > 0) {
                    setShadowLayer(layer.shadowRadius, 0f, 6f, layer.shadowColor)
                }
            }
            canvas.drawText(textToDraw, layer.x, layer.y, fillPaint)
        }

        // Export to File
        val fos = FileOutputStream(outputFile)
        result.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.flush()
        fos.close()

        return outputFile.length()
    }
}
