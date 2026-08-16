package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

data class PdfGenerationResult(
    val pageCount: Int,
    val fileSizeBytes: Long,
    val file: File
)

object PdfEngine {

    fun createPdfFromBitmaps(
        bitmaps: List<Bitmap>,
        outputFile: File,
        pageWidth: Int = 595, // A4 standard pt at 72dpi
        pageHeight: Int = 842,
        margin: Int = 24
    ): PdfGenerationResult {
        val document = PdfDocument()

        for (i in bitmaps.indices) {
            val bitmap = bitmaps[i]
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(Color.WHITE)

            val availableWidth = pageWidth - (margin * 2)
            val availableHeight = pageHeight - (margin * 2)

            val scale = min(
                availableWidth.toFloat() / bitmap.width,
                availableHeight.toFloat() / bitmap.height
            )

            val drawWidth = (bitmap.width * scale).toInt()
            val drawHeight = (bitmap.height * scale).toInt()

            val left = margin + (availableWidth - drawWidth) / 2
            val top = margin + (availableHeight - drawHeight) / 2

            val dstRect = Rect(left, top, left + drawWidth, top + drawHeight)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(bitmap, null, dstRect, paint)

            // Optional page numbering
            val pageTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.DKGRAY
                textSize = 10f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("${i + 1} / ${bitmaps.size}", pageWidth / 2f, pageHeight - 10f, pageTextPaint)

            document.finishPage(page)
        }

        val fos = FileOutputStream(outputFile)
        document.writeTo(fos)
        fos.flush()
        fos.close()
        document.close()

        return PdfGenerationResult(
            pageCount = bitmaps.size,
            fileSizeBytes = outputFile.length(),
            file = outputFile
        )
    }
}
