package com.snehil.auracode.mainui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snehil.auracode.domain.model.ProjectSummary
import com.snehil.auracode.domain.model.UsageToday
import com.snehil.auracode.ui.components.AuraBackground
import com.snehil.auracode.ui.components.AuraTextField
import com.snehil.auracode.ui.components.BrandBadge
import com.snehil.auracode.ui.components.EmptyState
import com.snehil.auracode.ui.components.ErrorState
import com.snehil.auracode.ui.components.GlassCard
import com.snehil.auracode.ui.components.InitialsAvatar
import com.snehil.auracode.ui.components.LoadingState
import com.snehil.auracode.ui.components.PrimaryButton
import com.snehil.auracode.ui.theme.BorderColor
import com.snehil.auracode.ui.theme.Primary

@Composable
fun DashboardScreen(
    onOpenProject: (Long) -> Unit,
    onOpenBilling: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }
    LaunchedEffect(Unit) {
        viewModel.loggedOut.collect { onLoggedOut() }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        AuraBackground {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                DashboardHeader(
                    userName = state.userName,
                    userInitials = state.userInitials,
                    onBilling = onOpenBilling,
                    onLogout = viewModel::logout
                )

                when {
                    state.loading -> LoadingState()
                    state.error != null && state.projects.isEmpty() ->
                        ErrorState(message = state.error!!, onRetry = viewModel::load)

                    else -> DashboardContent(
                        projects = state.projects,
                        usage = state.usage,
                        onOpenProject = onOpenProject,
                        onDelete = viewModel::deleteProject,
                        onCreate = { showCreate = true }
                    )
                }
            }
        }
    }

    if (showCreate) {
        CreateProjectDialog(
            creating = state.creating,
            onDismiss = { showCreate = false },
            onConfirm = { name, desc ->
                viewModel.createProject(name, desc) { projectId ->
                    showCreate = false
                    onOpenProject(projectId)
                }
            }
        )
    }
}

@Composable
private fun DashboardHeader(
    userName: String,
    userInitials: String,
    onBilling: () -> Unit,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrandBadge(size = 40)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "AuraCode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "AI App Platform",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onBilling) {
            Icon(
                imageVector = Icons.Outlined.CreditCard,
                contentDescription = "Billing",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        InitialsAvatar(initials = userInitials, size = 34)
        IconButton(onClick = onLogout) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = "Log out",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DashboardContent(
    projects: List<ProjectSummary>,
    usage: UsageToday?,
    onOpenProject: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onCreate: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (usage != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                UsageRow(usage)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Your projects",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                PrimaryButton(
                    text = "New project",
                    onClick = onCreate
                )
            }
        }

        if (projects.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    title = "No projects yet",
                    subtitle = "Create your first project to start building with AI.",
                    action = { PrimaryButton(text = "Create project", onClick = onCreate) }
                )
            }
        } else {
            items(projects, key = { it.id }) { project ->
                ProjectCard(
                    project = project,
                    onClick = { onOpenProject(project.id) },
                    onDelete = { onDelete(project.id) }
                )
            }
        }
    }
}

@Composable
private fun UsageRow(usage: UsageToday) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        UsageStatCard(
            label = "Tokens today",
            value = "${usage.tokensUsed} / ${usage.tokensLimit}",
            progress = safeRatio(usage.tokensUsed, usage.tokensLimit),
            modifier = Modifier.weight(1f)
        )
        UsageStatCard(
            label = "Previews",
            value = "${usage.previewsRunning} / ${usage.previewsLimit}",
            progress = safeRatio(usage.previewsRunning, usage.previewsLimit),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun UsageStatCard(
    label: String,
    value: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Primary,
                trackColor = BorderColor
            )
        }
    }
}

@Composable
private fun ProjectCard(
    project: ProjectSummary,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = project.name.take(1).uppercase(),
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = project.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = project.description?.ifBlank { "No description" } ?: "No description",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateProjectDialog(
    creating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!creating) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("New project", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier.imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AuraTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Project name",
                    enabled = !creating
                )
                AuraTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Description (optional)",
                    enabled = !creating
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Create",
                onClick = { onConfirm(name, description) },
                enabled = name.isNotBlank() && !creating,
                loading = creating,
                modifier = Modifier.width(120.dp)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !creating) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

private fun safeRatio(used: Int, total: Int): Float =
    if (total <= 0) 0f else (used.toFloat() / total.toFloat()).coerceIn(0f, 1f)
