package com.snehil.auracode.mainui.workspace.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snehil.auracode.domain.model.ChatMessage
import com.snehil.auracode.domain.model.MessageRole
import com.snehil.auracode.ui.components.ErrorState
import com.snehil.auracode.ui.theme.BorderColor
import com.snehil.auracode.ui.theme.CardSurface
import com.snehil.auracode.ui.theme.InputBackground
import com.snehil.auracode.ui.theme.MutedForeground
import com.snehil.auracode.ui.theme.Primary
import com.snehil.auracode.ui.theme.PrimaryForeground
import kotlinx.coroutines.delay

private val CHAT_SUGGESTIONS = listOf(
    "Create a modern landing page",
    "Add a responsive navbar",
    "Build a styled habit tracker"
)

private val BUILDING_PHRASES = listOf(
    "Understanding your idea…",
    "Sketching the layout…",
    "Crafting components…",
    "Styling the UI…",
    "Wiring interactions…",
    "Polishing the details…"
)

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val itemCount = state.messages.size + if (state.streaming) 1 else 0
    LaunchedEffect(itemCount, state.streamingText, state.liveFiles.size) {
        if (itemCount > 0) listState.scrollToItem(itemCount - 1)
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.error != null && state.messages.isEmpty() && !state.loading ->
                    ErrorState(message = state.error!!, onRetry = viewModel::load)
                state.messages.isEmpty() && !state.streaming ->
                    ChatEmptyState()
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        MessageBubble(message)
                    }
                    if (state.streaming) {
                        item(key = "streaming") {
                            StreamingBubble(
                                text = state.streamingText,
                                liveFiles = state.liveFiles
                            )
                        }
                    }
                }
            }
        }

        state.error?.takeIf { state.messages.isNotEmpty() }?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (state.messages.isEmpty() && !state.streaming && !state.loading) {
            SuggestionRow(onSuggestion = viewModel::applySuggestion)
        }

        ChatInput(
            value = state.input,
            onValueChange = viewModel::onInputChange,
            onSend = viewModel::send,
            enabled = !state.streaming
        )
    }
}

@Composable
private fun ChatEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = "What should we build?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Describe the app or change you want. AuraCode builds it in the background — watch Preview light up.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedForeground,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun SuggestionRow(onSuggestion: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "TRY ONE",
            style = MaterialTheme.typography.labelSmall,
            color = MutedForeground,
            letterSpacing = 1.sp
        )
        CHAT_SUGGESTIONS.forEach { suggestion ->
            Text(
                text = suggestion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardSurface)
                    .clickable { onSuggestion(suggestion) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Text(
            text = if (isUser) "You" else "AuraCode",
            style = MaterialTheme.typography.labelSmall,
            color = MutedForeground,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Box(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = if (isUser) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = if (isUser) 16.dp else 4.dp
                    )
                )
                .background(if (isUser) Primary.copy(alpha = 0.18f) else CardSurface)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (isUser) {
                Text(
                    text = message.content,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                AssistantContent(message.content, streaming = false)
            }
        }
    }
}

@Composable
private fun AssistantContent(rawContent: String, streaming: Boolean) {
    val timeline = parseChatTimeline(rawContent, complete = !streaming)
    val texts = timeline.filterIsInstance<ChatTimelineItem.Text>()
    val actions = timeline.filterIsInstance<ChatTimelineItem.Action>()
    val appliedCount = actions.count { it.action == ChatAction.WROTE }

    if (timeline.isEmpty()) {
        if (streaming) {
            Text(
                text = BUILDING_PHRASES.first(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            WorkCompleteBanner(appliedCount = 0)
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!streaming) {
            WorkCompleteBanner(appliedCount = appliedCount)
        }
        texts.forEach { item ->
            Text(
                text = item.text,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        actions.forEach { item ->
            ActionChip(item.action, item.detail)
        }
    }
}

@Composable
private fun WorkCompleteBanner(appliedCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Primary.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Completed",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Primary
            )
            Text(
                text = if (appliedCount > 0) {
                    "$appliedCount file${if (appliedCount == 1) "" else "s"} applied · open Preview"
                } else {
                    "Ready · open Preview to see your app"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Outlined.Visibility,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ActionChip(action: ChatAction, detail: String) {
    val (icon, label, tint) = when (action) {
        ChatAction.READING -> Triple(Icons.Outlined.Description, "Exploring", MaterialTheme.colorScheme.tertiary)
        ChatAction.WRITING -> Triple(Icons.Outlined.Edit, "Building", Primary)
        ChatAction.WROTE -> Triple(Icons.Outlined.CheckCircle, "Applied", Primary)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "$label  $detail",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2
        )
    }
}

@Composable
private fun StreamingBubble(text: String, liveFiles: List<String>) {
    var phraseIndex by remember { mutableIntStateOf(0) }
    val timeline = parseChatTimeline(text)
    val liveSet = liveFiles.toSet()
    val actions = timeline
        .filterIsInstance<ChatTimelineItem.Action>()
        .map { action ->
            if (action.action == ChatAction.WRITING && action.detail in liveSet) {
                action.copy(action = ChatAction.WROTE)
            } else {
                action
            }
        }
    val texts = timeline.filterIsInstance<ChatTimelineItem.Text>()
    val stillBuilding = actions.any { it.action == ChatAction.WRITING }
    val appliedCount = (actions.count { it.action == ChatAction.WROTE } +
        liveFiles.count { file -> actions.none { it.detail == file } })

    LaunchedEffect(stillBuilding) {
        if (!stillBuilding) return@LaunchedEffect
        while (true) {
            delay(2200)
            phraseIndex = (phraseIndex + 1) % BUILDING_PHRASES.size
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "AuraCode",
            style = MaterialTheme.typography.labelSmall,
            color = MutedForeground,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Box(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(CardSurface)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!stillBuilding && appliedCount > 0) {
                    WorkCompleteBanner(appliedCount = appliedCount)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Primary)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = BUILDING_PHRASES[phraseIndex],
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                texts.takeLast(2).forEach { item ->
                    Text(
                        text = item.text,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                actions.forEach { item ->
                    ActionChip(item.action, item.detail)
                }

                liveFiles.takeLast(6).forEach { file ->
                    if (actions.none { it.detail == file }) {
                        ActionChip(ChatAction.WROTE, file)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
            .padding(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Describe what to build…") },
            shape = RoundedCornerShape(14.dp),
            minLines = 1,
            maxLines = 4,
            enabled = enabled,
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = InputBackground,
                unfocusedContainerColor = InputBackground,
                focusedBorderColor = Primary,
                unfocusedBorderColor = BorderColor,
                cursorColor = Primary
            )
        )
        Spacer(Modifier.size(8.dp))
        IconButton(
            onClick = onSend,
            enabled = enabled && value.isNotBlank(),
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (enabled && value.isNotBlank()) Primary else Primary.copy(alpha = 0.4f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = PrimaryForeground
            )
        }
    }
}
