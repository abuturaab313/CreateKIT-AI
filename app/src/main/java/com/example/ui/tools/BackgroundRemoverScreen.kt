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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.engine.AiResult
import com.example.engine.BgType
import com.example.engine.processor.AppLogger
import com.example.engine.processor.MediaProcessor
import com.example.engine.processor.MediaStorageManager
import com.example.ui.MainViewModel
import com.example.ui.components.CheckerboardBackground
import com.example.ui.components.DiagnosticInfo
import com.example.ui.components.DiagnosticPanel
import com.example.ui.components.CreditDialog
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.GlassCard
import com.example.ui.components.ProcessingOverlay
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import kotlinx.coroutines.launch
import java.io.File
import android.graphics.Color as AndroidColor

@Composable
fun BackgroundRemoverScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedInputUri by remember { mutableStateOf<Uri?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var bgType by remember { mutableStateOf(BgType.TRANSPARENT) }
    var selectedColor by remember { mutableIntStateOf(AndroidColor.WHITE) }
    var customBgBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var addShadow by remember { mutableStateOf(false) }

    var isProcessing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var progressVal by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreditDialog by remember { mutableStateOf(false) }
    var lastSavedFile by remember { mutableStateOf<File?>(null) }
    var savedSuccess by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var diagnosticInfo by remember { mutableStateOf(DiagnosticInfo()) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedInputUri = uri
            errorMessage = null
            processedBitmap = null
            lastSavedFile = null
            savedSuccess = false
            saveError = null
            val inputSize = MediaProcessor.image.getFileSizeFromUri(context, uri)
            val mime = context.contentResolver.getType(uri) ?: "image/*"
            AppLogger.logStart("RemoveBackground", uri.toString(), mime, inputSize)
            val loaded = MediaProcessor.image.loadBitmapFromUri(context, uri, 1920)
            originalBitmap = loaded
            diagnosticInfo = DiagnosticInfo(
                selectedUri = uri.toString(),
                inputMime = mime,
                inputSize = "${inputSize / 1024} KB",
                inputDimensions = if (loaded != null) "${loaded.width}x${loaded.height}" else "Unknown",
                processor = "CreatorKit Foreground Engine",
                processingState = "Image Loaded"
            )
        }
    }

    val customBgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            customBgBitmap = MediaProcessor.image.loadBitmapFromUri(context, uri, 1920)
            bgType = BgType.CUSTOM_IMAGE
        }
    }

    val solidColorList = listOf(
        "White" to AndroidColor.WHITE,
        "Black" to AndroidColor.BLACK,
        "Cyan" to 0xFF00E5FF.toInt(),
        "Amber" to 0xFFFFB703.toInt(),
        "Purple" to 0xFF8B5CF6.toInt(),
        "Red" to 0xFFFF0055.toInt(),
        "Slate" to 0xFF334155.toInt()
    )

    fun startProcessing() {
        val src = originalBitmap ?: return
        if (!viewModel.useAiCredit()) {
            showCreditDialog = true
            return
        }

        isProcessing = true
        errorMessage = null
        progressVal = 0.15f
        statusText = "Segmenting foreground subject..."

        scope.launch {
            AppLogger.logProcessing("RemoveBackground", "MediaProcessor.ai.removeBackground", "bgType=$bgType shadow=$addShadow")
            val result = viewModel.cloudAiClient.removeBackgroundWithAi(
                bitmap = src,
                bgType = bgType,
                solidColor = selectedColor,
                customBg = customBgBitmap,
                feather = 2f,
                addShadow = addShadow
            ) { p, stage ->
                progressVal = p
                statusText = stage
            }

            isProcessing = false
            when (result) {
                is AiResult.Success -> {
                    val bmp = result.data
                    processedBitmap = bmp
                    savedSuccess = false
                    saveError = null

                    // Verify alpha channel & transparent pixels count
                    val hasAlpha = bmp.hasAlpha()
                    var hasTransparentPixels = false
                    val checkSampleStep = max(1, (bmp.width * bmp.height) / 1000)
                    for (y in 0 until bmp.height step 10) {
                        for (x in 0 until bmp.width step 10) {
                            val pixel = bmp.getPixel(x, y)
                            if (AndroidColor.alpha(pixel) < 50) {
                                hasTransparentPixels = true
                                break
                            }
                        }
                        if (hasTransparentPixels) break
                    }

                    diagnosticInfo = diagnosticInfo.copy(
                        processingState = "Completed (${result.latencyMs}ms)",
                        outputDimensions = "${bmp.width}x${bmp.height}",
                        outputMime = if (bgType == BgType.TRANSPARENT) "image/png" else "image/jpeg",
                        hasAlpha = hasAlpha,
                        transparentPixelsDetected = hasTransparentPixels,
                        maskGenerated = true,
                        alphaApplied = true
                    )
                    AppLogger.logSuccess("RemoveBackground", "Segmented successfully (${result.latencyMs}ms, hasAlpha=$hasAlpha, transparentPixels=$hasTransparentPixels)")
                }
                is AiResult.Error -> {
                    errorMessage = result.message
                    diagnosticInfo = diagnosticInfo.copy(processingState = "Error: ${result.message}")
                    AppLogger.logFailed("RemoveBackground", RuntimeException(result.message))
                }
            }
        }
    }

    fun performSave(onComplete: ((File?) -> Unit)? = null) {
        val result = processedBitmap ?: return
        isSaving = true
        saveError = null

        scope.launch {
            val isTransparent = (bgType == BgType.TRANSPARENT)
            val format = if (isTransparent) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            val ext = if (isTransparent) "png" else "jpg"
            val title = "cutout_${System.currentTimeMillis()}"

            val saveResult = MediaStorageManager.saveBitmapToGallery(
                context = context,
                bitmap = result,
                displayName = title,
                format = format,
                quality = 100
            )

            if (saveResult.isSuccess) {
                val (file, galleryUri) = saveResult.getOrThrow()
                lastSavedFile = file
                viewModel.saveProject(
                    title = if (isTransparent) "Transparent Cutout" else "Background Replacement",
                    tool = ToolType.BACKGROUND_REMOVER,
                    outputFile = file,
                    previewBitmap = result,
                    width = result.width,
                    height = result.height,
                    format = ext.uppercase()
                )
                savedSuccess = true
                isSaving = false
                diagnosticInfo = diagnosticInfo.copy(
                    outputUri = galleryUri.toString(),
                    outputSize = "${file.length() / 1024} KB",
                    saveStatus = "Saved to Gallery (${file.name})"
                )
                Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                onComplete?.invoke(file)
            } else {
                val err = saveResult.exceptionOrNull()?.localizedMessage ?: "Failed to save image"
                saveError = err
                isSaving = false
                diagnosticInfo = diagnosticInfo.copy(saveStatus = "Failed: $err")
                Toast.makeText(context, "Save failed: $err", Toast.LENGTH_SHORT).show()
                onComplete?.invoke(null)
            }
        }
    }

    fun performShare() {
        val cachedFile = lastSavedFile
        val isTransparent = (bgType == BgType.TRANSPARENT)
        val mime = if (isTransparent) "image/png" else "image/jpeg"

        if (cachedFile != null && cachedFile.exists()) {
            diagnosticInfo = diagnosticInfo.copy(shareStatus = "Shared existing file")
            MediaStorageManager.shareMediaFile(context, cachedFile, mime, "Share Cutout")
        } else {
            performSave { file ->
                if (file != null && file.exists()) {
                    diagnosticInfo = diagnosticInfo.copy(shareStatus = "Exported & Shared file")
                    MediaStorageManager.shareMediaFile(context, file, mime, "Share Cutout")
                }
            }
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
            // Top Bar
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
                        modifier = Modifier.testTag("bg_remover_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Background Remover",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (originalBitmap != null) {
                    TextButton(
                        onClick = { photoPicker.launch("image/*") },
                        modifier = Modifier.testTag("bg_change_photo_button")
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
                            .testTag("bg_upload_card"),
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
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = "Remove Background",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Choose Image for Cutout",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Instantly isolate people, products, animals or objects",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Preview Frame
                    val aspect = (originalBitmap!!.width.toFloat() / originalBitmap!!.height.coerceAtLeast(1).toFloat()).coerceIn(0.6f, 2.2f)
                    val showCheckerboard = (bgType == BgType.TRANSPARENT)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspect)
                            .clip(RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (showCheckerboard) {
                            CheckerboardBackground(
                                modifier = Modifier.fillMaxSize(),
                                squareSize = 12.dp
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(DarkSurface))
                        }

                        val displayBmp = processedBitmap ?: originalBitmap
                        if (displayBmp != null) {
                            Image(
                                bitmap = displayBmp.asImageBitmap(),
                                contentDescription = "Cutout Preview",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Background Options
                    Text(
                        text = "NEW BACKGROUND STYLE",
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
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (bgType == BgType.TRANSPARENT) ElectricCyan else DarkSurfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    bgType = BgType.TRANSPARENT
                                    processedBitmap = null
                                }
                                .testTag("bg_transparent_toggle")
                        ) {
                            Text(
                                text = "Transparent",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bgType == BgType.TRANSPARENT) Color.Black else Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (bgType == BgType.CUSTOM_COLOR || bgType == BgType.WHITE || bgType == BgType.BLACK) ElectricCyan else DarkSurfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    bgType = BgType.CUSTOM_COLOR
                                    processedBitmap = null
                                }
                                .testTag("bg_solid_toggle")
                        ) {
                            Text(
                                text = "Solid Color",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bgType == BgType.CUSTOM_COLOR || bgType == BgType.WHITE || bgType == BgType.BLACK) Color.Black else Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (bgType == BgType.CUSTOM_IMAGE) ElectricCyan else DarkSurfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    processedBitmap = null
                                    customBgPicker.launch("image/*")
                                }
                                .testTag("bg_custom_image_toggle")
                        ) {
                            Text(
                                text = "Image BG",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bgType == BgType.CUSTOM_IMAGE) Color.Black else Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }

                    // Color palette if Solid Color selected
                    if (bgType == BgType.CUSTOM_COLOR || bgType == BgType.WHITE || bgType == BgType.BLACK) {
                        Spacer(modifier = Modifier.height(14.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(solidColorList) { (name, colorInt) ->
                                val isChosen = selectedColor == colorInt
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorInt))
                                        .clickable {
                                            selectedColor = colorInt
                                            bgType = BgType.CUSTOM_COLOR
                                            processedBitmap = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isChosen) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = name,
                                            tint = if (colorInt == AndroidColor.WHITE) Color.Black else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Drop Shadow & Edge Refinement
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurfaceElevated)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Add Realistic Studio Drop Shadow",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Switch(
                            checked = addShadow,
                            onCheckedChange = {
                                addShadow = it
                                processedBitmap = null
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ElectricCyan,
                                checkedTrackColor = DarkSurface
                            )
                        )
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

                    Spacer(modifier = Modifier.height(16.dp))

                    DiagnosticPanel(info = diagnosticInfo)

                    Spacer(modifier = Modifier.height(20.dp))

                    if (errorMessage != null) {
                        ErrorStateCard(
                            errorMessage = errorMessage!!,
                            onRetry = { startProcessing() },
                            onCancel = { errorMessage = null }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (processedBitmap == null) {
                        Button(
                            onClick = { startProcessing() },
                            enabled = !isProcessing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("start_bg_remove_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Remove Background",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { startProcessing() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Re-Process", color = Color.White)
                            }

                            Button(
                                onClick = { performSave() },
                                enabled = !isSaving,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("save_bg_cutout_button"),
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
                                        text = if (savedSuccess) "Saved!" else "Save PNG",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Cutout", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        ProcessingOverlay(
            isProcessing = isProcessing,
            progress = progressVal,
            statusText = statusText,
            onCancel = { isProcessing = false }
        )

        if (showCreditDialog) {
            CreditDialog(
                remainingCredits = viewModel.aiCredits.value,
                isPremium = viewModel.isPremium.value,
                onDismiss = { showCreditDialog = false },
                onWatchAdForCredits = { viewModel.addBonusCredits(2) },
                onUpgradeClick = {
                    showCreditDialog = false
                    viewModel.navigateTo("premium")
                }
            )
        }
    }
}
