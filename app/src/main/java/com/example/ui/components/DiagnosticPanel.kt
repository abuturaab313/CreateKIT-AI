package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan

data class DiagnosticInfo(
    val selectedUri: String = "None",
    val inputMime: String = "N/A",
    val inputSize: String = "N/A",
    val inputDimensions: String = "N/A",
    val processor: String = "CreatorKit Engine",
    val processingState: String = "Idle",
    val outputUri: String = "None",
    val outputMime: String = "N/A",
    val outputSize: String = "N/A",
    val outputDimensions: String = "N/A",
    val hasAlpha: Boolean = false,
    val transparentPixelsDetected: Boolean = false,
    val maskGenerated: Boolean = false,
    val alphaApplied: Boolean = false,
    val saveStatus: String = "Idle",
    val shareStatus: String = "Idle"
)

@Composable
fun DiagnosticPanel(
    info: DiagnosticInfo,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceElevated.copy(alpha = 0.9f),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .testTag("diagnostic_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Debug Diagnostic",
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Engine Diagnostics",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ElectricCyan
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle Diagnostics",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    DiagRow("Input URI", info.selectedUri)
                    DiagRow("Input MIME", info.inputMime)
                    DiagRow("Input Size", info.inputSize)
                    DiagRow("Input Dim", info.inputDimensions)
                    Spacer(modifier = Modifier.height(6.dp))
                    DiagRow("Processor", info.processor)
                    DiagRow("State", info.processingState)
                    DiagRow("Mask Generated", if (info.maskGenerated) "YES" else "NO")
                    DiagRow("Alpha Applied", if (info.alphaApplied) "YES" else "NO")
                    Spacer(modifier = Modifier.height(6.dp))
                    DiagRow("Output URI", info.outputUri)
                    DiagRow("Output MIME", info.outputMime)
                    DiagRow("Output Size", info.outputSize)
                    DiagRow("Output Dim", info.outputDimensions)
                    DiagRow("Has Alpha", if (info.hasAlpha) "YES (RGBA)" else "NO")
                    DiagRow("Alpha Pixels", if (info.transparentPixelsDetected) "YES" else "NO")
                    Spacer(modifier = Modifier.height(6.dp))
                    DiagRow("Save Status", info.saveStatus)
                    DiagRow("Share Status", info.shareStatus)
                }
            }
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value.takeLast(35),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )
    }
}
