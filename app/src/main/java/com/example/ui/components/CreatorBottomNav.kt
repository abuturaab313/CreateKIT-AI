package com.example.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonAmber

enum class NavDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    CREATE("create", "Create", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline),
    PROJECTS("projects", "Projects", Icons.Filled.Folder, Icons.Outlined.Folder),
    PREMIUM("premium", "Premium", Icons.Filled.WorkspacePremium, Icons.Outlined.WorkspacePremium),
    SETTINGS("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun CreatorBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavDestination.values().forEach { dest ->
            val isSelected = currentRoute == dest.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(dest.route) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = dest.title,
                        tint = if (isSelected) {
                            if (dest == NavDestination.PREMIUM) NeonAmber else ElectricCyan
                        } else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = dest.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) {
                            if (dest == NavDestination.PREMIUM) NeonAmber else ElectricCyan
                        } else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = if (dest == NavDestination.PREMIUM) {
                        NeonAmber.copy(alpha = 0.2f)
                    } else ElectricCyan.copy(alpha = 0.15f)
                ),
                modifier = Modifier.testTag("nav_tab_${dest.route}")
            )
        }
    }
}
