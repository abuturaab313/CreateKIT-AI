package com.example.ui.tools

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ToolType
import com.example.engine.ShapeLayer
import com.example.engine.StickerLayer
import com.example.engine.TextLayer
import com.example.engine.ThumbnailEngine
import com.example.engine.ThumbnailTemplate
import com.example.engine.processor.AppLogger
import com.example.engine.processor.MediaProcessor
import com.example.engine.processor.MediaStorageManager
import com.example.ui.MainViewModel
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonViolet
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ThumbnailMakerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var bgBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var bgGradStart by remember { mutableStateOf(0xFF0D0B2E.toInt()) }
    var bgGradEnd by remember { mutableStateOf(0xFFFF0055.toInt()) }

    val textLayers = remember {
        mutableStateListOf<TextLayer>().apply {
            addAll(ThumbnailEngine.TEMPLATES[0].defaultTexts.map { it.copy() })
        }
    }
    val stickerLayers = remember {
        mutableStateListOf<StickerLayer>().apply {
            addAll(ThumbnailEngine.TEMPLATES[0].defaultStickers.map { it.copy() })
        }
    }
    val shapeLayers = remember {
        mutableStateListOf<ShapeLayer>().apply {
            addAll(ThumbnailEngine.TEMPLATES[0].defaultShapes.map { it.copy() })
        }
    }

    var renderedPreview by remember { mutableStateOf<Bitmap?>(null) }
    var activeTab by remember { mutableStateOf("templates") }
    var selectedTextIndex by remember { mutableStateOf(0) }
    var isSaving by remember { mutableStateOf(false) }
    var savedSuccess by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var lastExportedFile by remember { mutableStateOf<File?>(null) }

    fun refreshPreview() {
        try {
            val tempFile = File(context.cacheDir, "thumb_preview.png")
            ThumbnailEngine.renderThumbnail(
                bgBitmap = bgBitmap,
                bgGradientStart = bgGradStart,
                bgGradientEnd = bgGradEnd,
                textLayers = textLayers.toList(),
                stickers = stickerLayers.toList(),
                shapes = shapeLayers.toList(),
                outputFile = tempFile
            )
            renderedPreview = MediaProcessor.image.loadBitmapFromUri(context, Uri.fromFile(tempFile), 1280)
            savedSuccess = false
        } catch (e: Exception) {
            AppLogger.logFailed("ThumbnailMaker_refreshPreview", e)
        }
    }

    LaunchedEffect(Unit) {
        refreshPreview()
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            bgBitmap = MediaProcessor.image.loadBitmapFromUri(context, uri, 1920)
            refreshPreview()
        }
    }

    fun applyTemplate(template: ThumbnailTemplate) {
        bgGradStart = template.bgGradientStart
        bgGradEnd = template.bgGradientEnd
        textLayers.clear()
        textLayers.addAll(template.defaultTexts.map { it.copy() })
        stickerLayers.clear()
        stickerLayers.addAll(template.defaultStickers.map { it.copy() })
        shapeLayers.clear()
        shapeLayers.addAll(template.defaultShapes.map { it.copy() })
        selectedTextIndex = 0
        refreshPreview()
    }

    fun exportAndSave(onComplete: ((File?) -> Unit)? = null) {
        isSaving = true
        saveError = null

        scope.launch {
            try {
                val exportDir = MediaProcessor.getExportDirectory(context)
                val exportFile = File(exportDir, "thumbnail_1280x720_${System.currentTimeMillis()}.png")

                AppLogger.logProcessing("ThumbnailMaker", "renderThumbnail", "1280x720 PNG")
                ThumbnailEngine.renderThumbnail(
                    bgBitmap = bgBitmap,
                    bgGradientStart = bgGradStart,
                    bgGradientEnd = bgGradEnd,
                    textLayers = textLayers.toList(),
                    stickers = stickerLayers.toList(),
                    shapes = shapeLayers.toList(),
                    outputFile = exportFile
                )

                lastExportedFile = exportFile

                val saveResult = MediaStorageManager.saveImageToGallery(
                    context = context,
                    sourceFile = exportFile,
                    displayName = exportFile.name,
                    mimeType = "image/png"
                )

                if (saveResult.isSuccess) {
                    viewModel.saveProject(
                        title = "YouTube Thumbnail (1280×720)",
                        tool = ToolType.THUMBNAIL_MAKER,
                        outputFile = exportFile,
                        previewBitmap = renderedPreview,
                        width = 1280,
                        height = 720,
                        format = "PNG"
                    )
                    savedSuccess = true
                    isSaving = false
                    AppLogger.logSuccess("ThumbnailMaker", "Saved to gallery (${AppLogger.formatBytes(exportFile.length())})")
                    Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                    onComplete?.invoke(exportFile)
                } else {
                    val err = saveResult.exceptionOrNull()?.localizedMessage ?: "Failed to save thumbnail"
                    saveError = err
                    isSaving = false
                    Toast.makeText(context, "Save failed: $err", Toast.LENGTH_SHORT).show()
                    onComplete?.invoke(null)
                }
            } catch (e: Exception) {
                isSaving = false
                saveError = e.localizedMessage
                AppLogger.logFailed("ThumbnailMaker", e)
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                onComplete?.invoke(null)
            }
        }
    }

    fun performShare() {
        if (lastExportedFile != null && lastExportedFile!!.exists()) {
            MediaStorageManager.shareMediaFile(
                context = context,
                file = lastExportedFile!!,
                mimeType = "image/png",
                chooserTitle = "Share YouTube Thumbnail"
            )
        } else {
            exportAndSave { file ->
                if (file != null) {
                    MediaStorageManager.shareMediaFile(
                        context = context,
                        file = file,
                        mimeType = "image/png",
                        chooserTitle = "Share YouTube Thumbnail"
                    )
                }
            }
        }
    }

    val emojiChoices = listOf("🔥", "👑", "🤖", "⚡", "🎬", "🚀", "💡", "🚨", "🎯", "😱", "🏆", "💥")

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
                        modifier = Modifier.testTag("thumb_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thumbnail Maker",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonAmber.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, NeonAmber)
                ) {
                    Text(
                        text = "1280 × 720 HD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonAmber,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 16:9 Canvas Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurface),
                    contentAlignment = Alignment.Center
                ) {
                    if (renderedPreview != null) {
                        Image(
                            bitmap = renderedPreview!!.asImageBitmap(),
                            contentDescription = "Thumbnail Canvas",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Editor Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "templates" to "Templates",
                        "text" to "Text Layers",
                        "stickers" to "Stickers",
                        "bg" to "Background"
                    ).forEach { (id, label) ->
                        val isChosen = activeTab == id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isChosen) ElectricCyan else DarkSurfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { activeTab = id }
                                .testTag("thumb_tab_$id")
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isChosen) Color.Black else Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Content
                when (activeTab) {
                    "templates" -> {
                        Text(
                            text = "PRE-DESIGNED CREATOR TEMPLATES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(ThumbnailEngine.TEMPLATES) { template ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = DarkSurfaceElevated,
                                    border = BorderStroke(1.dp, Color(template.category.badgeColor).copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .width(150.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { applyTemplate(template) }
                                        .testTag("template_${template.id}")
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(template.category.badgeColor).copy(alpha = 0.25f)
                                        ) {
                                            Text(
                                                text = template.category.label,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(template.category.badgeColor),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = template.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "text" -> {
                        if (textLayers.isNotEmpty()) {
                            val activeLayer = textLayers.getOrNull(selectedTextIndex) ?: textLayers[0]

                            // Layer Picker
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                textLayers.indices.forEach { idx ->
                                    val isSel = selectedTextIndex == idx
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSel) NeonViolet else DarkSurfaceElevated,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { selectedTextIndex = idx }
                                    ) {
                                        Text(
                                            text = "Line ${idx + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        textLayers.add(
                                            TextLayer(
                                                text = "NEW HOOK TEXT",
                                                x = 640f,
                                                y = (300 + textLayers.size * 90).toFloat(),
                                                fontSize = 64f
                                            )
                                        )
                                        selectedTextIndex = textLayers.size - 1
                                        refreshPreview()
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Text Layer", tint = ElectricCyan)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = activeLayer.text,
                                onValueChange = {
                                    activeLayer.text = it
                                    refreshPreview()
                                },
                                label = { Text("Headline Text") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricCyan,
                                    unfocusedBorderColor = DarkSurfaceElevated,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("thumb_text_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Font size slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Text Size (${activeLayer.fontSize.toInt()}pt)", fontSize = 12.sp, color = Color.White)
                                Slider(
                                    value = activeLayer.fontSize,
                                    onValueChange = {
                                        activeLayer.fontSize = it
                                        refreshPreview()
                                    },
                                    valueRange = 36f..110f,
                                    modifier = Modifier.width(180.dp),
                                    colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                                )
                            }

                            // Y-Position Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Vertical Position", fontSize = 12.sp, color = Color.White)
                                Slider(
                                    value = activeLayer.y,
                                    onValueChange = {
                                        activeLayer.y = it
                                        refreshPreview()
                                    },
                                    valueRange = 100f..650f,
                                    modifier = Modifier.width(180.dp),
                                    colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                                )
                            }

                            // Badge toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurfaceElevated)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("High-Contrast Backdrop Badge", fontSize = 12.sp, color = Color.White)
                                Switch(
                                    checked = activeLayer.hasBackgroundBadge,
                                    onCheckedChange = {
                                        activeLayer.hasBackgroundBadge = it
                                        refreshPreview()
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan)
                                )
                            }
                        }
                    }

                    "stickers" -> {
                        Text(
                            text = "ADD HIGH-CTR STICKERS & BADGES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(emojiChoices) { emoji ->
                                Surface(
                                    shape = CircleShape,
                                    color = DarkSurfaceElevated,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            stickerLayers.add(
                                                StickerLayer(
                                                    emojiOrText = emoji,
                                                    x = (200..1000).random().toFloat(),
                                                    y = (150..550).random().toFloat(),
                                                    size = 110f,
                                                    rotationDeg = (-20..20).random().toFloat()
                                                )
                                            )
                                            refreshPreview()
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = emoji, fontSize = 22.sp)
                                    }
                                }
                            }
                        }

                        if (stickerLayers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Active Stickers: ${stickerLayers.size}", fontSize = 12.sp, color = Color.LightGray)
                                TextButton(
                                    onClick = {
                                        stickerLayers.clear()
                                        refreshPreview()
                                    }
                                ) {
                                    Text("Clear Stickers", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    "bg" -> {
                        Text(
                            text = "CUSTOM BACKGROUND IMAGE OR GRADIENT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { photoPicker.launch("image/*") },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Photo", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            if (bgBitmap != null) {
                                OutlinedButton(
                                    onClick = {
                                        bgBitmap = null
                                        refreshPreview()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Use Gradient", color = Color.White)
                                }
                            }
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

                // Action Buttons
                Button(
                    onClick = { exportAndSave() },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_thumbnail_button"),
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
                            text = if (savedSuccess) "Saved!" else "Save Thumbnail",
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
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share to YouTube / Socials", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
