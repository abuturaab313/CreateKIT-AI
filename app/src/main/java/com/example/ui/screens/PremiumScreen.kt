package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet

enum class PremiumPlan(
    val title: String,
    val price: String,
    val period: String,
    val discountBadge: String?,
    val isPopular: Boolean
) {
    LIFETIME("Lifetime Creator", "$29.99", "one-time payment", "BEST VALUE", true),
    ANNUAL("Annual Pro", "$19.99", "$1.66 / month, billed yearly", "50% OFF", false),
    MONTHLY("Monthly Pass", "$3.99", "billed monthly", null, false)
}

data class ProFeatureItem(
    val icon: ImageVector,
    val title: String,
    val description: String
)

@Composable
fun PremiumScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val isAlreadyPremium by viewModel.isPremium.collectAsState()
    var selectedPlan by remember { mutableStateOf(PremiumPlan.LIFETIME) }
    var showSuccessCelebration by remember { mutableStateOf(false) }

    val features = listOf(
        ProFeatureItem(
            icon = Icons.Default.AutoAwesome,
            title = "Unlimited AI Photo Enhancements",
            description = "High-definition upscaling, face reconstruction & low-light restoration without limits"
        ),
        ProFeatureItem(
            icon = Icons.Default.FlashOn,
            title = "Instant Background & Object Removal",
            description = "Edge-perfect transparent cutouts and seamless inpainting object eraser"
        ),
        ProFeatureItem(
            icon = Icons.Default.WorkspacePremium,
            title = "Pro Video Compression & Auto Captions",
            description = "High-speed 1080p/4K encoding with customized animated caption overlays"
        ),
        ProFeatureItem(
            icon = Icons.Default.Star,
            title = "Zero Ads & Priority Cloud Queue",
            description = "Pure uninterrupted workflow with highest speed AI model inference"
        )
    )

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("premium_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeonAmber.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAlreadyPremium) "ACTIVE PRO" else "CREATOR PASS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hero Badge & Title
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(NeonViolet, ElectricCyan)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "CreatorKit AI Pro",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Supercharge your workflow with unlimited AI tools, 4K exports & zero restrictions.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Feature Highlights
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = DarkSurface
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    features.forEachIndexed { index, feat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = feat.icon,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = feat.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = feat.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        if (index < features.size - 1) {
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pricing Plans
            Text(
                text = "CHOOSE YOUR PLAN",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            PremiumPlan.values().forEach { plan ->
                val isSelected = selectedPlan == plan
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) DarkSurfaceElevated else DarkSurface,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) ElectricCyan else DarkSurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { selectedPlan = plan }
                        .testTag("plan_card_${plan.name}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = plan.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (plan.discountBadge != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (plan.isPopular) NeonViolet else NeonEmerald.copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = plan.discountBadge,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (plan.isPopular) Color.White else NeonEmerald,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = plan.period,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = plan.price,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) ElectricCyan else Color.White
                            )

                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) ElectricCyan else DarkSurfaceElevated)
                                    .border(1.dp, if (isSelected) ElectricCyan else DarkSurfaceBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            Button(
                onClick = {
                    viewModel.upgradeToPremium()
                    showSuccessCelebration = true
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("unlock_premium_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAlreadyPremium) "You Are a Pro Creator" else "Unlock ${selectedPlan.title} (${selectedPlan.price})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Trust note
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "One-tap secure unlock • Instant activation • No hidden fees",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Celebration Toast/Banner
        AnimatedVisibility(
            visible = showSuccessCelebration,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = NeonEmerald,
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "CreatorKit AI Pro Unlocked!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                        Text(
                            text = "Enjoy unlimited access to all AI tools & exports.",
                            fontSize = 12.sp,
                            color = Color.Black.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}
