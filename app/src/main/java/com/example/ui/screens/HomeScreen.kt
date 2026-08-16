package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.ProjectEntity
import com.example.data.model.IntentPreset
import com.example.data.model.ToolCategory
import com.example.data.model.ToolType
import com.example.ui.MainViewModel
import com.example.ui.components.CreatorTopBar
import com.example.ui.components.CreditDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.UserProfileDialog
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import java.io.File

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenTool: (ToolType) -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProjects: () -> Unit
) {
    val credits by viewModel.aiCredits.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val projects by viewModel.allProjects.collectAsState()
    val selectedIntent by viewModel.selectedIntent.collectAsState()

    var showCreditDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CreatorTopBar(
            credits = credits,
            isPremium = isPremium,
            onCreditsClick = { showCreditDialog = true },
            onPremiumClick = onNavigateToPremium,
            onProfileClick = { showProfileDialog = true },
            onSettingsClick = onNavigateToSettings
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Welcome Intent Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("welcome_intent_card"),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = DarkSurface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    NeonViolet.copy(alpha = 0.2f),
                                    ElectricCyan.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "What are you creating today?",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Select your workflow to see optimized AI & utility tools",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Choice Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(IntentPreset.values()) { preset ->
                                val isChosen = selectedIntent == preset
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isChosen) ElectricCyan else DarkSurfaceElevated,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isChosen) ElectricCyan else DarkSurfaceBorder
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { viewModel.selectIntent(preset) }
                                        .testTag("intent_chip_${preset.name}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${preset.emoji} ${preset.title}",
                                            fontSize = 13.sp,
                                            fontWeight = if (isChosen) FontWeight.Black else FontWeight.Bold,
                                            color = if (isChosen) Color.Black else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recommended Tools Section based on Intent
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RECOMMENDED FOR ${selectedIntent.title.uppercase()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(selectedIntent.recommendedTools) { tool ->
                    ToolHighlightCard(
                        tool = tool,
                        onClick = { onOpenTool(tool) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // AI Power Tools Section
            Text(
                text = "AI POWER SUITE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            ToolType.values().filter { it.isAi }.forEach { tool ->
                ToolListCard(
                    tool = tool,
                    onClick = { onOpenTool(tool) },
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Essential Creator Utilities
            Text(
                text = "ESSENTIAL UTILITIES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            ToolType.values().filter { !it.isAi }.forEach { tool ->
                ToolListCard(
                    tool = tool,
                    onClick = { onOpenTool(tool) },
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Projects
            if (projects.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT SAVED MEDIA (${projects.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onNavigateToProjects) {
                        Text("View All", color = ElectricCyan, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(projects.take(6)) { project ->
                        ProjectMiniCard(
                            project = project,
                            onClick = onNavigateToProjects
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Creator Tip Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = DarkSurfaceElevated
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NeonAmber.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Creator Tip of the Day",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Thumbnails with bold gradient text and transparent cutouts increase YouTube CTR by up to 34%.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showCreditDialog) {
        CreditDialog(
            remainingCredits = credits,
            isPremium = isPremium,
            onDismiss = { showCreditDialog = false },
            onWatchAdForCredits = { viewModel.addBonusCredits(2) },
            onUpgradeClick = {
                showCreditDialog = false
                onNavigateToPremium()
            }
        )
    }

    if (showProfileDialog) {
        UserProfileDialog(
            isPremium = isPremium,
            credits = credits,
            projectCount = projects.size,
            onDismiss = { showProfileDialog = false },
            onNavigateToPremium = {
                showProfileDialog = false
                onNavigateToPremium()
            },
            onNavigateToSettings = {
                showProfileDialog = false
                onNavigateToSettings()
            }
        )
    }
}

@Composable
fun ToolHighlightCard(
    tool: ToolType,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("tool_highlight_${tool.name}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = tool.emoji, fontSize = 28.sp)
                if (tool.badge != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ElectricCyan.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = tool.badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = tool.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = tool.subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ToolListCard(
    tool: ToolType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("tool_list_item_${tool.name}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(text = tool.emoji, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tool.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (tool.badge != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (tool.badge == "AI") NeonViolet.copy(alpha = 0.25f) else ElectricCyan.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = tool.badge,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (tool.badge == "AI") NeonViolet else ElectricCyan,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tool.subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun ProjectMiniCard(
    project: ProjectEntity,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("project_mini_${project.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                val previewFile = File(project.previewPath)
                if (previewFile.exists()) {
                    AsyncImage(
                        model = previewFile,
                        contentDescription = project.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = project.fileType,
                        color = ElectricCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = project.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${project.fileSize / 1024} KB",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
