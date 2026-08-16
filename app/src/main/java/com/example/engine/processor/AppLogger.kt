package com.example.engine.processor

import android.util.Log

object AppLogger {
    private const val TAG = "CreatorKitAI"

    fun logStart(feature: String, inputUri: String?, inputMimeType: String?, inputSize: Long) {
        Log.i(TAG, "════════════════════════════════════════════════════════════")
        Log.i(TAG, "PROCESS_START: $feature")
        Log.i(TAG, "inputUri: $inputUri")
        Log.i(TAG, "inputMimeType: $inputMimeType")
        Log.i(TAG, "inputSize: $inputSize bytes (${formatBytes(inputSize)})")
    }

    fun logProcessing(feature: String, processor: String, stage: String? = null) {
        Log.d(TAG, "PROCESSING: $feature | processor=$processor | stage=${stage ?: "running"}")
    }

    fun logResult(feature: String, outputUri: String?, outputPath: String?, outputSize: Long) {
        Log.i(TAG, "PROCESS_RESULT: $feature")
        Log.i(TAG, "outputUri: $outputUri")
        Log.i(TAG, "outputPath: $outputPath")
        Log.i(TAG, "outputSize: $outputSize bytes (${formatBytes(outputSize)})")
    }

    fun logSuccess(feature: String, message: String? = null) {
        Log.i(TAG, "PROCESS_SUCCESS: $feature - ${message ?: "Completed successfully"}")
        Log.i(TAG, "════════════════════════════════════════════════════════════")
    }

    fun logFailed(feature: String, e: Throwable, message: String? = null) {
        Log.e(TAG, "PROCESS_FAILED: $feature")
        Log.e(TAG, "message: ${message ?: e.localizedMessage}")
        Log.e(TAG, "exception: ${e::class.java.simpleName}")
        Log.e(TAG, "stackTrace:\n${Log.getStackTraceString(e)}")
        Log.e(TAG, "════════════════════════════════════════════════════════════")
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024f * 1024f * 1024f))
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024f * 1024f))
            bytes >= 1024 -> String.format("%.2f KB", bytes / 1024f)
            else -> "$bytes B"
        }
    }
}
