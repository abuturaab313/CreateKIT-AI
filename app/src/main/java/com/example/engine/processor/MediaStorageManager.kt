package com.example.engine.processor

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object MediaStorageManager {

    /**
     * Inserts an existing processed image file into Android MediaStore (Pictures/CreatorKit).
     */
    suspend fun saveImageToGallery(
        context: Context,
        sourceFile: File,
        displayName: String,
        mimeType: String = "image/jpeg"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists() || sourceFile.length() == 0L) {
                throw IllegalStateException("Source file does not exist or is empty: ${sourceFile.absolutePath}")
            }

            val filename = if (displayName.contains(".")) displayName else "$displayName.${getExtensionFromMime(mimeType)}"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CreatorKit")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val itemUri = resolver.insert(collection, values)
                ?: throw IllegalStateException("Failed to create MediaStore image entry")

            resolver.openOutputStream(itemUri)?.use { outStream ->
                FileInputStream(sourceFile).use { inStream ->
                    inStream.copyTo(outStream)
                }
                outStream.flush()
            } ?: throw IllegalStateException("Failed to open output stream for MediaStore entry")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }

            AppLogger.logResult("SaveImageToGallery", itemUri.toString(), sourceFile.absolutePath, sourceFile.length())
            Result.success(itemUri)
        } catch (e: Exception) {
            AppLogger.logFailed("SaveImageToGallery", e)
            Result.failure(e)
        }
    }

    /**
     * Encodes and saves a Bitmap to both internal export and MediaStore gallery.
     */
    suspend fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 90
    ): Result<Pair<File, Uri>> = withContext(Dispatchers.IO) {
        try {
            val mimeType = when (format) {
                Bitmap.CompressFormat.PNG -> "image/png"
                Bitmap.CompressFormat.WEBP,
                Bitmap.CompressFormat.WEBP_LOSSY,
                Bitmap.CompressFormat.WEBP_LOSSLESS -> "image/webp"
                else -> "image/jpeg"
            }
            val ext = getExtensionFromMime(mimeType)
            val exportDir = MediaProcessor.getExportDirectory(context)
            val localFile = File(exportDir, "${displayName}_${System.currentTimeMillis()}.$ext")

            FileOutputStream(localFile).use { fos ->
                bitmap.compress(format, quality.coerceIn(1, 100), fos)
                fos.flush()
            }

            if (!localFile.exists() || localFile.length() == 0L) {
                throw IllegalStateException("Failed to write bitmap to local storage")
            }

            val galleryResult = saveImageToGallery(context, localFile, localFile.name, mimeType)
            if (galleryResult.isSuccess) {
                Result.success(Pair(localFile, galleryResult.getOrThrow()))
            } else {
                // If media store insert failed, internal export is still valid
                val internalUri = Uri.fromFile(localFile)
                Result.success(Pair(localFile, internalUri))
            }
        } catch (e: Exception) {
            AppLogger.logFailed("SaveBitmapToGallery", e)
            Result.failure(e)
        }
    }

    /**
     * Saves a video file to Movies/CreatorKit in MediaStore.
     */
    suspend fun saveVideoToGallery(
        context: Context,
        sourceVideoFile: File,
        displayName: String,
        mimeType: String = "video/mp4"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            if (!sourceVideoFile.exists() || sourceVideoFile.length() == 0L) {
                throw IllegalStateException("Source video file does not exist: ${sourceVideoFile.absolutePath}")
            }

            val filename = if (displayName.endsWith(".mp4")) displayName else "$displayName.mp4"
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, filename)
                put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CreatorKit")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val itemUri = resolver.insert(collection, values)
                ?: throw IllegalStateException("Failed to create MediaStore video entry")

            resolver.openOutputStream(itemUri)?.use { outStream ->
                FileInputStream(sourceVideoFile).use { inStream ->
                    inStream.copyTo(outStream)
                }
                outStream.flush()
            } ?: throw IllegalStateException("Failed to open output stream for video MediaStore entry")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }

            AppLogger.logResult("SaveVideoToGallery", itemUri.toString(), sourceVideoFile.absolutePath, sourceVideoFile.length())
            Result.success(itemUri)
        } catch (e: Exception) {
            AppLogger.logFailed("SaveVideoToGallery", e)
            Result.failure(e)
        }
    }

    /**
     * Saves a document (PDF, TXT, SRT, VTT, etc.) to MediaStore Downloads / Documents.
     */
    suspend fun saveDocumentToDownloads(
        context: Context,
        sourceFile: File,
        displayName: String,
        mimeType: String = "application/pdf"
    ): Result<Uri> = saveDocumentToStorage(context, sourceFile, displayName, mimeType)

    /**
     * Saves a PDF or subtitle document to MediaStore Downloads / Documents.
     */
    suspend fun saveDocumentToStorage(
        context: Context,
        sourceFile: File,
        displayName: String,
        mimeType: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists() || sourceFile.length() == 0L) {
                throw IllegalStateException("Source document file does not exist: ${sourceFile.absolutePath}")
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/CreatorKit")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Files.getContentUri("external")
            }

            val itemUri = resolver.insert(collection, values)
                ?: throw IllegalStateException("Failed to create MediaStore document entry")

            resolver.openOutputStream(itemUri)?.use { outStream ->
                FileInputStream(sourceFile).use { inStream ->
                    inStream.copyTo(outStream)
                }
                outStream.flush()
            } ?: throw IllegalStateException("Failed to open output stream for document MediaStore entry")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }

            AppLogger.logResult("SaveDocumentToStorage", itemUri.toString(), sourceFile.absolutePath, sourceFile.length())
            Result.success(itemUri)
        } catch (e: Exception) {
            AppLogger.logFailed("SaveDocumentToStorage", e)
            Result.failure(e)
        }
    }

    /**
     * Shares a file securely using FileProvider and ACTION_SEND.
     * Prevents crashes by checking for available receiving activities and granting URI read permissions.
     */
    fun shareMediaFile(
        context: Context,
        file: File,
        mimeType: String,
        chooserTitle: String = "Share"
    ): Result<Unit> {
        return try {
            if (!file.exists() || file.length() == 0L) {
                throw IllegalStateException("Cannot share missing or empty file: ${file.absolutePath}")
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)
            AppLogger.logSuccess("ShareMediaFile", "Dispatched share intent for ${file.name} ($mimeType)")
            Result.success(Unit)
        } catch (e: ActivityNotFoundException) {
            AppLogger.logFailed("ShareMediaFile", e, "No app found to handle sharing")
            Toast.makeText(context, "No compatible app was found to share this file.", Toast.LENGTH_LONG).show()
            Result.failure(e)
        } catch (e: Exception) {
            AppLogger.logFailed("ShareMediaFile", e)
            Toast.makeText(context, "Sharing failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            Result.failure(e)
        }
    }

    fun getExtensionFromMime(mimeType: String): String {
        return when (mimeType.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/jpeg", "image/jpg" -> "jpg"
            "video/mp4" -> "mp4"
            "application/pdf" -> "pdf"
            "text/plain", "text/vtt" -> "vtt"
            "application/x-subrip" -> "srt"
            else -> "bin"
        }
    }
}
