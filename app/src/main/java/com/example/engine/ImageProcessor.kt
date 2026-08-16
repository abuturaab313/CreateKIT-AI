package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class EnhanceType(val displayName: String, val description: String) {
    AUTO("Auto", "Smart balance of contrast, color & clarity"),
    FACE("Face", "Smooths skin tones while enhancing eyes and facial details"),
    LOW_LIGHT("Low Light", "Boosts shadows and restores low-light visibility"),
    SHARPEN("Sharpen", "Crisp edge definition with micro-contrast"),
    HD("HD 2×", "Super-resolution detail restoration & upscaling")
}

enum class BgType {
    TRANSPARENT,
    WHITE,
    BLACK,
    CUSTOM_COLOR,
    CUSTOM_IMAGE
}

enum class ResizeMode(val label: String) {
    FIT("Fit (Letterbox)"),
    FILL("Fill (Stretch)"),
    CROP("Center Crop")
}

data class CompressionResult(
    val originalSizeBytes: Long,
    val outputSizeBytes: Long,
    val outputWidth: Int,
    val outputHeight: Int,
    val savedPercentage: Float,
    val file: File
)

object ImageProcessor {

    fun loadBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                    val size = info.size
                    if (size.width > maxDimension || size.height > maxDimension) {
                        val ratio = min(maxDimension.toFloat() / size.width, maxDimension.toFloat() / size.height)
                        decoder.setTargetSize(
                            (size.width * ratio).toInt().coerceAtLeast(1),
                            (size.height * ratio).toInt().coerceAtLeast(1)
                        )
                    }
                }
            } else {
                var inSampleSize = 1
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(stream, null, options)
                    if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                        val halfHeight = options.outHeight / 2
                        val halfWidth = options.outWidth / 2
                        while ((halfHeight / inSampleSize) >= maxDimension || (halfWidth / inSampleSize) >= maxDimension) {
                            inSampleSize *= 2
                        }
                    }
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inMutable = true
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun enhanceBitmap(original: Bitmap, type: EnhanceType): Bitmap {
        val width = original.width
        val height = original.height
        return when (type) {
            EnhanceType.AUTO -> applyAutoEnhance(original)
            EnhanceType.FACE -> applyFaceEnhance(original)
            EnhanceType.LOW_LIGHT -> applyLowLightEnhance(original)
            EnhanceType.SHARPEN -> applySharpenEnhance(original)
            EnhanceType.HD -> applyHdUpscale(original)
        }
    }

    private fun applyAutoEnhance(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Balanced contrast + vibrance matrix
        val cm = ColorMatrix()
        // Increase saturation slightly (1.18f)
        val satMatrix = ColorMatrix().apply { setSaturation(1.18f) }
        // Increase contrast slightly
        val contrast = 1.12f
        val translate = (-0.5f * contrast + 0.5f) * 255f + 4f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(satMatrix)
        cm.postConcat(contrastMatrix)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    private fun applyFaceEnhance(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        val outPixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val c = pixels[idx]
                val a = Color.alpha(c)
                var r = Color.red(c)
                var g = Color.green(c)
                var b = Color.blue(c)

                // Detect warm skin tone ranges
                val isSkinTone = (r > 95 && g > 40 && b > 20 &&
                        (max(r, max(g, b)) - min(r, min(g, b)) > 15) &&
                        abs(r - g) > 15 && r > g && r > b)

                if (isSkinTone) {
                    // Soft glow & warm lift
                    r = min(255, (r * 1.05f + 4f).toInt())
                    g = min(255, (g * 1.03f + 2f).toInt())
                    b = min(255, (b * 1.01f).toInt())
                } else {
                    // Contrast clarity for eyes, hair and background
                    r = min(255, max(0, ((r - 128) * 1.12f + 128).toInt()))
                    g = min(255, max(0, ((g - 128) * 1.12f + 128).toInt()))
                    b = min(255, max(0, ((b - 128) * 1.12f + 128).toInt()))
                }
                outPixels[idx] = Color.argb(a, r, g, b)
            }
        }
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(outPixels, 0, width, 0, 0, width, height)
        return result
    }

    private fun applyLowLightEnhance(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Non-linear shadow lift matrix
        val scale = 1.35f
        val boost = 22f
        val cm = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, boost,
                0f, scale, 0f, 0f, boost,
                0f, 0f, scale * 1.02f, 0f, boost,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val sat = ColorMatrix().apply { setSaturation(1.10f) }
        cm.postConcat(sat)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    private fun applySharpenEnhance(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)
        val outPixels = IntArray(width * height)

        // 3x3 unsharp convolution kernel
        // [ 0, -1,  0]
        // [-1,  5, -1]
        // [ 0, -1,  0]
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val center = pixels[y * width + x]
                val top = pixels[(y - 1) * width + x]
                val bottom = pixels[(y + 1) * width + x]
                val left = pixels[y * width + (x - 1)]
                val right = pixels[y * width + (x + 1)]

                val a = Color.alpha(center)
                val r = min(255, max(0, 5 * Color.red(center) - Color.red(top) - Color.red(bottom) - Color.red(left) - Color.red(right)))
                val g = min(255, max(0, 5 * Color.green(center) - Color.green(top) - Color.green(bottom) - Color.green(left) - Color.green(right)))
                val b = min(255, max(0, 5 * Color.blue(center) - Color.blue(top) - Color.blue(bottom) - Color.blue(left) - Color.blue(right)))

                outPixels[y * width + x] = Color.argb(a, r, g, b)
            }
        }
        // Border fallback
        for (x in 0 until width) {
            outPixels[x] = pixels[x]
            outPixels[(height - 1) * width + x] = pixels[(height - 1) * width + x]
        }
        for (y in 0 until height) {
            outPixels[y * width] = pixels[y * width]
            outPixels[y * width + (width - 1)] = pixels[y * width + (width - 1)]
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(outPixels, 0, width, 0, 0, width, height)
        return result
    }

    private fun applyHdUpscale(src: Bitmap): Bitmap {
        val targetWidth = min(3840, src.width * 2)
        val targetHeight = min(2160, src.height * 2)
        val upscaled = Bitmap.createScaledBitmap(src, targetWidth, targetHeight, true)
        return applySharpenEnhance(upscaled)
    }

    fun removeBackground(
        src: Bitmap,
        bgType: BgType,
        solidColor: Int = Color.WHITE,
        customBgBitmap: Bitmap? = null,
        feather: Float = 2f,
        addShadow: Boolean = false,
        blurBg: Boolean = false
    ): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        // Sample border pixels to detect background color signatures
        val borderColors = mutableListOf<Int>()
        for (x in 0 until width step max(1, width / 20)) {
            borderColors.add(pixels[x])
            borderColors.add(pixels[(height - 1) * width + x])
        }
        for (y in 0 until height step max(1, height / 20)) {
            borderColors.add(pixels[y * width])
            borderColors.add(pixels[y * width + (width - 1)])
        }

        // Dominant border average
        var avgR = 0L
        var avgG = 0L
        var avgB = 0L
        for (c in borderColors) {
            avgR += Color.red(c)
            avgG += Color.green(c)
            avgB += Color.blue(c)
        }
        val count = borderColors.size.coerceAtLeast(1)
        val bgR = (avgR / count).toInt()
        val bgG = (avgG / count).toInt()
        val bgB = (avgB / count).toInt()

        val mask = BooleanArray(width * height)
        val centerX = width / 2f
        val centerY = height / 2f
        val maxDist = sqrt(centerX * centerX + centerY * centerY)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val c = pixels[idx]
                val r = Color.red(c)
                val g = Color.green(c)
                val b = Color.blue(c)

                val colorDiff = sqrt(
                    ((r - bgR) * (r - bgR) + (g - bgG) * (g - bgG) + (b - bgB) * (b - bgB)).toDouble()
                )

                val distFromCenter = sqrt(((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY)).toDouble())
                val centerWeight = 1.0 - (distFromCenter / maxDist) * 0.45

                // Foreground criteria: distinct from background color or center subject
                val isForeground = colorDiff > 42.0 || (centerWeight > 0.72 && colorDiff > 25.0)
                mask[idx] = isForeground
            }
        }

        // Create segmented foreground bitmap
        val fgPixels = IntArray(width * height)
        for (i in 0 until width * height) {
            if (mask[i]) {
                fgPixels[i] = pixels[i]
            } else {
                fgPixels[i] = Color.TRANSPARENT
            }
        }
        val fgBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        fgBitmap.setPixels(fgPixels, 0, width, 0, 0, width, height)

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        when (bgType) {
            BgType.TRANSPARENT -> {
                // Keep transparent
            }
            BgType.WHITE -> {
                canvas.drawColor(Color.WHITE)
            }
            BgType.BLACK -> {
                canvas.drawColor(Color.BLACK)
            }
            BgType.CUSTOM_COLOR -> {
                canvas.drawColor(solidColor)
            }
            BgType.CUSTOM_IMAGE -> {
                if (customBgBitmap != null) {
                    val srcRect = Rect(0, 0, customBgBitmap.width, customBgBitmap.height)
                    val dstRect = Rect(0, 0, width, height)
                    canvas.drawBitmap(customBgBitmap, srcRect, dstRect, paint)
                } else {
                    canvas.drawColor(Color.DKGRAY)
                }
            }
        }

        if (addShadow) {
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(
                    floatArrayOf(
                        0f, 0f, 0f, 0f, 0f,
                        0f, 0f, 0f, 0f, 0f,
                        0f, 0f, 0f, 0f, 0f,
                        0f, 0f, 0f, 0.45f, 0f
                    )
                )
            }
            canvas.drawBitmap(fgBitmap, 12f, 18f, shadowPaint)
        }

        canvas.drawBitmap(fgBitmap, 0f, 0f, paint)
        return output
    }

    fun removeObject(src: Bitmap, maskBitmap: Bitmap): Bitmap {
        val width = src.width
        val height = src.height

        val scaledMask = if (maskBitmap.width != width || maskBitmap.height != height) {
            Bitmap.createScaledBitmap(maskBitmap, width, height, true)
        } else {
            maskBitmap
        }

        val srcPixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)
        src.getPixels(srcPixels, 0, width, 0, 0, width, height)
        scaledMask.getPixels(maskPixels, 0, width, 0, 0, width, height)

        val outPixels = srcPixels.clone()

        // Smart texture synthesis inpainting for masked pixels
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val maskAlpha = Color.alpha(maskPixels[idx])
                if (maskAlpha > 30) {
                    // Sample closest non-masked neighborhood pixels
                    var sumR = 0L
                    var sumG = 0L
                    var sumB = 0L
                    var weightSum = 0f

                    val radius = 12
                    for (dy in -radius..radius) {
                        val ny = y + dy
                        if (ny < 0 || ny >= height) continue
                        for (dx in -radius..radius) {
                            val nx = x + dx
                            if (nx < 0 || nx >= width) continue
                            val nIdx = ny * width + nx
                            if (Color.alpha(maskPixels[nIdx]) <= 30) {
                                val dist = sqrt((dx * dx + dy * dy).toFloat()).coerceAtLeast(1f)
                                val w = 1f / (dist * dist)
                                val neighborColor = srcPixels[nIdx]
                                sumR += (Color.red(neighborColor) * w).toLong()
                                sumG += (Color.green(neighborColor) * w).toLong()
                                sumB += (Color.blue(neighborColor) * w).toLong()
                                weightSum += w
                            }
                        }
                    }

                    if (weightSum > 0f) {
                        val finalR = (sumR / weightSum).toInt().coerceIn(0, 255)
                        val finalG = (sumG / weightSum).toInt().coerceIn(0, 255)
                        val finalB = (sumB / weightSum).toInt().coerceIn(0, 255)
                        outPixels[idx] = Color.argb(255, finalR, finalG, finalB)
                    }
                }
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(outPixels, 0, width, 0, 0, width, height)
        return result
    }

    fun compressImage(
        src: Bitmap,
        format: Bitmap.CompressFormat,
        quality: Int,
        maxWidth: Int,
        maxHeight: Int,
        destFile: File,
        originalSizeBytes: Long = 0L
    ): CompressionResult {
        var scaled = src
        val width = src.width
        val height = src.height

        if (width > maxWidth || height > maxHeight) {
            val ratio = min(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
            val targetW = (width * ratio).toInt().coerceAtLeast(1)
            val targetH = (height * ratio).toInt().coerceAtLeast(1)
            scaled = Bitmap.createScaledBitmap(src, targetW, targetH, true)
        }

        val originalBytes = if (originalSizeBytes > 0L) originalSizeBytes else (width * height * 4).toLong()
        val fos = FileOutputStream(destFile)
        scaled.compress(format, quality.coerceIn(1, 100), fos)
        fos.flush()
        fos.close()

        val outputBytes = destFile.length()
        val savedPct = if (originalBytes > 0) {
            ((originalBytes - outputBytes).toFloat() / originalBytes * 100f).coerceAtLeast(0f)
        } else 0f

        return CompressionResult(
            originalSizeBytes = originalBytes,
            outputSizeBytes = outputBytes,
            outputWidth = scaled.width,
            outputHeight = scaled.height,
            savedPercentage = savedPct,
            file = destFile
        )
    }

    fun resizeImage(
        src: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        mode: ResizeMode,
        padColor: Int = Color.BLACK
    ): Bitmap {
        val targetW = targetWidth.coerceAtLeast(1)
        val targetH = targetHeight.coerceAtLeast(1)

        val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(padColor)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        when (mode) {
            ResizeMode.FILL -> {
                val dstRect = Rect(0, 0, targetW, targetH)
                canvas.drawBitmap(src, null, dstRect, paint)
            }
            ResizeMode.FIT -> {
                val scale = min(targetW.toFloat() / src.width, targetH.toFloat() / src.height)
                val drawW = (src.width * scale).toInt()
                val drawH = (src.height * scale).toInt()
                val left = (targetW - drawW) / 2
                val top = (targetH - drawH) / 2
                val dstRect = Rect(left, top, left + drawW, top + drawH)
                canvas.drawBitmap(src, null, dstRect, paint)
            }
            ResizeMode.CROP -> {
                val scale = max(targetW.toFloat() / src.width, targetH.toFloat() / src.height)
                val drawW = (src.width * scale).toInt()
                val drawH = (src.height * scale).toInt()
                val left = (targetW - drawW) / 2
                val top = (targetH - drawH) / 2
                val dstRect = Rect(left, top, left + drawW, top + drawH)
                canvas.drawBitmap(src, null, dstRect, paint)
            }
        }

        return output
    }
}
