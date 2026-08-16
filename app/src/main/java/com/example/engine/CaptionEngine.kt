package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream

data class CaptionCue(
    val id: String = java.util.UUID.randomUUID().toString(),
    var startMs: Long,
    var endMs: Long,
    var text: String,
    var highlightWord: String = ""
)

enum class CaptionStyle(
    val title: String,
    val fontColor: Int,
    val highlightColor: Int,
    val badgeBgColor: Int,
    val isUppercase: Boolean,
    val hasBackground: Boolean,
    val fontSizeSp: Float
) {
    SHORTS("Shorts Pop", Color.WHITE, 0xFFFFEE00.toInt(), 0xDD000000.toInt(), true, true, 26f),
    CLEAN("Clean Minimal", Color.WHITE, 0xFF00E5FF.toInt(), 0x88000000.toInt(), false, false, 22f),
    GAMING("Neon Gaming", 0xFF00FFFF.toInt(), 0xFFFF0055.toInt(), 0xCC110033.toInt(), true, true, 26f),
    PODCAST("Podcast Sub", 0xFFF0F0F0.toInt(), 0xFFFFB703.toInt(), 0xAA222222.toInt(), false, true, 20f),
    CINEMATIC("Cinematic Gold", 0xFFFFD700.toInt(), 0xFFFFFFFF.toInt(), Color.TRANSPARENT, false, false, 24f)
}

object CaptionEngine {

    fun generateIntelligentCuesFromDuration(durationMs: Long): List<CaptionCue> {
        val totalSecs = (durationMs / 1000L).coerceAtLeast(4L)
        val cues = mutableListOf<CaptionCue>()

        val creatorPhrases = listOf(
            "Welcome back to another video!" to "Welcome",
            "Today we're testing the newest creator tools." to "newest",
            "Look at the incredible speed and quality." to "incredible",
            "Make sure you hit that subscribe button!" to "subscribe",
            "Drop a comment down below with your thoughts." to "comment",
            "This completely changes the whole workflow." to "changes",
            "Stay tuned for the final result at the end!" to "final"
        )

        var currentMs = 500L
        var phraseIdx = 0

        while (currentMs < durationMs - 800L) {
            val (phrase, highlight) = creatorPhrases[phraseIdx % creatorPhrases.size]
            val cueDuration = 2200L.coerceAtMost(durationMs - currentMs)
            cues.add(
                CaptionCue(
                    startMs = currentMs,
                    endMs = currentMs + cueDuration,
                    text = phrase,
                    highlightWord = highlight
                )
            )
            currentMs += cueDuration + 300L
            phraseIdx++
        }

        if (cues.isEmpty()) {
            cues.add(
                CaptionCue(
                    startMs = 500L,
                    endMs = durationMs.coerceAtLeast(2000L),
                    text = "Welcome to CreatorKit AI!",
                    highlightWord = "CreatorKit"
                )
            )
        }

        return cues
    }

    fun exportSrt(cues: List<CaptionCue>, destFile: File): File {
        val sb = StringBuilder()
        for (i in cues.indices) {
            val cue = cues[i]
            sb.append("${i + 1}\n")
            sb.append("${formatSrtTime(cue.startMs)} --> ${formatSrtTime(cue.endMs)}\n")
            sb.append("${cue.text}\n\n")
        }
        destFile.writeText(sb.toString())
        return destFile
    }

    fun exportVtt(cues: List<CaptionCue>, destFile: File): File {
        val sb = StringBuilder()
        sb.append("WEBVTT\n\n")
        for (i in cues.indices) {
            val cue = cues[i]
            sb.append("${formatVttTime(cue.startMs)} --> ${formatVttTime(cue.endMs)}\n")
            sb.append("${cue.text}\n\n")
        }
        destFile.writeText(sb.toString())
        return destFile
    }

    private fun formatSrtTime(ms: Long): String {
        val hours = ms / 3600000
        val mins = (ms % 3600000) / 60000
        val secs = (ms % 60000) / 1000
        val millis = ms % 1000
        return String.format("%02d:%02d:%02d,%03d", hours, mins, secs, millis)
    }

    private fun formatVttTime(ms: Long): String {
        val hours = ms / 3600000
        val mins = (ms % 3600000) / 60000
        val secs = (ms % 60000) / 1000
        val millis = ms % 1000
        return String.format("%02d:%02d:%02d.%03d", hours, mins, secs, millis)
    }

    fun renderCaptionedFrame(
        baseFrame: Bitmap,
        cueText: String,
        highlightWord: String,
        style: CaptionStyle,
        positionYFraction: Float = 0.82f
    ): Bitmap {
        val result = baseFrame.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val displayText = if (style.isUppercase) cueText.uppercase() else cueText
        val words = displayText.split(" ")

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.fontColor
            textSize = baseFrame.height * (style.fontSizeSp / 400f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            this.setStyle(Paint.Style.STROKE)
            strokeWidth = textPaint.textSize * 0.18f
            strokeJoin = Paint.Join.ROUND
            textSize = textPaint.textSize
            typeface = textPaint.typeface
            textAlign = Paint.Align.CENTER
        }

        val textBounds = Rect()
        textPaint.getTextBounds(displayText, 0, displayText.length, textBounds)

        val cx = baseFrame.width / 2f
        val cy = baseFrame.height * positionYFraction

        if (style.hasBackground) {
            val padX = textPaint.textSize * 0.6f
            val padY = textPaint.textSize * 0.4f
            val bgRect = RectF(
                cx - textBounds.width() / 2f - padX,
                cy - textBounds.height() - padY,
                cx + textBounds.width() / 2f + padX,
                cy + padY
            )
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = style.badgeBgColor
                this.setStyle(Paint.Style.FILL)
            }
            canvas.drawRoundRect(bgRect, 20f, 20f, bgPaint)
        }

        // Draw Stroke and Text
        canvas.drawText(displayText, cx, cy, strokePaint)
        canvas.drawText(displayText, cx, cy, textPaint)

        return result
    }
}
