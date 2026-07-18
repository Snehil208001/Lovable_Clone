package com.snehil.auracode.mainui.workspace.code

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snehil.auracode.domain.model.FileNode
import com.snehil.auracode.ui.components.EmptyState
import com.snehil.auracode.ui.components.ErrorState
import com.snehil.auracode.ui.components.LoadingState
import com.snehil.auracode.ui.theme.Background
import com.snehil.auracode.ui.theme.CardSurface
import com.snehil.auracode.ui.theme.Primary

@Composable
fun CodeScreen(
    viewModel: CodeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.loadingFiles -> LoadingState()
        state.filesError != null && state.files.isEmpty() ->
            ErrorState(message = state.filesError!!, onRetry = viewModel::loadFiles)
        state.files.isEmpty() ->
            EmptyState(
                title = "No files yet",
                subtitle = "Generate your app in the Chat tab and files will appear here."
            )
        else -> Column(modifier = Modifier.fillMaxSize()) {
            FileList(
                files = state.files,
                selectedPath = state.selectedPath,
                onSelect = viewModel::selectFile,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Box(modifier = Modifier.fillMaxWidth().weight(0.6f)) {
                when {
                    state.contentLoading -> LoadingState()
                    state.contentError != null ->
                        ErrorState(message = state.contentError!!)
                    else -> CodeViewer(
                        path = state.selectedPath.orEmpty(),
                        content = state.content
                    )
                }
            }
        }
    }
}

@Composable
private fun FileList(
    files: List<FileNode>,
    selectedPath: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "FILE EXPLORER",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(files, key = { it.path }) { file ->
                val selected = file.path == selectedPath
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(file.path) }
                        .background(if (selected) Primary.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = file.path,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeViewer(path: String, content: String) {
    val html = remember(path, content) { buildHighlightHtml(path, content) }
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.loadWithOverviewMode = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                "https://cdnjs.cloudflare.com",
                html,
                "text/html",
                "utf-8",
                null
            )
        }
    )
}

private fun buildHighlightHtml(path: String, code: String): String {
    val escaped = code
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    val language = languageClass(path)
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github-dark.min.css">
          <style>
            html, body { margin: 0; padding: 0; background: #0f172a; }
            pre { margin: 0; padding: 14px; }
            code {
              font-family: 'JetBrains Mono', ui-monospace, monospace;
              font-size: 12px;
              line-height: 1.55;
              white-space: pre;
              tab-size: 2;
            }
            .hljs { background: #0f172a !important; }
          </style>
        </head>
        <body>
          <pre><code class="$language">$escaped</code></pre>
          <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
          <script>hljs.highlightAll();</script>
        </body>
        </html>
    """.trimIndent()
}

private fun languageClass(path: String): String = when (path.substringAfterLast('.', "")) {
    "ts" -> "language-typescript"
    "tsx" -> "language-typescript"
    "js", "jsx", "mjs", "cjs" -> "language-javascript"
    "json" -> "language-json"
    "css" -> "language-css"
    "html" -> "language-html"
    "md" -> "language-markdown"
    "kt", "kts" -> "language-kotlin"
    "java" -> "language-java"
    "xml" -> "language-xml"
    "py" -> "language-python"
    else -> ""
}
