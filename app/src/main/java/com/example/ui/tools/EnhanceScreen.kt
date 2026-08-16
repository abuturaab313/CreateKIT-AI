package com.example.ui.tools

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.core.content.FileProvider
import com.example.data.model.ToolType
import com.example.engine.AiResult
import com.example.engine.EnhanceType
import com.example.engine.ImageProcessor
import com.example.ui.MainViewModel
import com.example.ui.components.BeforeAfterSlider
import com.example.ui.components.CreditDialog
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.GlassCard
import com.example.ui.components.ProcessingOverlay
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonViolet
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun EnhanceScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var enhancedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedMode by remember { mutableStateOf(EnhanceType.AUTO) }

    var isProcessing by remember { mutableStateOf(false) }
    var progressVal by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreditDialog by remember { mutableStateOf(false) }
    var savedSuccess by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            errorMessage = null
            enhancedBitmap = null
            savedSuccess = false
            originalBitmap = ImageProcessor.loadBitmapFromUri(context, uri, 1920)
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
        statusText = "Initializing AI model..."

        scope.launch {
            val result = viewModel.cloudAiClient.enhanceImageWithAi(bmp, selectedMode) { p, stage ->
                progressVal = p
                statusText = stage
            }

            isProcessing = false
            when (result) {
                is AiResult.Success -> {
                    enhancedBitmap = result.data
                }
                is AiResult.Error -> {
                    errorMessage = result.message
                }
            }
        }
    }

    fun saveAndShare(share: Boolean = false) {
        val result = enhancedBitmap ?: return
        scope.launch {
            val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
            val exportFile = File(exportDir, "enhanced_${System.currentTimeMillis()}.jpg")
            val fos = FileOutputStream(exportFile)
            result.compress(Bitmap.CompressFormat.JPEG, 95, fos)
            fos.flush()
            fos.close()

            viewModel.saveProject(
                title = "Enhanced (${selectedMode.displayName})",
                tool = ToolType.AI_ENHANCE,
                outputFile = exportFile,
                previewBitmap = result,
                width = result.width,
                height = result.height,
                format = "JPG"
            )
            savedSuccess = true

            if (share) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", exportFile)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Enhanced Photo"))
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
                    // Upload Placeholder Box
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
                    // Preview Area (Before/After Slider if enhanced, or Original)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
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
                                    .clickable { selectedMode = mode }
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

                    Spacer(modifier = Modifier.height(24.dp))

                    // Error Banner if failed
                    if (errorMessage != null) {
                        ErrorStateCard(
                            errorMessage = errorMessage!!,
                            onRetry = { startEnhance() },
                            onCancel = { errorMessage = null },
                            onReportProblem = { /* Open support */ }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Action Buttons
                    if (enhancedBitmap == null) {
                        Button(
                            onClick = { startEnhance() },
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
                                text = "Enhance with AI (1 Credit)",
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
                                onClick = { saveAndShare(share = false) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("save_enhance_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                            ) {
                                Icon(
                                    imageVector = if (savedSuccess) Icons.Default.Check else Icons.Default.Download,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (savedSuccess) "Saved!" else "Save Project",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { saveAndShare(share = true) },
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
                            Text("Share to Social Media", color = Color.White, fontWeight = FontWeight.Bold)
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
