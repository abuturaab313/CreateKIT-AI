package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.model.ToolType
import com.example.ui.MainViewModel
import com.example.ui.components.CreatorBottomNav
import com.example.ui.screens.CreateScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PremiumScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.tools.AutoCaptionScreen
import com.example.ui.tools.BackgroundRemoverScreen
import com.example.ui.tools.CompressScreen
import com.example.ui.tools.EnhanceScreen
import com.example.ui.tools.FormatConverterScreen
import com.example.ui.tools.ImageToPdfScreen
import com.example.ui.tools.ObjectRemoverScreen
import com.example.ui.tools.ResizeScreen
import com.example.ui.tools.ThumbnailMakerScreen
import com.example.ui.tools.VideoCompressorScreen

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = when (themeMode) {
                "LIGHT" -> false
                "SYSTEM" -> isSystemInDarkTheme()
                else -> true // DARK default for creator studio
            }

            MyApplicationTheme(darkTheme = isDark) {
                CreatorApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun CreatorApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeTool by viewModel.activeTool.collectAsState()

    // Handle back presses
    if (activeTool != null) {
        BackHandler {
            viewModel.closeTool()
        }
    } else if (currentScreen != "home") {
        BackHandler {
            viewModel.navigateTo("home")
        }
    }

    if (activeTool != null) {
        // Full screen Tool Canvas View
        Box(modifier = Modifier.fillMaxSize()) {
            when (activeTool) {
                ToolType.ENHANCE -> EnhanceScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.closeTool() }
                )
                ToolType.BACKGROUND_REMOVER -> BackgroundRemoverScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.closeTool() }
                )
                ToolType.OBJECT_REMOVER -> ObjectRemoverScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.closeTool() }
                )
                ToolType.COMPRESS -> CompressScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.closeTool() }
                )
                ToolType.RESIZE -> ResizeScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.closeTool() }
                )
                ToolType.THUMBNAIL_MAKER -> ThumbnailMakerScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.closeTool() }
                )
                ToolType.AUTO_CAPTION -> AutoCaptionScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.closeTool() }
                )
                ToolType.IMAGE_TO_PDF -> ImageToPdfScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.closeTool() }
                )
                ToolType.VIDEO_COMPRESSOR -> VideoCompressorScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.closeTool() }
                )
                ToolType.FORMAT_CONVERTER -> FormatConverterScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.closeTool() }
                )
                null -> {}
            }
        }
    } else {
        // Main Tab Shell with Bottom Navigation
        Scaffold(
            bottomBar = {
                CreatorBottomNav(
                    currentRoute = currentScreen,
                    onNavigate = { route -> viewModel.navigateTo(route) }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "MainScreenTransition"
                ) { screen ->
                    when (screen) {
                        "home" -> HomeScreen(
                            viewModel = viewModel,
                            onOpenTool = { tool -> viewModel.openTool(tool) },
                            onNavigateToPremium = { viewModel.navigateTo("premium") },
                            onNavigateToSettings = { viewModel.navigateTo("settings") },
                            onNavigateToProjects = { viewModel.navigateTo("projects") }
                        )
                        "create" -> CreateScreen(
                            viewModel = viewModel,
                            onOpenTool = { tool -> viewModel.openTool(tool) }
                        )
                        "projects" -> ProjectsScreen(
                            viewModel = viewModel,
                            onNavigateToCreate = { viewModel.navigateTo("create") }
                        )
                        "premium" -> PremiumScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateTo("home") }
                        )
                        "settings" -> SettingsScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateTo("home") },
                            onNavigateToPremium = { viewModel.navigateTo("premium") }
                        )
                        else -> HomeScreen(
                            viewModel = viewModel,
                            onOpenTool = { tool -> viewModel.openTool(tool) },
                            onNavigateToPremium = { viewModel.navigateTo("premium") },
                            onNavigateToSettings = { viewModel.navigateTo("settings") },
                            onNavigateToProjects = { viewModel.navigateTo("projects") }
                        )
                    }
                }
            }
        }
    }
}
