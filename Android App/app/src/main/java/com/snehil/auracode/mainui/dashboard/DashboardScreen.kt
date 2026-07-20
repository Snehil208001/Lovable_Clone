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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Visibility
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.snehil.auracode.ui.theme.ChartAmber
import com.snehil.auracode.ui.theme.ChartCyan
import com.snehil.auracode.ui.theme.ChartRed
import com.snehil.auracode.ui.theme.ChartViolet
import com.snehil.auracode.ui.theme.EmeraldDeep
import com.snehil.auracode.ui.theme.MutedForeground
import com.snehil.auracode.ui.theme.Primary
import com.snehil.auracode.ui.theme.PrimaryForeground
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ProjectAccentColors = listOf(
    Primary,
    Color(0xFF3B82F6),
    ChartViolet,
    Color(0xFFF97316),
    Color(0xFF1D4ED8),
    ChartRed,
    ChartCyan,
    ChartAmber
)

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
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        AuraBackground {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                DashboardHeader(
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
    userInitials: String,
    onBilling: () -> Unit,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrandBadge(size = 40)
        Spacer(modifier = Modifier.width(10.dp))
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
                color = MutedForeground
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
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (usage != null) {
            item(span = { GridItemSpan(2) }) {
                UsageRow(usage)
            }
        }

        item(span = { GridItemSpan(2) }) {
            NewProjectCta(onClick = onCreate)
        }

        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Your projects",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Recently updated",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedForeground
                    )
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MutedForeground,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (projects.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
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
    val tokensProgress = safeRatio(usage.tokensUsed, usage.tokensLimit)
    val previewsProgress = safeRatio(usage.previewsRunning, usage.previewsLimit)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        UsageStatCard(
            label = "Tokens today",
            value = "${usage.tokensUsed} / ${usage.tokensLimit}",
            percentLabel = "${(tokensProgress * 100).toInt()}% used",
            progress = tokensProgress,
            icon = Icons.Outlined.Layers,
            modifier = Modifier.weight(1f)
        )
        UsageStatCard(
            label = "Previews",
            value = "${usage.previewsRunning} / ${usage.previewsLimit}",
            percentLabel = "${(previewsProgress * 100).toInt()}% used",
            progress = previewsProgress,
            icon = Icons.Outlined.Visibility,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun UsageStatCard(
    label: String,
    value: String,
    percentLabel: String,
    progress: Float,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedForeground,
                    letterSpacing = 0.6.sp
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                text = percentLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MutedForeground,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Primary,
                trackColor = BorderColor
            )
        }
    }
}

@Composable
private fun NewProjectCta(onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Primary, EmeraldDeep)))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(1.5.dp, PrimaryForeground.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = PrimaryForeground,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "New project",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryForeground
            )
            Text(
                text = "Start building your next idea",
                style = MaterialTheme.typography.bodySmall,
                color = PrimaryForeground.copy(alpha = 0.75f)
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = PrimaryForeground,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun ProjectCard(
    project: ProjectSummary,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val accent = ProjectAccentColors[(project.id % ProjectAccentColors.size).toInt()]

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = project.name.take(1).uppercase(),
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Updated ${formatUpdatedAt(project.updatedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedForeground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "More",
                        tint = MutedForeground,
                        modifier = Modifier.size(18.dp)
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

private val UpdatedAtFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

private fun formatUpdatedAt(raw: String?): String {
    if (raw.isNullOrBlank()) return "—"
    return try {
        val instant = Instant.parse(raw)
        UpdatedAtFormatter.format(instant.atZone(ZoneId.systemDefault()))
    } catch (_: Exception) {
        try {
            // Fallback for local datetime strings without zone.
            raw.take(10).let { date ->
                val parts = date.split("-")
                if (parts.size == 3) {
                    val month = listOf(
                        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                    )[parts[1].toInt() - 1]
                    "${parts[2].toInt()} $month ${parts[0]}"
                } else raw
            }
        } catch (_: Exception) {
            raw
        }
    }
}
