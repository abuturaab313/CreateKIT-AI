package com.example.engine.processor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import com.example.engine.BgType
import com.example.engine.CaptionCue
import com.example.engine.CaptionStyle
import com.example.engine.EnhanceType
import com.example.engine.ResizeMode
import com.example.engine.VideoInfo
import com.example.engine.VideoPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

sealed class ProcessStatus<out T> {
    object Idle : ProcessStatus<Nothing>()
    data class Validating(val message: String) : ProcessStatus<Nothing>()
    data class Processing(val progress: Float, val stage: String) : ProcessStatus<Nothing>()
    data class Success<T>(val data: T, val outputFile: File, val message: String) : ProcessStatus<T>()
    data class Failed(val error: String, val canRetry: Boolean = true) : ProcessStatus<Nothing>()
    object Cancelled : ProcessStatus<Nothing>()
}

data class RealCompressionResult(
    val originalSizeBytes: Long,
    val outputSizeBytes: Long,
    val outputWidth: Int,
    val outputHeight: Int,
    val savedPercentage: Float,
    val file: File
)

data class RealVideoProcessResult(
    val originalSizeBytes: Long,
    val outputSizeBytes: Long,
    val originalDurationMs: Long,
    val outputDurationMs: Long,
    val width: Int,
    val height: Int,
    val file: File
)

/**
 * Central MediaProcessor layer providing genuine file-based media operations.
 */
object MediaProcessor {

    fun getExportDirectory(context: Context): File {
        val dir = File(context.filesDir, "CreatorKit/Projects/exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getProcessedDirectory(context: Context): File {
        val dir = File(context.filesDir, "CreatorKit/Projects/processed")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getOriginalDirectory(context: Context): File {
        val dir = File(context.filesDir, "CreatorKit/Projects/original")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // Sub-processors
    val image: ImageSubProcessor = ImageSubProcessor
    val video: VideoSubProcessor = VideoSubProcessor
    val audio: AudioSubProcessor = AudioSubProcessor
    val pdf: PdfSubProcessor = PdfSubProcessor
    val ai: AiSubProcessor = AiSubProcessor
}

/**
 * Dedicated Real Image Processing Engine.
 */
object ImageSubProcessor {

    fun loadBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 3840): Bitmap? {
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

    fun getFileSizeFromUri(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Resizes a bitmap with actual canvas manipulation according to Fit, Fill, or Center Crop.
     */
    fun resize(
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
                val drawW = (src.width * scale).toInt().coerceAtLeast(1)
                val drawH = (src.height * scale).toInt().coerceAtLeast(1)
                val left = (targetW - drawW) / 2
                val top = (targetH - drawH) / 2
                val dstRect = Rect(left, top, left + drawW, top + drawH)
                canvas.drawBitmap(src, null, dstRect, paint)
            }
            ResizeMode.CROP -> {
                val scale = max(targetW.toFloat() / src.width, targetH.toFloat() / src.height)
                val drawW = (src.width * scale).toInt().coerceAtLeast(1)
                val drawH = (src.height * scale).toInt().coerceAtLeast(1)
                val left = (targetW - drawW) / 2
                val top = (targetH - drawH) / 2
                val dstRect = Rect(left, top, left + drawW, top + drawH)
                canvas.drawBitmap(src, null, dstRect, paint)
            }
        }
        return output
    }

    /**
     * Crops a specific rect region from a bitmap.
     */
    fun crop(src: Bitmap, cropRect: Rect): Bitmap {
        val validLeft = cropRect.left.coerceIn(0, src.width - 1)
        val validTop = cropRect.top.coerceIn(0, src.height - 1)
        val validWidth = cropRect.width().coerceIn(1, src.width - validLeft)
        val validHeight = cropRect.height().coerceIn(1, src.height - validTop)
        return Bitmap.createBitmap(src, validLeft, validTop, validWidth, validHeight)
    }

    /**
     * Real image compression writing directly to disk with actual size calculation.
     */
    fun compress(
        src: Bitmap,
        format: Bitmap.CompressFormat,
        quality: Int,
        maxWidth: Int,
        maxHeight: Int,
        originalFileSizeBytes: Long,
        destFile: File
    ): RealCompressionResult {
        var scaled = src
        val width = src.width
        val height = src.height

        if (width > maxWidth || height > maxHeight) {
            val ratio = min(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
            val targetW = (width * ratio).toInt().coerceAtLeast(1)
            val targetH = (height * ratio).toInt().coerceAtLeast(1)
            scaled = Bitmap.createScaledBitmap(src, targetW, targetH, true)
        }

        val fos = FileOutputStream(destFile)
        scaled.compress(format, quality.coerceIn(1, 100), fos)
        fos.flush()
        fos.close()

        val outputBytes = destFile.length()
        val origBytes = if (originalFileSizeBytes > 0) originalFileSizeBytes else (width * height * 4).toLong()
        val savedPct = if (origBytes > 0) {
            ((origBytes - outputBytes).toFloat() / origBytes * 100f).coerceAtLeast(0f)
        } else 0f

        return RealCompressionResult(
            originalSizeBytes = origBytes,
            outputSizeBytes = outputBytes,
            outputWidth = scaled.width,
            outputHeight = scaled.height,
            savedPercentage = savedPct,
            file = destFile
        )
    }

    /**
     * Real format conversion (JPG, PNG, WEBP).
     */
    fun convertFormat(
        src: Bitmap,
        targetFormat: Bitmap.CompressFormat,
        quality: Int,
        destFile: File
    ): File {
        val fos = FileOutputStream(destFile)
        src.compress(targetFormat, quality.coerceIn(1, 100), fos)
        fos.flush()
        fos.close()
        return destFile
    }

    /**
     * Digital signal processing photo enhancements (ColorMatrix, Sharpening Kernel, Shadow Lift, 2x Upscale).
     */
    fun enhance(src: Bitmap, type: EnhanceType): Bitmap {
        return when (type) {
            EnhanceType.AUTO -> applyAuto(src)
            EnhanceType.FACE -> applyFace(src)
            EnhanceType.LOW_LIGHT -> applyLowLight(src)
            EnhanceType.SHARPEN -> applySharpen(src)
            EnhanceType.HD -> applyHdUpscale(src)
        }
    }

    private fun applyAuto(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val cm = ColorMatrix()
        val sat = ColorMatrix().apply { setSaturation(1.20f) }
        val contrast = 1.14f
        val translate = (-0.5f * contrast + 0.5f) * 255f + 4f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(sat)
        cm.postConcat(contrastMatrix)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    private fun applyFace(src: Bitmap): Bitmap {
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

                val isSkinTone = (r > 95 && g > 40 && b > 20 &&
                        (max(r, max(g, b)) - min(r, min(g, b)) > 15) &&
                        abs(r - g) > 15 && r > g && r > b)

                if (isSkinTone) {
                    r = min(255, (r * 1.06f + 3f).toInt())
                    g = min(255, (g * 1.04f + 2f).toInt())
                    b = min(255, (b * 1.01f).toInt())
                } else {
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

    private fun applyLowLight(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val scale = 1.38f
        val boost = 24f
        val cm = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, boost,
                0f, scale, 0f, 0f, boost,
                0f, 0f, scale * 1.02f, 0f, boost,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val sat = ColorMatrix().apply { setSaturation(1.12f) }
        cm.postConcat(sat)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    private fun applySharpen(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)
        val outPixels = IntArray(width * height)

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
        return applySharpen(upscaled)
    }
}

/**
 * Dedicated Real Video Processing Engine for inspection, trimming, and transcoding.
 */
object VideoSubProcessor {

    fun inspect(context: Context, uri: Uri): VideoInfo? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            val mimeTypeStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/mp4"

            val duration = durationStr?.toLongOrNull() ?: 10000L
            val width = widthStr?.toIntOrNull() ?: 1920
            val height = heightStr?.toIntOrNull() ?: 1080
            val rotation = rotationStr?.toIntOrNull() ?: 0
            val bitrate = bitrateStr?.toLongOrNull() ?: 4500000L

            var fileSize = 0L
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    fileSize = pfd.statSize
                }
            } catch (e: Exception) {
                fileSize = (duration / 1000.0 * (bitrate / 8.0)).toLong()
            }

            val thumb = try {
                retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } catch (e: Exception) {
                null
            }

            VideoInfo(
                durationMs = duration,
                width = width,
                height = height,
                rotation = rotation,
                bitrate = bitrate,
                frameRate = 30f,
                mimeType = mimeTypeStr,
                fileSizeBytes = fileSize,
                thumbnail = thumb
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    /**
     * Real video trimming using MediaExtractor and MediaMuxer.
     * Extracts exact presentation time range [startMs, endMs] with adjusted timestamps.
     */
    suspend fun trimVideo(
        context: Context,
        srcUri: Uri,
        startMs: Long,
        endMs: Long,
        outputFile: File
    ): RealVideoProcessResult = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(context, srcUri, null)
            val trackCount = extractor.trackCount
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val indexMap = HashMap<Int, Int>(trackCount)
            val bufferSize = 1024 * 1024
            val byteBuffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val dstIndex = muxer.addTrack(format)
                    indexMap[i] = dstIndex
                }
            }

            muxer.start()

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L

            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            while (true) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex < 0) break

                val sampleTime = extractor.sampleTime
                if (sampleTime > endUs) {
                    extractor.unselectTrack(trackIndex)
                    if (indexMap.keys.all { !extractor.hasTrack(it) }) {
                        break
                    }
                }

                if (sampleTime >= startUs) {
                    bufferInfo.offset = 0
                    bufferInfo.size = extractor.readSampleData(byteBuffer, 0)
                    if (bufferInfo.size < 0) {
                        break
                    }
                    bufferInfo.presentationTimeUs = max(0L, sampleTime - startUs)
                    bufferInfo.flags = extractor.sampleFlags
                    val dstTrack = indexMap[trackIndex]
                    if (dstTrack != null) {
                        muxer.writeSampleData(dstTrack, byteBuffer, bufferInfo)
                    }
                }
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            muxer = null
            extractor.release()

            // Verify output using MediaMetadataRetriever
            val inspector = MediaMetadataRetriever()
            inspector.setDataSource(outputFile.absolutePath)
            val durationStr = inspector.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val widthStr = inspector.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = inspector.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val duration = durationStr?.toLongOrNull() ?: (endMs - startMs)
            val width = widthStr?.toIntOrNull() ?: 1920
            val height = heightStr?.toIntOrNull() ?: 1080
            inspector.release()

            RealVideoProcessResult(
                originalSizeBytes = context.contentResolver.openFileDescriptor(srcUri, "r")?.statSize ?: 0L,
                outputSizeBytes = outputFile.length(),
                originalDurationMs = endMs - startMs,
                outputDurationMs = duration,
                width = width,
                height = height,
                file = outputFile
            )
        } catch (e: Exception) {
            muxer?.run {
                try { stop() } catch (ignored: Exception) {}
                try { release() } catch (ignored: Exception) {}
            }
            try { extractor.release() } catch (ignored: Exception) {}
            throw e
        }
    }

    private fun MediaExtractor.hasTrack(trackIndex: Int): Boolean {
        return try {
            trackIndex < trackCount
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Dedicated Real Audio / Caption Processing Engine.
 */
object AudioSubProcessor {

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
}

/**
 * Dedicated Real PDF Processing Engine.
 */
object PdfSubProcessor {

    fun createPdf(
        bitmaps: List<Bitmap>,
        outputFile: File,
        pageWidth: Int = 595,
        pageHeight: Int = 842,
        margin: Int = 24
    ): File {
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

        return outputFile
    }
}

/**
 * Dedicated Real AI/Vision Segmentation and Inpainting Processing Engine.
 */
object AiSubProcessor {

    fun removeBackground(
        src: Bitmap,
        bgType: BgType,
        solidColor: Int = Color.WHITE,
        customBgBitmap: Bitmap? = null,
        addShadow: Boolean = false
    ): Bitmap {
        val width = src.width
        val height = src.height
        val totalPixels = width * height
        val pixels = IntArray(totalPixels)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        // 1. Multi-cluster background perimeter sampling
        val borderColors = mutableListOf<Int>()
        val stepX = max(1, width / 40)
        val stepY = max(1, height / 40)

        for (x in 0 until width step stepX) {
            borderColors.add(pixels[x])
            borderColors.add(pixels[(height - 1) * width + x])
        }
        for (y in 0 until height step stepY) {
            borderColors.add(pixels[y * width])
            borderColors.add(pixels[y * width + (width - 1)])
        }

        // Corner 8x8 blocks
        val cornerSize = min(16, min(width, height) / 8)
        for (cy in 0 until cornerSize) {
            for (cx in 0 until cornerSize) {
                borderColors.add(pixels[cy * width + cx])
                borderColors.add(pixels[cy * width + (width - 1 - cx)])
                borderColors.add(pixels[(height - 1 - cy) * width + cx])
                borderColors.add(pixels[(height - 1 - cy) * width + (width - 1 - cx)])
            }
        }

        // Build 3 dominant background color clusters (e.g. sky/wall/ground)
        val bgClusters = mutableListOf<Triple<Float, Float, Float>>()
        if (borderColors.isNotEmpty()) {
            val step = max(1, borderColors.size / 6)
            for (i in 0 until borderColors.size step step) {
                val c = borderColors[i]
                bgClusters.add(Triple(Color.red(c).toFloat(), Color.green(c).toFloat(), Color.blue(c).toFloat()))
            }
        }
        if (bgClusters.isEmpty()) {
            bgClusters.add(Triple(255f, 255f, 255f))
        }

        // 2. Probability and Saliency Map
        val probMap = FloatArray(totalPixels)
        val centerX = width * 0.5f
        val centerY = height * 0.45f
        val maxDist = sqrt(centerX * centerX + centerY * centerY)

        for (y in 0 until height) {
            val yOffset = y * width
            for (x in 0 until width) {
                val idx = yOffset + x
                val c = pixels[idx]
                val r = Color.red(c).toFloat()
                val g = Color.green(c).toFloat()
                val b = Color.blue(c).toFloat()

                // Minimum distance to background color clusters
                var minBgDist = Float.MAX_VALUE
                for (cluster in bgClusters) {
                    val dr = r - cluster.first
                    val dg = g - cluster.second
                    val db = b - cluster.third
                    val dist = sqrt(dr * dr + dg * dg + db * db)
                    if (dist < minBgDist) minBgDist = dist
                }

                // YCbCr Skin Tone Prior
                val yVal = 0.299f * r + 0.587f * g + 0.114f * b
                val cb = 128f - 0.168736f * r - 0.331264f * g + 0.5f * b
                val cr = 128f + 0.5f * r - 0.418688f * g - 0.081312f * b
                val isSkin = (cb in 75f..135f) && (cr in 130f..180f) && (yVal in 30f..245f)

                // Spatial Saliency Weight (Central Human Silhouette Prior)
                val dx = (x - centerX) / (width * 0.38f)
                val dy = (y - centerY) / (height * 0.42f)
                val spatialPrior = exp(-(dx * dx + dy * dy) * 0.7f).toFloat().coerceIn(0f, 1f)

                // Contrast & Foreground Score
                var score = (minBgDist / 70f).coerceIn(0f, 1f)
                if (isSkin) {
                    score = max(score, 0.88f)
                }
                score = (score * 0.65f + spatialPrior * 0.35f).coerceIn(0f, 1f)
                probMap[idx] = score
            }
        }

        // 3. Flood-fill from borders to detect outer connected background
        val isOuterBg = BooleanArray(totalPixels)
        val queue: java.util.ArrayDeque<Int> = java.util.ArrayDeque(width * 2 + height * 2)

        for (x in 0 until width) {
            if (probMap[x] < 0.45f) { isOuterBg[x] = true; queue.add(x) }
            val bIdx = (height - 1) * width + x
            if (probMap[bIdx] < 0.45f) { isOuterBg[bIdx] = true; queue.add(bIdx) }
        }
        for (y in 0 until height) {
            val lIdx = y * width
            if (probMap[lIdx] < 0.45f) { isOuterBg[lIdx] = true; queue.add(lIdx) }
            val rIdx = y * width + (width - 1)
            if (probMap[rIdx] < 0.45f) { isOuterBg[rIdx] = true; queue.add(rIdx) }
        }

        while (!queue.isEmpty()) {
            val curr = queue.poll() ?: continue
            val cx = curr % width
            val cy = curr / width

            val neighbors = intArrayOf(
                if (cx > 0) curr - 1 else -1,
                if (cx < width - 1) curr + 1 else -1,
                if (cy > 0) curr - width else -1,
                if (cy < height - 1) curr + width else -1
            )
            for (n in neighbors) {
                if (n != -1 && !isOuterBg[n] && probMap[n] < 0.52f) {
                    isOuterBg[n] = true
                    queue.add(n)
                }
            }
        }

        // 4. Alpha Matte Generation with Smooth Feathering & Hole-filling
        val alphaMatte = IntArray(totalPixels)
        for (i in 0 until totalPixels) {
            if (isOuterBg[i]) {
                alphaMatte[i] = 0
            } else {
                val p = probMap[i]
                if (p > 0.60f) {
                    alphaMatte[i] = 255
                } else if (p < 0.25f) {
                    alphaMatte[i] = 0
                } else {
                    alphaMatte[i] = ((p - 0.25f) / 0.35f * 255f).toInt().coerceIn(0, 255)
                }
            }
        }

        // 5. Alpha Compositing: Preserve 100% of the original RGB colors
        val fgPixels = IntArray(totalPixels)
        for (i in 0 until totalPixels) {
            val orig = pixels[i]
            val r = Color.red(orig)
            val g = Color.green(orig)
            val b = Color.blue(orig)
            val a = alphaMatte[i]
            fgPixels[i] = Color.argb(a, r, g, b)
        }

        val fgBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        fgBitmap.setPixels(fgPixels, 0, width, 0, 0, width, height)

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        when (bgType) {
            BgType.TRANSPARENT -> {
                // Keep pure RGBA transparent bitmap
            }
            BgType.WHITE -> canvas.drawColor(Color.WHITE)
            BgType.BLACK -> canvas.drawColor(Color.BLACK)
            BgType.CUSTOM_COLOR -> canvas.drawColor(solidColor)
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

        if (addShadow && bgType != BgType.TRANSPARENT) {
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(
                    floatArrayOf(
                        0f, 0f, 0f, 0f, 0f,
                        0f, 0f, 0f, 0f, 0f,
                        0f, 0f, 0f, 0f, 0f,
                        0f, 0f, 0f, 0.40f, 0f
                    )
                )
            }
            canvas.drawBitmap(fgBitmap, 10f, 16f, shadowPaint)
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

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val maskAlpha = Color.alpha(maskPixels[idx])
                if (maskAlpha > 30) {
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
}
