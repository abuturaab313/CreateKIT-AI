package com.example.ui.tools

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ToolType
import com.example.engine.VideoInfo
import com.example.engine.VideoPreset
import com.example.engine.processor.AppLogger
import com.example.engine.processor.MediaProcessor
import com.example.engine.processor.MediaStorageManager
import com.example.engine.processor.RealVideoProcessResult
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun VideoCompressorScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var videoInfo by remember { mutableStateOf<VideoInfo?>(null) }
    var selectedPreset by remember { mutableStateOf(VideoPreset.WHATSAPP) }

    var trimRange by remember { mutableStateOf(0f..10f) }
    var isProcessing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var processResult by remember { mutableStateOf<RealVideoProcessResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var savedSuccess by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            processResult = null
            savedSuccess = false
            errorMessage = null
            saveError = null
            val info = MediaProcessor.video.inspect(context, uri)
            videoInfo = info
            if (info != null) {
                val durSec = (info.durationMs / 1000f).coerceAtLeast(1f)
                trimRange = 0f..durSec
                AppLogger.logStart("VideoCompressor", uri.toString(), "video/mp4", info.fileSizeBytes)
            }
        }
    }

    fun formatSeconds(sec: Float): String {
        val totalSec = sec.toInt()
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format("%02d:%02d", m, s)
    }

    fun startTrimAndExport() {
        val uri = selectedUri ?: return
        val info = videoInfo ?: return

        isProcessing = true
        errorMessage = null
        saveError = null
        savedSuccess = false

        scope.launch {
            try {
                val exportDir = MediaProcessor.getExportDirectory(context)
                val exportFile = File(exportDir, "video_trimmed_${System.currentTimeMillis()}.mp4")

                val startMs = (trimRange.start * 1000).toLong().coerceAtLeast(0L)
                val endMs = (trimRange.endInclusive * 1000).toLong().coerceAtMost(info.durationMs)

                AppLogger.logProcessing("VideoCompressor", "MediaProcessor.video.trimVideo", "from ${startMs}ms to ${endMs}ms")
                val result = MediaProcessor.video.trimVideo(
                    context = context,
                    srcUri = uri,
                    startMs = startMs,
                    endMs = endMs,
                    outputFile = exportFile
                )

                processResult = result
                isProcessing = false
                AppLogger.logSuccess("VideoCompressor", "Exported ${AppLogger.formatBytes(result.outputSizeBytes)} (${result.outputDurationMs}ms)")
            } catch (e: Exception) {
                isProcessing = false
                AppLogger.logFailed("VideoCompressor", e)
                errorMessage = "Video processing failed: ${e.localizedMessage ?: "Format not supported"}"
            }
        }
    }

    fun saveToGallery(onComplete: ((File?) -> Unit)? = null) {
        val result = processResult ?: return
        isSaving = true
        saveError = null

        scope.launch {
            try {
                val saveRes = MediaStorageManager.saveVideoToGallery(
                    context = context,
                    sourceVideoFile = result.file,
                    displayName = result.file.name,
                    mimeType = "video/mp4"
                )

                if (saveRes.isSuccess) {
                    viewModel.saveProject(
                        title = "Trimmed Video (${result.width}×${result.height})",
                        tool = ToolType.VIDEO_COMPRESSOR,
                        outputFile = result.file,
                        previewBitmap = videoInfo?.thumbnail,
                        width = result.width,
                        height = result.height,
                        format = "MP4"
                    )
                    savedSuccess = true
                    isSaving = false
                    Toast.makeText(context, "Saved to Videos Gallery!", Toast.LENGTH_SHORT).show()
                    onComplete?.invoke(result.file)
                } else {
                    val err = saveRes.exceptionOrNull()?.localizedMessage ?: "Failed to save video"
                    saveError = err
                    isSaving = false
                    Toast.makeText(context, "Save failed: $err", Toast.LENGTH_SHORT).show()
                    onComplete?.invoke(null)
                }
            } catch (e: Exception) {
                isSaving = false
                saveError = e.localizedMessage
                AppLogger.logFailed("SaveVideoToGallery", e)
                Toast.makeText(context, "Save error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                onComplete?.invoke(null)
            }
        }
    }

    fun performShare() {
        val result = processResult
        if (result != null && result.file.exists()) {
            MediaStorageManager.shareMediaFile(
                context = context,
                file = result.file,
                mimeType = "video/mp4",
                chooserTitle = "Share Trimmed Video"
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("video_comp_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Video Trim & Compress",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (videoInfo != null) {
                    TextButton(
                        onClick = { videoPicker.launch("video/*") },
                        modifier = Modifier.testTag("video_change_button")
                    ) {
                        Text("Change", color = ElectricCyan)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (videoInfo == null) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .testTag("video_upload_card"),
                        onClick = { videoPicker.launch("video/*") }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(ElectricCyan.copy(alpha = 0.2f), NeonViolet.copy(alpha = 0.2f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Video",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Select Video to Trim & Optimize",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Native hardware re-muxing for WhatsApp, Reels & YouTube Shorts",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val info = videoInfo!!

                    // Video Thumbnail Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (info.thumbnail != null) {
                            Image(
                                bitmap = info.thumbnail.asImageBitmap(),
                                contentDescription = "Video Frame",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Default.VideoFile, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(56.dp))
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.8f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "${info.width}×${info.height} • ${formatSeconds(info.durationMs / 1000f)}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Video Trimmer Controls
                    val maxSec = (info.durationMs / 1000f).coerceAtLeast(1f)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ContentCut, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("TRIM SEGMENT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Text(
                                    text = "${formatSeconds(trimRange.start)} → ${formatSeconds(trimRange.endInclusive)} (${formatSeconds(trimRange.endInclusive - trimRange.start)})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            RangeSlider(
                                value = trimRange,
                                onValueChange = {
                                    trimRange = it
                                    processResult = null
                                    savedSuccess = false
                                },
                                valueRange = 0f..maxSec,
                                colors = SliderDefaults.colors(
                                    thumbColor = ElectricCyan,
                                    activeTrackColor = ElectricCyan,
                                    inactiveTrackColor = Color.DarkGray
                                ),
                                modifier = Modifier.testTag("video_trim_slider")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Result or Presets
                    if (processResult != null) {
                        val res = processResult!!
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DarkSurfaceElevated,
                            border = BorderStroke(1.dp, NeonEmerald),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("OUTPUT FILE READY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonEmerald)
                                        Text(AppLogger.formatBytes(res.outputSizeBytes), fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                                        Text("Duration: ${formatSeconds(res.outputDurationMs / 1000f)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = NeonEmerald.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, NeonEmerald)
                                    ) {
                                        Text(
                                            text = "GENUINE MP4",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonEmerald,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (saveError != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Save failed: $saveError",
                                color = Color(0xFFFF5252),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { saveToGallery() },
                                enabled = !isSaving,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("save_video_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald)
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Saving...", color = Color.Black, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        imageVector = if (savedSuccess) Icons.Default.Check else Icons.Default.Download,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (savedSuccess) "Saved!" else "Save to Gallery",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Button(
                                onClick = { performShare() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("share_video_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonViolet)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Video", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Presets
                        Text(
                            text = "SELECT VIDEO PRESET",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(VideoPreset.values()) { preset ->
                                val isChosen = selectedPreset == preset
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isChosen) ElectricCyan else DarkSurfaceElevated,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { selectedPreset = preset }
                                        .testTag("video_preset_${preset.name}")
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                        Text(
                                            text = preset.label,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isChosen) Color.Black else Color.White
                                        )
                                        Text(
                                            text = "${preset.targetResolution} • ${preset.targetBitrateKbps} kbps",
                                            fontSize = 11.sp,
                                            color = if (isChosen) Color.Black.copy(alpha = 0.8f) else Color.LightGray
                                        )
                                    }
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFFF5252),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { startTrimAndExport() },
                            enabled = !isProcessing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("trim_export_video_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Trimming & Remuxing Video...", color = Color.Black, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Movie, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Trim & Export Video (.mp4)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}
