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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.key
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snehil.auracode.ui.components.ErrorState
import com.snehil.auracode.ui.theme.Background
import com.snehil.auracode.ui.theme.Primary
import kotlinx.coroutines.delay

@Composable
fun PreviewScreen(viewModel: PreviewViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.loading, state.error, state.payloadJson, state.status) {
        PreviewLog.i(
            "uiState loading=${state.loading} error=${state.error != null} " +
                "payload=${state.payloadJson != null} bytes=${state.payloadBytes} " +
                "files=${state.fileCount} key=${state.renderKey} status=${state.status}"
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.loading -> {
                PreviewLoadingOverlay(status = state.status)
            }

            state.error != null -> {
                ErrorState(message = state.error!!, onRetry = viewModel::load)
            }

            state.payloadJson != null -> {
                PreviewContent(
                    payloadJson = state.payloadJson.orEmpty(),
                    renderKey = state.renderKey,
                    onRefresh = {
                        PreviewLog.i("manual refresh tapped")
                        viewModel.load()
                    }
                )
            }

            else -> {
                PreviewLog.e("Unexpected empty UI state")
                ErrorState(message = "Preview has no data. Tap retry.", onRetry = viewModel::load)
            }
        }
    }
}

@Composable
private fun PreviewLoadingOverlay(status: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Primary)
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp)
            )
        }
    }
}

@Composable
private fun BoxScope.PreviewContent(
    payloadJson: String,
    renderKey: Int,
    onRefresh: () -> Unit
) {
    key(renderKey) {
        SandpackWebView(
            payloadJson = payloadJson,
            renderKey = renderKey,
            modifier = Modifier.fillMaxSize()
        )
    }
    SmallFloatingActionButton(
        onClick = onRefresh,
        containerColor = Primary,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp)
    ) {
        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh preview")
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SandpackWebView(
    payloadJson: String,
    renderKey: Int,
    modifier: Modifier = Modifier
) {
    val bgColor = Background.toArgb()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val nestedScroll = rememberNestedScrollInteropConnection()
    val rootView = LocalView.current

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
                                ConsoleMessage.MessageLevel.ERROR ->
                                    PreviewLog.e("console[$level@$line] $msg")
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
                        mainHandler.post { PreviewLog.e("js-error: $message") }
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
