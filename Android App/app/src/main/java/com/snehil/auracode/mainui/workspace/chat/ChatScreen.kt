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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.snehil.auracode.ui.components.LoadingState
import com.snehil.auracode.ui.theme.BorderColor
import com.snehil.auracode.ui.theme.CardSurface
import com.snehil.auracode.ui.theme.InputBackground
import com.snehil.auracode.ui.theme.Primary
import com.snehil.auracode.ui.theme.PrimaryForeground

private val CHAT_SUGGESTIONS = listOf(
    "Create a modern landing page",
    "Add a responsive navbar",
    "Build a styled habit tracker"
)

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val itemCount = state.messages.size + if (state.streaming) 1 else 0
    LaunchedEffect(itemCount, state.streamingText) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.loading -> LoadingState()
                state.error != null && state.messages.isEmpty() ->
                    ErrorState(message = state.error!!, onRetry = viewModel::load)
                state.messages.isEmpty() && !state.streaming ->
                    ChatEmptyState()
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.messages) { message ->
                        MessageBubble(message)
                    }
                    if (state.streaming) {
                        item {
                            StreamingBubble(state.streamingText)
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
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = "Build with AuraCode",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Describe an app or UI change. Files apply as they finish generating.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
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
            text = "SUGGESTIONS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            text = if (isUser) "You" else "Assistant",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
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
                AssistantContent(message.content)
            }
        }
    }
}

@Composable
private fun AssistantContent(rawContent: String) {
    val timeline = parseChatTimeline(rawContent)
    if (timeline.isEmpty()) {
        Text(
            text = formatChatContent(rawContent).ifBlank { "…" },
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        timeline.forEach { item ->
            when (item) {
                is ChatTimelineItem.Text -> Text(
                    text = item.text,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
                is ChatTimelineItem.Action -> ActionChip(item.action, item.detail)
            }
        }
    }
}

@Composable
private fun ActionChip(action: ChatAction, detail: String) {
    val (icon, label) = when (action) {
        ChatAction.READING -> Icons.Outlined.Description to "Reading"
        ChatAction.WRITING -> Icons.Outlined.Edit to "Writing"
        ChatAction.WROTE -> Icons.Outlined.Edit to "Wrote"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (action == ChatAction.READING) MaterialTheme.colorScheme.tertiary else Primary,
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
private fun StreamingBubble(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Assistant",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(CardSurface)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (text.isBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = Primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "Thinking…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                AssistantContent(text)
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
            placeholder = { Text("Describe UI or request code changes…") },
            shape = RoundedCornerShape(14.dp),
            minLines = 1,
            maxLines = 4,
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
