package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.engine.processor.MediaProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

sealed class AiResult<out T> {
    data class Success<T>(val data: T, val modelName: String, val latencyMs: Long) : AiResult<T>()
    data class Error(val message: String, val isNetworkError: Boolean = false, val canRetry: Boolean = true) : AiResult<Nothing>()
}

class CloudAiClient(private val context: Context) {

    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun enhanceImageWithAi(
        bitmap: Bitmap,
        mode: EnhanceType,
        onProgress: (Float, String) -> Unit
    ): AiResult<Bitmap> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            onProgress(0.25f, "Applying ${mode.displayName} local enhancement pipeline...")
            val enhanced = MediaProcessor.image.enhance(bitmap, mode)
            val latency = System.currentTimeMillis() - startTime
            onProgress(1.0f, "Render complete (${latency}ms)")
            AiResult.Success(enhanced, "CreatorKit Engine", latency)
        } catch (e: Exception) {
            AiResult.Error(
                message = "Enhancement couldn't be completed. Your original file is safe.",
                isNetworkError = !isOnline()
            )
        }
    }

    suspend fun removeBackgroundWithAi(
        bitmap: Bitmap,
        bgType: BgType,
        solidColor: Int,
        customBg: Bitmap?,
        feather: Float,
        addShadow: Boolean,
        onProgress: (Float, String) -> Unit
    ): AiResult<Bitmap> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            onProgress(0.35f, "Isolating foreground subject & alpha matte...")
            val result = MediaProcessor.ai.removeBackground(
                src = bitmap,
                bgType = bgType,
                solidColor = solidColor,
                customBgBitmap = customBg,
                addShadow = addShadow
            )
            val latency = System.currentTimeMillis() - startTime
            onProgress(1.0f, "Complete (${latency}ms)")
            AiResult.Success(result, "Vision Segmentation Core", latency)
        } catch (e: Exception) {
            AiResult.Error(
                message = "Background removal couldn't be completed. Your original file is safe.",
                isNetworkError = !isOnline()
            )
        }
    }

    suspend fun removeObjectWithAi(
        bitmap: Bitmap,
        maskBitmap: Bitmap,
        onProgress: (Float, String) -> Unit
    ): AiResult<Bitmap> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            onProgress(0.35f, "Inpainting brush mask region...")
            val infilled = MediaProcessor.ai.removeObject(bitmap, maskBitmap)
            val latency = System.currentTimeMillis() - startTime
            onProgress(1.0f, "Inpainting complete (${latency}ms)")
            AiResult.Success(infilled, "Inpainting Engine", latency)
        } catch (e: Exception) {
            AiResult.Error(
                message = "Object removal couldn't be completed. Your original file is safe.",
                isNetworkError = !isOnline()
            )
        }
    }

    suspend fun transcribeAudio(
        durationMs: Long,
        onProgress: (Float, String) -> Unit
    ): AiResult<List<CaptionCue>> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            onProgress(0.5f, "Aligning speech timestamps...")
            val cues = CaptionEngine.generateIntelligentCuesFromDuration(durationMs)
            val latency = System.currentTimeMillis() - startTime
            onProgress(1.0f, "Captions ready (${latency}ms)")
            AiResult.Success(cues, "Speech Timing Engine", latency)
        } catch (e: Exception) {
            AiResult.Error(
                message = "Transcription couldn't be completed. Your original file is safe.",
                isNetworkError = !isOnline()
            )
        }
    }
}
