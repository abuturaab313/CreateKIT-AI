package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectRepository
import com.example.data.model.IntentPreset
import com.example.data.model.ToolType
import com.example.engine.AiResult
import com.example.engine.CloudAiClient
import com.example.engine.ImageProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

sealed class ProcessingUiState {
    object Idle : ProcessingUiState()
    data class Processing(val progress: Float, val stage: String) : ProcessingUiState()
    data class Success(val message: String, val resultFile: File?, val previewBitmap: Bitmap? = null) : ProcessingUiState()
    data class Error(val message: String, val canRetry: Boolean = true) : ProcessingUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = ProjectRepository(database.projectDao())
    val preferencesManager = PreferencesManager(application)
    val cloudAiClient = CloudAiClient(application)

    val allProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiCredits: StateFlow<Int> = preferencesManager.aiCredits
    val isPremium: StateFlow<Boolean> = preferencesManager.isPremium
    val themeMode: StateFlow<String> = preferencesManager.themeMode
    val isOnboardingCompleted: StateFlow<Boolean> = preferencesManager.isOnboardingCompleted
    val defaultFormat: StateFlow<String> = preferencesManager.defaultFormat
    val defaultQuality: StateFlow<Int> = preferencesManager.defaultQuality

    private val _selectedIntent = MutableStateFlow<IntentPreset>(IntentPreset.YOUTUBE_VIDEO)
    val selectedIntent: StateFlow<IntentPreset> = _selectedIntent.asStateFlow()

    private val _currentScreen = MutableStateFlow("home")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _activeTool = MutableStateFlow<ToolType?>(null)
    val activeTool: StateFlow<ToolType?> = _activeTool.asStateFlow()

    private val _processingUiState = MutableStateFlow<ProcessingUiState>(ProcessingUiState.Idle)
    val processingUiState: StateFlow<ProcessingUiState> = _processingUiState.asStateFlow()

    fun selectIntent(intent: IntentPreset) {
        _selectedIntent.value = intent
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun openTool(tool: ToolType) {
        _activeTool.value = tool
        _processingUiState.value = ProcessingUiState.Idle
    }

    fun closeTool() {
        _activeTool.value = null
        _processingUiState.value = ProcessingUiState.Idle
    }

    fun setProcessingState(state: ProcessingUiState) {
        _processingUiState.value = state
    }

    fun saveProject(
        title: String,
        tool: ToolType,
        outputFile: File,
        previewBitmap: Bitmap?,
        width: Int,
        height: Int,
        format: String
    ) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            // Save preview thumbnail
            val previewDir = File(context.filesDir, "previews").apply { mkdirs() }
            val previewFile = File(previewDir, "thumb_${System.currentTimeMillis()}.jpg")
            if (previewBitmap != null) {
                val fos = FileOutputStream(previewFile)
                previewBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                fos.flush()
                fos.close()
            }

            val project = ProjectEntity(
                title = title,
                toolType = tool.name,
                previewPath = if (previewBitmap != null) previewFile.absolutePath else outputFile.absolutePath,
                outputPath = outputFile.absolutePath,
                fileType = format,
                fileSize = outputFile.length(),
                width = width,
                height = height,
                createdAt = System.currentTimeMillis()
            )
            repository.insertProject(project)
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProject(id)
        }
    }

    fun renameProject(project: ProjectEntity, newTitle: String) {
        viewModelScope.launch {
            repository.updateProject(project.copy(title = newTitle))
        }
    }

    fun useAiCredit(): Boolean {
        return preferencesManager.useCredit()
    }

    fun addBonusCredits(amount: Int = 2) {
        preferencesManager.addBonusCredits(amount)
    }

    fun upgradeToPremium() {
        preferencesManager.setPremium(true)
    }

    fun setOnboardingDone() {
        preferencesManager.setOnboardingCompleted(true)
    }

    fun setTheme(theme: String) {
        preferencesManager.setTheme(theme)
    }

    fun setDefaultFormat(format: String) {
        preferencesManager.setDefaultFormat(format)
    }

    fun setDefaultQuality(quality: Int) {
        preferencesManager.setDefaultQuality(quality)
    }
}
