package com.example.ui.tools

import android.graphics.Bitmap
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.engine.EnhanceType
import com.example.engine.processor.AppLogger
import com.example.engine.processor.MediaProcessor
import com.example.engine.processor.MediaStorageManager
import com.example.ui.MainViewModel
import com.example.ui.components.BeforeAfterSlider
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

@Composable
fun EnhanceScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedInputUri by remember { mutableStateOf<Uri?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var enhancedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedMode by remember { mutableStateOf(EnhanceType.AUTO) }

    var isProcessing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var progressVal by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreditDialog by remember { mutableStateOf(false) }
    var lastSavedFile by remember { mutableStateOf<File?>(null) }
    var savedSuccess by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedInputUri = uri
            errorMessage = null
            enhancedBitmap = null
            lastSavedFile = null
            savedSuccess = false
            saveError = null
            val inputSize = MediaProcessor.image.getFileSizeFromUri(context, uri)
            AppLogger.logStart("AiEnhance", uri.toString(), context.contentResolver.getType(uri), inputSize)
            originalBitmap = MediaProcessor.image.loadBitmapFromUri(context, uri, 1920)
        }
    }

    fun startEnhance() {
        val bmp = originalBitmap ?: return
        if (!viewModel.useAiCredit()) {
            showCreditDialog = true
            return
        }

        isProcessing = true
        errorMessage = null
        progressVal = 0.1f
        statusText = "Processing enhancement..."

        scope.launch {
            AppLogger.logProcessing("AiEnhance", "MediaProcessor.image.enhance", selectedMode.displayName)
            val result = viewModel.cloudAiClient.enhanceImageWithAi(bmp, selectedMode) { p, stage ->
                progressVal = p
                statusText = stage
            }

            isProcessing = false
            when (result) {
                is AiResult.Success -> {
                    enhancedBitmap = result.data
                    savedSuccess = false
                    saveError = null
                    AppLogger.logSuccess("AiEnhance", "Engine: ${result.modelName}, Latency: ${result.latencyMs}ms")
                }
                is AiResult.Error -> {
                    errorMessage = result.message
                    AppLogger.logFailed("AiEnhance", RuntimeException(result.message))
                }
            }
        }
    }

    fun performSave(onComplete: ((File?) -> Unit)? = null) {
        val result = enhancedBitmap ?: return
        isSaving = true
        saveError = null

        scope.launch {
            val title = "enhanced_${selectedMode.name.lowercase()}_${System.currentTimeMillis()}"
            val saveResult = MediaStorageManager.saveBitmapToGallery(
                context = context,
                bitmap = result,
                displayName = title,
                format = Bitmap.CompressFormat.JPEG,
                quality = 95
            )

            if (saveResult.isSuccess) {
                val (file, galleryUri) = saveResult.getOrThrow()
                lastSavedFile = file
                viewModel.saveProject(
                    title = "Enhanced (${selectedMode.displayName})",
                    tool = ToolType.AI_ENHANCE,
                    outputFile = file,
                    previewBitmap = result,
                    width = result.width,
                    height = result.height,
                    format = "JPG"
                )
                savedSuccess = true
                isSaving = false
                Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                onComplete?.invoke(file)
            } else {
                val err = saveResult.exceptionOrNull()?.localizedMessage ?: "Failed to save image"
                saveError = err
                isSaving = false
                Toast.makeText(context, "Save failed: $err", Toast.LENGTH_SHORT).show()
                onComplete?.invoke(null)
            }
        }
    }

    fun performShare() {
        val cachedFile = lastSavedFile
        if (cachedFile != null && cachedFile.exists()) {
            MediaStorageManager.shareMediaFile(context, cachedFile, "image/jpeg", "Share Enhanced Photo")
        } else {
            performSave { file ->
                if (file != null && file.exists()) {
                    MediaStorageManager.shareMediaFile(context, file, "image/jpeg", "Share Enhanced Photo")
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
                        modifier = Modifier.testTag("enhance_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Enhance",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (originalBitmap != null) {
                    TextButton(
                        onClick = { photoPicker.launch("image/*") },
                        modifier = Modifier.testTag("change_image_button")
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
                            .testTag("enhance_upload_card"),
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
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Select Photo",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Select Image to Enhance",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Supports JPG, PNG, WEBP up to 4K resolution",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Preview Area with Before/After Slider
                    val aspect = (originalBitmap!!.width.toFloat() / originalBitmap!!.height.coerceAtLeast(1).toFloat()).coerceIn(0.6f, 2.2f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspect)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkSurface)
                    ) {
                        if (enhancedBitmap != null) {
                            BeforeAfterSlider(
                                beforeBitmap = originalBitmap!!,
                                afterBitmap = enhancedBitmap!!,
                                modifier = Modifier.fillMaxSize(),
                                beforeLabel = "ORIGINAL",
                                afterLabel = "AI ${selectedMode.displayName.uppercase()}"
                            )
                        } else {
                            Image(
                                bitmap = originalBitmap!!.asImageBitmap(),
                                contentDescription = "Original Photo",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Enhancement Mode Pills
                    Text(
                        text = "SELECT ENHANCEMENT ENGINE",
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
                        items(EnhanceType.values()) { mode ->
                            val isSelected = selectedMode == mode
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) ElectricCyan else DarkSurfaceElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) ElectricCyan else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        selectedMode = mode
                                        enhancedBitmap = null
                                    }
                                    .testTag("enhance_mode_${mode.name}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = mode.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = selectedMode.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )

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

                    // Error Banner if failed
                    if (errorMessage != null) {
                        ErrorStateCard(
                            errorMessage = errorMessage!!,
                            onRetry = { startEnhance() },
                            onCancel = { errorMessage = null }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Action Buttons
                    if (enhancedBitmap == null) {
                        Button(
                            onClick = { startEnhance() },
                            enabled = !isProcessing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("start_enhance_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enhance with AI",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    } else {
                        // Success Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { startEnhance() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Re-Enhance", color = Color.White)
                            }

                            Button(
                                onClick = { performSave() },
                                enabled = !isSaving,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("save_enhance_button"),
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
                                        text = if (savedSuccess) "Saved!" else "Save to Gallery",
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
                                .height(52.dp)
                                .testTag("share_enhance_button"),
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
                            Text("Share Enhanced Photo", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Processing Overlay
        ProcessingOverlay(
            isProcessing = isProcessing,
            progress = progressVal,
            statusText = statusText,
            onCancel = { isProcessing = false }
        )

        // Credit Dialog
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
