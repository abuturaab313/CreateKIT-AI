package com.example.ui.tools

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.example.engine.ImageProcessor
import com.example.ui.MainViewModel
import com.example.ui.components.BeforeAfterSlider
import com.example.ui.components.BrushCanvas
import com.example.ui.components.CreditDialog
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.GlassCard
import com.example.ui.components.ProcessingOverlay
import com.example.ui.components.StrokePath
import com.example.ui.components.generateMaskBitmap
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonViolet
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun ObjectRemoverScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cleanedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val strokes = remember { mutableStateListOf<StrokePath>() }
    var brushRadius by remember { mutableFloatStateOf(36f) }

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
            errorMessage = null
            cleanedBitmap = null
            strokes.clear()
            savedSuccess = false
            originalBitmap = ImageProcessor.loadBitmapFromUri(context, uri, 1920)
        }
    }

    fun startInpainting() {
        val src = originalBitmap ?: return
        if (strokes.isEmpty()) return

        if (!viewModel.useAiCredit()) {
            showCreditDialog = true
            return
        }

        isProcessing = true
        errorMessage = null
        progressVal = 0.15f
        statusText = "Analyzing brush mask..."

        scope.launch {
            // Generate mask matching src dimensions
            val mask = generateMaskBitmap(
                width = src.width,
                height = src.height,
                viewWidth = 800f,
                viewHeight = 600f,
                strokes = strokes.toList()
            )

            val result = viewModel.cloudAiClient.removeObjectWithAi(src, mask) { p, stage ->
                progressVal = p
                statusText = stage
            }

            isProcessing = false
            when (result) {
                is AiResult.Success -> {
                    cleanedBitmap = result.data
                }
                is AiResult.Error -> {
                    errorMessage = result.message
                }
            }
        }
    }

    fun saveAndShare(share: Boolean = false) {
        val result = cleanedBitmap ?: return
        scope.launch {
            val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
            val exportFile = File(exportDir, "object_removed_${System.currentTimeMillis()}.jpg")
            val fos = FileOutputStream(exportFile)
            result.compress(Bitmap.CompressFormat.JPEG, 95, fos)
            fos.flush()
            fos.close()

            viewModel.saveProject(
                title = "Object Removed",
                tool = ToolType.OBJECT_REMOVER,
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
                context.startActivity(Intent.createChooser(intent, "Share Cleaned Photo"))
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
                        modifier = Modifier.testTag("obj_remover_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Object Eraser",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (originalBitmap != null) {
                    TextButton(
                        onClick = { photoPicker.launch("image/*") },
                        modifier = Modifier.testTag("obj_change_photo_button")
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
                            .testTag("obj_upload_card"),
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
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = "Erase Objects",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Select Image to Clean",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Brush over unwanted people, photobombers, wires or clutter",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Canvas / Comparison Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkSurface)
                    ) {
                        if (cleanedBitmap != null) {
                            BeforeAfterSlider(
                                beforeBitmap = originalBitmap!!,
                                afterBitmap = cleanedBitmap!!,
                                modifier = Modifier.fillMaxSize(),
                                beforeLabel = "ORIGINAL",
                                afterLabel = "OBJECT ERASED"
                            )
                        } else {
                            BrushCanvas(
                                baseBitmap = originalBitmap!!,
                                brushRadius = brushRadius,
                                strokes = strokes,
                                onAddStroke = { stroke -> strokes.add(stroke) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (cleanedBitmap == null) {
                        // Brush Size Slider & Undo Controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkSurfaceElevated)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Brush,
                                    contentDescription = "Brush Size",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${brushRadius.toInt()}px",
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }

                            Slider(
                                value = brushRadius,
                                onValueChange = { brushRadius = it },
                                valueRange = 10f..80f,
                                modifier = Modifier.width(160.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = ElectricCyan,
                                    activeTrackColor = ElectricCyan
                                )
                            )

                            Row {
                                IconButton(
                                    onClick = { if (strokes.isNotEmpty()) strokes.removeLast() },
                                    enabled = strokes.isNotEmpty()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Undo,
                                        contentDescription = "Undo",
                                        tint = if (strokes.isNotEmpty()) Color.White else Color.Gray
                                    )
                                }
                                IconButton(
                                    onClick = { strokes.clear() },
                                    enabled = strokes.isNotEmpty()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Clear",
                                        tint = if (strokes.isNotEmpty()) Color.White else Color.Gray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (errorMessage != null) {
                        ErrorStateCard(
                            errorMessage = errorMessage!!,
                            onRetry = { startInpainting() },
                            onCancel = { errorMessage = null },
                            onReportProblem = {}
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (cleanedBitmap == null) {
                        Button(
                            onClick = { startInpainting() },
                            enabled = strokes.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("start_object_erase_button"),
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
                                text = if (strokes.isEmpty()) "Brush Over Object First" else "Erase Selected Object (1 Credit)",
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
                                onClick = {
                                    cleanedBitmap = null
                                    strokes.clear()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Erase More", color = Color.White)
                            }

                            Button(
                                onClick = { saveAndShare(share = false) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("save_obj_result_button"),
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
                                    text = if (savedSuccess) "Saved!" else "Save JPG",
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
                            Text("Share Cleaned Photo", color = Color.White, fontWeight = FontWeight.Bold)
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
