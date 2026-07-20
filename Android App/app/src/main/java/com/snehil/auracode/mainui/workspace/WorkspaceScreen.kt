package com.snehil.auracode.mainui.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import com.snehil.auracode.mainui.workspace.chat.ChatScreen
import com.snehil.auracode.mainui.workspace.code.CodeScreen
import com.snehil.auracode.mainui.workspace.preview.PreviewScreen
import com.snehil.auracode.ui.components.AuraBackground
import com.snehil.auracode.ui.theme.Primary

private enum class WorkspaceTab(val label: String, val icon: ImageVector) {
    CHAT("Chat", Icons.Outlined.ChatBubbleOutline),
    PREVIEW("Preview", Icons.Outlined.Visibility),
    CODE("Code", Icons.Outlined.Code)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    projectId: Long,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(WorkspaceTab.CHAT) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedTab.label,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                WorkspaceTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            indicatorColor = Primary.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        AuraBackground {
            // Keep all tabs composed so Preview WebView + chat state survive switches.
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                KeepAlivePane(visible = selectedTab == WorkspaceTab.CHAT) {
                    ChatScreen()
                }
                KeepAlivePane(visible = selectedTab == WorkspaceTab.PREVIEW) {
                    PreviewScreen()
                }
                KeepAlivePane(visible = selectedTab == WorkspaceTab.CODE) {
                    CodeScreen()
                }
            }
        }
    }
}

@Composable
private fun KeepAlivePane(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(if (visible) 1f else 0f)
            .graphicsLayer { alpha = if (visible) 1f else 0f }
    ) {
        content()
    }
}
