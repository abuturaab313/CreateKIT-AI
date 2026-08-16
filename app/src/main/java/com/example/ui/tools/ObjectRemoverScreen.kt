package com.example.ui.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ToolType
import com.example.engine.AiResult
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

data class StrokePath(
    val points: List<Offset>,
    val radius: Float
)

@Composable
fun ObjectRemoverScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedInputUri by remember { mutableStateOf<Uri?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cleanedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var brushRadius by remember { mutableFloatStateOf(28f) }
    val strokes = remember { mutableStateListOf<StrokePath>() }
    var canvasSize by remember { mutableStateOf(IntSize(800, 600)) }

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
            cleanedBitmap = null
            lastSavedFile = null
            savedSuccess = false
            saveError = null
            strokes.clear()
            val inputSize = MediaProcessor.image.getFileSizeFromUri(context, uri)
            AppLogger.logStart("RemoveObject", uri.toString(), context.contentResolver.getType(uri), inputSize)
            originalBitmap = MediaProcessor.image.loadBitmapFromUri(context, uri, 1920)
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
            val mask = generateMaskBitmap(
                width = src.width,
                height = src.height,
                viewWidth = canvasSize.width.toFloat().coerceAtLeast(1f),
                viewHeight = canvasSize.height.toFloat().coerceAtLeast(1f),
                strokes = strokes.toList()
            )

            AppLogger.logProcessing("RemoveObject", "MediaProcessor.ai.removeObject", "strokes=${strokes.size}")
            val result = viewModel.cloudAiClient.removeObjectWithAi(src, mask) { p, stage ->
                progressVal = p
                statusText = stage
            }

            isProcessing = false
            when (result) {
                is AiResult.Success -> {
                    cleanedBitmap = result.data
                    savedSuccess = false
                    saveError = null
                    AppLogger.logSuccess("RemoveObject", "Inpainting completed (${result.latencyMs}ms)")
                }
                is AiResult.Error -> {
                    errorMessage = result.message
                    AppLogger.logFailed("RemoveObject", RuntimeException(result.message))
                }
            }
        }
    }

    fun performSave(onComplete: ((File?) -> Unit)? = null) {
        val result = cleanedBitmap ?: return
        isSaving = true
        saveError = null

        scope.launch {
            val title = "object_removed_${System.currentTimeMillis()}"
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
                    title = "Object Removed",
                    tool = ToolType.OBJECT_REMOVER,
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
            MediaStorageManager.shareMediaFile(context, cachedFile, "image/jpeg", "Share Cleaned Photo")
        } else {
            performSave { file ->
                if (file != null && file.exists()) {
                    MediaStorageManager.shareMediaFile(context, file, "image/jpeg", "Share Cleaned Photo")
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
                    val aspect = (originalBitmap!!.width.toFloat() / originalBitmap!!.height.coerceAtLeast(1).toFloat()).coerceIn(0.6f, 2.2f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspect)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkSurface)
                            .onSizeChanged { canvasSize = it }
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

                    if (saveError != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Save failed: $saveError",
                            color = Color(0xFFFF5252),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (errorMessage != null) {
                        ErrorStateCard(
                            errorMessage = errorMessage!!,
                            onRetry = { startInpainting() },
                            onCancel = { errorMessage = null }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (cleanedBitmap == null) {
                        Button(
                            onClick = { startInpainting() },
                            enabled = strokes.isNotEmpty() && !isProcessing,
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
                                text = if (strokes.isEmpty()) "Brush Over Object First" else "Erase Selected Object",
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
                                onClick = { performSave() },
                                enabled = !isSaving,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("save_obj_result_button"),
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
                                        text = if (savedSuccess) "Saved!" else "Save JPG",
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

@Composable
fun BrushCanvas(
    baseBitmap: Bitmap,
    brushRadius: Float,
    strokes: List<StrokePath>,
    onAddStroke: (StrokePath) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentPoints = listOf(offset)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentPoints = currentPoints + change.position
                    },
                    onDragEnd = {
                        if (currentPoints.isNotEmpty()) {
                            onAddStroke(StrokePath(currentPoints, brushRadius))
                            currentPoints = emptyList()
                        }
                    },
                    onDragCancel = {
                        currentPoints = emptyList()
                    }
                )
            }
    ) {
        ComposeCanvas(modifier = Modifier.fillMaxSize()) {
            val imgBitmap = baseBitmap.asImageBitmap()
            drawImage(
                image = imgBitmap,
                dstSize = IntSize(size.width.toInt(), size.height.toInt())
            )

            // Draw past strokes with translucent neon overlay
            for (stroke in strokes) {
                if (stroke.points.size > 1) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(stroke.points.first().x, stroke.points.first().y)
                        for (pt in stroke.points.drop(1)) {
                            lineTo(pt.x, pt.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFFFF0055).copy(alpha = 0.6f),
                        style = Stroke(
                            width = stroke.radius * 2f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                } else if (stroke.points.size == 1) {
                    drawCircle(
                        color = Color(0xFFFF0055).copy(alpha = 0.6f),
                        radius = stroke.radius,
                        center = stroke.points.first()
                    )
                }
            }

            // Draw current active stroke
            if (currentPoints.size > 1) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(currentPoints.first().x, currentPoints.first().y)
                    for (pt in currentPoints.drop(1)) {
                        lineTo(pt.x, pt.y)
                    }
                }
                drawPath(
                    path = path,
                    color = Color(0xFFFF0055).copy(alpha = 0.7f),
                    style = Stroke(
                        width = brushRadius * 2f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            } else if (currentPoints.size == 1) {
                drawCircle(
                    color = Color(0xFFFF0055).copy(alpha = 0.7f),
                    radius = brushRadius,
                    center = currentPoints.first()
                )
            }
        }
    }
}

fun generateMaskBitmap(
    width: Int,
    height: Int,
    viewWidth: Float,
    viewHeight: Float,
    strokes: List<StrokePath>
): Bitmap {
    val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(mask)
    canvas.drawColor(AndroidColor.TRANSPARENT)

    val scaleX = width.toFloat() / viewWidth.coerceAtLeast(1f)
    val scaleY = height.toFloat() / viewHeight.coerceAtLeast(1f)
    val avgScale = (scaleX + scaleY) / 2f

    val paint = Paint().apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    for (stroke in strokes) {
        paint.strokeWidth = stroke.radius * 2f * avgScale
        if (stroke.points.size > 1) {
            val path = Path()
            path.moveTo(stroke.points[0].x * scaleX, stroke.points[0].y * scaleY)
            for (i in 1 until stroke.points.size) {
                path.lineTo(stroke.points[i].x * scaleX, stroke.points[i].y * scaleY)
            }
            canvas.drawPath(path, paint)
        } else if (stroke.points.size == 1) {
            val pt = stroke.points[0]
            val circlePaint = Paint().apply {
                color = AndroidColor.WHITE
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(pt.x * scaleX, pt.y * scaleY, stroke.radius * avgScale, circlePaint)
        }
    }

    return mask
}
