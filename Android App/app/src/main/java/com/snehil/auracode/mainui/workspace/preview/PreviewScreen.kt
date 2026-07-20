package com.snehil.auracode.mainui.workspace.preview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloseFullscreen
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import com.snehil.auracode.ui.components.PrimaryButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.key
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snehil.auracode.ui.components.ErrorState
import com.snehil.auracode.ui.theme.Background
import com.snehil.auracode.ui.theme.CardSurface
import com.snehil.auracode.ui.theme.MutedForeground
import com.snehil.auracode.ui.theme.Primary
import kotlinx.coroutines.delay

@Composable
fun PreviewScreen(viewModel: PreviewViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var fullscreen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onAppear()
    }

    LaunchedEffect(state.loading, state.error, state.payloadJson, state.status, state.autoFixing) {
        PreviewLog.i(
            "uiState loading=${state.loading} error=${state.error != null} " +
                "payload=${state.payloadJson != null} bytes=${state.payloadBytes} " +
                "files=${state.fileCount} key=${state.renderKey} status=${state.status} " +
                "autoFix=${state.autoFixing}"
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.loading && !state.autoFixing -> {
                PreviewQuietStatus(status = state.status)
            }

            state.error != null && !state.autoFixing && state.payloadJson == null -> {
                ErrorState(
                    message = state.error!!,
                    onRetry = {
                        if (state.repairStuck) viewModel.retryAutoFix() else viewModel.load()
                    }
                )
            }

            state.payloadJson != null -> {
                PreviewContent(
                    payloadJson = state.payloadJson.orEmpty(),
                    renderKey = state.renderKey,
                    onRefresh = {
                        PreviewLog.i("manual refresh tapped")
                        viewModel.load()
                    },
                    onFullscreen = { fullscreen = true },
                    onRuntimeError = viewModel::onRuntimeError
                )
            }

            else -> {
                PreviewLog.e("Unexpected empty UI state")
                ErrorState(message = "Preview has no data. Tap retry.", onRetry = viewModel::load)
            }
        }

        if (state.autoFixing) {
            AutoFixOverlay(status = state.autoFixStatus.ifBlank { "AuraCode is fixing the preview…" })
        } else if (state.refreshing && state.payloadJson != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .background(CardSurface.copy(alpha = 0.95f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Updating preview…",
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary
                )
            }
        }

        if (state.repairStuck && !state.autoFixing) {
            RepairStuckBanner(
                onFixAgain = viewModel::retryAutoFix,
                onDismiss = viewModel::dismissRepairStuck
            )
        }
    }

    if (fullscreen && state.payloadJson != null) {
        Dialog(
            onDismissRequest = { fullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            BackHandler { fullscreen = false }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
            ) {
                key("fs-${state.renderKey}") {
                    SandpackWebView(
                        payloadJson = state.payloadJson.orEmpty(),
                        renderKey = state.renderKey,
                        onRuntimeError = viewModel::onRuntimeError,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (state.autoFixing) {
                    AutoFixOverlay(status = state.autoFixStatus.ifBlank { "AuraCode is fixing the preview…" })
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(CardSurface.copy(alpha = 0.92f), RoundedCornerShape(12.dp))
                ) {
                    IconButton(onClick = {
                        PreviewLog.i("fullscreen refresh tapped")
                        viewModel.load()
                    }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = Primary)
                    }
                    IconButton(onClick = { fullscreen = false }) {
                        Icon(Icons.Outlined.CloseFullscreen, contentDescription = "Exit fullscreen", tint = Primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun RepairStuckBanner(
    onFixAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurface, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = Primary
                )
                Text(
                    text = "Still broken",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss",
                        tint = MutedForeground
                    )
                }
            }
            Text(
                text = "Auto-fix didn’t fully resolve it. Tap Fix again — AuraCode will retry.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedForeground,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            PrimaryButton(
                text = "Fix again",
                onClick = onFixAgain,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AutoFixOverlay(status: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(28.dp)
                .background(CardSurface, RoundedCornerShape(18.dp))
                .padding(horizontal = 22.dp, vertical = 20.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Text(
                text = "Fixing automatically",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MutedForeground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "You can keep browsing — Preview will refresh when ready.",
                style = MaterialTheme.typography.labelSmall,
                color = MutedForeground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun PreviewQuietStatus(status: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Building preview",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MutedForeground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp)
            )
        }
    }
}

@Composable
private fun BoxScope.PreviewContent(
    payloadJson: String,
    renderKey: Int,
    onRefresh: () -> Unit,
    onFullscreen: () -> Unit,
    onRuntimeError: (String) -> Unit
) {
    key(renderKey) {
        SandpackWebView(
            payloadJson = payloadJson,
            renderKey = renderKey,
            onRuntimeError = onRuntimeError,
            modifier = Modifier.fillMaxSize()
        )
    }
    Column(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp),
        horizontalAlignment = Alignment.End
    ) {
        SmallFloatingActionButton(
            onClick = onFullscreen,
            containerColor = CardSurface,
            contentColor = Primary,
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Icon(Icons.Outlined.Fullscreen, contentDescription = "Fullscreen")
        }
        SmallFloatingActionButton(
            onClick = onRefresh,
            containerColor = Primary,
            contentColor = Color.Black
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh preview")
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SandpackWebView(
    payloadJson: String,
    renderKey: Int,
    onRuntimeError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = Background.toArgb()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val nestedScroll = rememberNestedScrollInteropConnection()
    val rootView = LocalView.current
    val errorHandler = remember(onRuntimeError) { onRuntimeError }

    LaunchedEffect(renderKey) {
        PreviewLog.i("SandpackWebView compose renderKey=$renderKey payloadChars=${payloadJson.length}")
        delay(90_000)
        PreviewLog.w("90s timeout — if blank, CDN/bundler likely blocked on device")
    }

    AndroidView(
        factory = { ctx ->
            PreviewLog.i("creating WebView instance renderKey=$renderKey")
            WebView.setWebContentsDebuggingEnabled(true)
            InteractiveWebView(ctx).apply {
                setBackgroundColor(bgColor)
                isVerticalScrollBarEnabled = true
                isHorizontalScrollBarEnabled = false
                isNestedScrollingEnabled = true
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.mediaPlaybackRequiresUserGesture = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                settings.allowContentAccess = true
                settings.loadsImagesAutomatically = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.setSupportMultipleWindows(false)
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        val msg = consoleMessage?.message() ?: return true
                        if (isBenignConsoleNoise(msg)) return true
                        val level = consoleMessage.messageLevel()?.name ?: "?"
                        val line = consoleMessage.lineNumber()
                        mainHandler.post {
                            when (consoleMessage.messageLevel()) {
                                ConsoleMessage.MessageLevel.ERROR -> {
                                    PreviewLog.e("console[$level@$line] $msg")
                                    if (PreviewViewModel.looksLikeAppError(msg)) {
                                        errorHandler(msg)
                                    }
                                }
                                ConsoleMessage.MessageLevel.WARNING ->
                                    PreviewLog.w("console[$level@$line] $msg")
                                else ->
                                    PreviewLog.i("console[$level@$line] $msg")
                            }
                        }
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        PreviewLog.i("onPageStarted url=$url")
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        PreviewLog.i("onPageFinished url=$url title=${view?.title}")
                        view?.requestFocus()
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        PreviewLog.e(
                            "resource error code=${error?.errorCode} main=${request?.isForMainFrame} " +
                                "url=${request?.url} desc=${error?.description}"
                        )
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?
                    ) {
                        PreviewLog.e("HTTP ${errorResponse?.statusCode} url=${request?.url}")
                    }
                }

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun postStatus(message: String) {
                        mainHandler.post { PreviewLog.i("js-status: $message") }
                    }

                    @JavascriptInterface
                    fun onRendered() {
                        mainHandler.post { PreviewLog.i("js-onRendered") }
                    }

                    @JavascriptInterface
                    fun onPreviewReady(detail: String) {
                        mainHandler.post {
                            PreviewLog.i("js-onPreviewReady $detail")
                            requestFocus()
                            rootView.requestFocus()
                        }
                    }

                    @JavascriptInterface
                    fun postError(message: String) {
                        mainHandler.post {
                            PreviewLog.e("js-error: $message")
                            errorHandler(message)
                        }
                    }

                    @JavascriptInterface
                    fun postLog(message: String) {
                        mainHandler.post { PreviewLog.i("js: $message") }
                    }
                }, "AndroidPreview")

                loadDataWithBaseURL(
                    "https://auracode.preview/",
                    PreviewHtml.render(payloadJson),
                    "text/html",
                    "utf-8",
                    null
                )
            }
        },
        modifier = modifier.nestedScroll(nestedScroll),
        update = { webView ->
            webView.requestFocus()
        },
        onRelease = {
            PreviewLog.i("AndroidView onRelease destroy WebView")
            it.destroy()
        }
    )
}

private fun isBenignConsoleNoise(msg: String): Boolean {
    val m = msg.lowercase()
    return m.contains("cdn.tailwindcss.com") ||
        m.contains("unrecognized feature") ||
        m.contains("react router future flag") ||
        m.contains("v7_starttransition") ||
        m.contains("v7_relativesplatpath") ||
        m.contains("err_name_not_resolved") ||
        m.contains("favicon")
}
