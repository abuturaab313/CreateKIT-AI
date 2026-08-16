package com.example.ui.tools

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
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
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.engine.BgType
import com.example.engine.ImageProcessor
import com.example.ui.MainViewModel
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
fun BackgroundRemoverScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var customBgBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var bgType by remember { mutableStateOf(BgType.TRANSPARENT) }
    var selectedColor by remember { mutableStateOf(AndroidColor.WHITE) }
    var addShadow by remember { mutableStateOf(false) }
    var featherVal by remember { mutableFloatStateOf(2f) }

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
            processedBitmap = null
            savedSuccess = false
            originalBitmap = ImageProcessor.loadBitmapFromUri(context, uri, 1920)
        }
    }

    val customBgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            customBgBitmap = ImageProcessor.loadBitmapFromUri(context, uri, 1920)
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
        statusText = "Segmenting background..."

        scope.launch {
            val result = viewModel.cloudAiClient.removeBackgroundWithAi(
                bitmap = src,
                bgType = bgType,
                solidColor = selectedColor,
                customBg = customBgBitmap,
                feather = featherVal,
                addShadow = addShadow
            ) { p, stage ->
                progressVal = p
                statusText = stage
            }

            isProcessing = false
            when (result) {
                is AiResult.Success -> {
                    processedBitmap = result.data
                }
                is AiResult.Error -> {
                    errorMessage = result.message
                }
            }
        }
    }

    fun saveAndShare(share: Boolean = false) {
        val result = processedBitmap ?: return
        scope.launch {
            val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
            val isTransparent = (bgType == BgType.TRANSPARENT)
            val ext = if (isTransparent) "png" else "jpg"
            val format = if (isTransparent) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

            val exportFile = File(exportDir, "cutout_${System.currentTimeMillis()}.$ext")
            val fos = FileOutputStream(exportFile)
            result.compress(format, 100, fos)
            fos.flush()
            fos.close()

            viewModel.saveProject(
                title = if (isTransparent) "Transparent Cutout" else "Background Replacement",
                tool = ToolType.BACKGROUND_REMOVER,
                outputFile = exportFile,
                previewBitmap = result,
                width = result.width,
                height = result.height,
                format = ext.uppercase()
            )
            savedSuccess = true

            if (share) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", exportFile)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = if (isTransparent) "image/png" else "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Cutout"))
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkSurface),
                        contentAlignment = Alignment.Center
                    ) {
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
                                .clickable { bgType = BgType.TRANSPARENT }
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
                                .clickable { bgType = BgType.CUSTOM_COLOR }
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
                                .clickable { customBgPicker.launch("image/*") }
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
                                        }
                                        .then(
                                            if (isChosen) Modifier.background(
                                                ElectricCyan,
                                                shape = CircleShape
                                            ) else Modifier
                                        ),
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
                            onCheckedChange = { addShadow = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ElectricCyan,
                                checkedTrackColor = DarkSurface
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (errorMessage != null) {
                        ErrorStateCard(
                            errorMessage = errorMessage!!,
                            onRetry = { startProcessing() },
                            onCancel = { errorMessage = null },
                            onReportProblem = {}
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (processedBitmap == null) {
                        Button(
                            onClick = { startProcessing() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("start_bg_remove_button"),
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
                                text = "Remove Background (1 Credit)",
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
                                onClick = { saveAndShare(share = false) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("save_bg_cutout_button"),
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
                                    text = if (savedSuccess) "Saved!" else "Save PNG",
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
