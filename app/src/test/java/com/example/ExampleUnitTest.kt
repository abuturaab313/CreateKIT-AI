package com.example

import com.example.engine.processor.AppLogger
import com.example.engine.processor.MediaStorageManager
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testFormatBytes() {
        assertEquals("500 B", AppLogger.formatBytes(500))
        assertEquals("1.00 KB", AppLogger.formatBytes(1024))
        assertEquals("1.00 MB", AppLogger.formatBytes(1024 * 1024))
        assertEquals("1.00 GB", AppLogger.formatBytes(1024L * 1024 * 1024))
    }

    @Test
    fun testMimeExtensions() {
        assertEquals("png", MediaStorageManager.getExtensionFromMime("image/png"))
        assertEquals("webp", MediaStorageManager.getExtensionFromMime("image/webp"))
        assertEquals("jpg", MediaStorageManager.getExtensionFromMime("image/jpeg"))
        assertEquals("mp4", MediaStorageManager.getExtensionFromMime("video/mp4"))
        assertEquals("pdf", MediaStorageManager.getExtensionFromMime("application/pdf"))
    }
}
