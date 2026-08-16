package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

data class VideoInfo(
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val bitrate: Long,
    val frameRate: Float,
    val mimeType: String,
    val fileSizeBytes: Long,
    val thumbnail: Bitmap?
)

enum class VideoPreset(
    val label: String,
    val targetResolution: String,
    val targetBitrateKbps: Int,
    val targetFps: Int,
    val description: String
) {
    WHATSAPP("WhatsApp", "720p", 1200, 30, "Optimized for WhatsApp 16MB/64MB limit"),
    INSTAGRAM_REELS("Instagram Reels", "1080p", 3500, 30, "Crisp 9:16 vertical 1080x1920"),
    YOUTUBE_SHORTS("YouTube Shorts", "1080p", 4500, 60, "High bitrate 60fps vertical format"),
    BALANCED_720P("Balanced (720p)", "720p", 2000, 30, "Great quality-to-size ratio"),
    MAX_QUALITY_1080P("Max Quality (1080p)", "1080p", 6000, 60, "Studio creator fidelity"),
    SMALL_FILE_480P("Small File (480p)", "480p", 800, 24, "Ultra fast sharing & compact size")
}

object VideoInspectorEngine {

    fun inspectVideo(context: Context, uri: Uri): VideoInfo? {
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

    fun estimateCompressedSize(durationMs: Long, preset: VideoPreset): Long {
        val durationSeconds = durationMs / 1000.0
        val targetBitrateBps = preset.targetBitrateKbps * 1000L
        val audioBitrateBps = 128000L
        val totalBitrateBps = targetBitrateBps + audioBitrateBps
        val bytes = (durationSeconds * (totalBitrateBps / 8.0)).toLong()
        return bytes.coerceAtLeast(102400L)
    }
}
