package com.example.ui.tools

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.engine.PdfEngine
import com.example.engine.processor.AppLogger
import com.example.engine.processor.MediaProcessor
import com.example.engine.processor.MediaStorageManager
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class TargetFormat(val label: String, val ext: String, val mime: String) {
    PNG("PNG", "png", "image/png"),
    JPG("JPG", "jpg", "image/jpeg"),
    WEBP("WEBP", "webp", "image/webp"),
    PDF("PDF Document", "pdf", "application/pdf")
}

@Composable
fun FormatConverterScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedInputUri by remember { mutableStateOf<Uri?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedTarget by remember { mutableStateOf(TargetFormat.PNG) }
    var convertedFile by remember { mutableStateOf<File?>(null) }
    var isConverting by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var savedSuccess by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedInputUri = uri
            convertedFile = null
            savedSuccess = false
            saveError = null
            val inputSize = MediaProcessor.image.getFileSizeFromUri(context, uri)
            AppLogger.logStart("FormatConverter", uri.toString(), context.contentResolver.getType(uri), inputSize)
            originalBitmap = MediaProcessor.image.loadBitmapFromUri(context, uri, 3840)
        }
    }

    fun convertFormat() {
        val src = originalBitmap ?: return
        isConverting = true
        saveError = null
        savedSuccess = false

        scope.launch {
            try {
                val exportDir = MediaProcessor.getExportDirectory(context)
                val exportFile = File(exportDir, "converted_${System.currentTimeMillis()}.${selectedTarget.ext}")

                AppLogger.logProcessing("FormatConverter", "Conversion", "Target format: ${selectedTarget.label}")
                if (selectedTarget == TargetFormat.PDF) {
                    PdfEngine.createPdfFromBitmaps(listOf(src), exportFile)
                } else {
                    val format = when (selectedTarget) {
                        TargetFormat.PNG -> Bitmap.CompressFormat.PNG
                        TargetFormat.JPG -> Bitmap.CompressFormat.JPEG
                        TargetFormat.WEBP -> Bitmap.CompressFormat.WEBP
                        else -> Bitmap.CompressFormat.PNG
                    }
                    val fos = FileOutputStream(exportFile)
                    src.compress(format, 95, fos)
                    fos.flush()
                    fos.close()
                }

                convertedFile = exportFile
                isConverting = false
                AppLogger.logSuccess("FormatConverter", "Converted to ${exportFile.name} (${AppLogger.formatBytes(exportFile.length())})")
            } catch (e: Exception) {
                isConverting = false
                AppLogger.logFailed("FormatConverter", e)
                Toast.makeText(context, "Conversion failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun performSave(onComplete: ((File?) -> Unit)? = null) {
        val file = convertedFile ?: return
        isSaving = true
        saveError = null

        scope.launch {
            try {
                val result = if (selectedTarget == TargetFormat.PDF) {
                    MediaStorageManager.saveDocumentToDownloads(
                        context = context,
                        sourceFile = file,
                        displayName = file.name,
                        mimeType = selectedTarget.mime
                    )
                } else {
                    MediaStorageManager.saveImageToGallery(
                        context = context,
                        sourceFile = file,
                        displayName = file.name,
                        mimeType = selectedTarget.mime
                    )
                }

                if (result.isSuccess) {
                    viewModel.saveProject(
                        title = "Converted to ${selectedTarget.label}",
                        tool = ToolType.FORMAT_CONVERTER,
                        outputFile = file,
                        previewBitmap = originalBitmap,
                        width = originalBitmap?.width ?: 0,
                        height = originalBitmap?.height ?: 0,
                        format = selectedTarget.label
                    )
                    savedSuccess = true
                    isSaving = false
                    val destMsg = if (selectedTarget == TargetFormat.PDF) "Saved to Downloads!" else "Saved to Gallery!"
                    Toast.makeText(context, destMsg, Toast.LENGTH_SHORT).show()
                    onComplete?.invoke(file)
                } else {
                    val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to save file"
                    saveError = err
                    isSaving = false
                    Toast.makeText(context, "Save failed: $err", Toast.LENGTH_SHORT).show()
                    onComplete?.invoke(null)
                }
            } catch (e: Exception) {
                isSaving = false
                saveError = e.localizedMessage
                AppLogger.logFailed("SaveConvertedFile", e)
                Toast.makeText(context, "Save error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                onComplete?.invoke(null)
            }
        }
    }

    fun performShare() {
        val file = convertedFile
        if (file != null && file.exists()) {
            MediaStorageManager.shareMediaFile(
                context = context,
                file = file,
                mimeType = selectedTarget.mime,
                chooserTitle = "Share Converted File"
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
                        modifier = Modifier.testTag("converter_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Format Converter",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (originalBitmap != null) {
                    TextButton(
                        onClick = { photoPicker.launch("image/*") }
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
                if (originalBitmap == null) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .testTag("converter_upload_card"),
                        onClick = { photoPicker.launch("image/*") }
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
                                    imageVector = Icons.Default.Transform,
                                    contentDescription = "Convert",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Select File to Convert",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Convert between PNG, JPG, WEBP and PDF instantly on-device",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val aspect = (originalBitmap!!.width.toFloat() / originalBitmap!!.height.coerceAtLeast(1).toFloat()).coerceIn(0.6f, 2.2f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspect)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = originalBitmap!!.asImageBitmap(),
                            contentDescription = "Source File",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "CONVERT TARGET FORMAT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TargetFormat.values().forEach { fmt ->
                            val isChosen = selectedTarget == fmt
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isChosen) ElectricCyan else DarkSurfaceElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedTarget = fmt
                                        convertedFile = null
                                    }
                                    .testTag("convert_format_${fmt.name}")
                            ) {
                                Text(
                                    text = fmt.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChosen) Color.Black else Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }

                    if (saveError != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Save failed: $saveError",
                            color = Color(0xFFFF5252),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (convertedFile == null) {
                        Button(
                            onClick = { convertFormat() },
                            enabled = !isConverting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("start_convert_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                        ) {
                            if (isConverting) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Converting...", color = Color.Black, fontWeight = FontWeight.Bold)
                            } else {
                                Text(
                                    text = "Convert to ${selectedTarget.label}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = { performSave() },
                            enabled = !isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("save_converted_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
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
                                    text = if (savedSuccess) "Saved!" else "Save File",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { performShare() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonViolet)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Converted File", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
