package com.snehil.auracode.mainui.billing

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutWebViewDialog(
    target: CheckoutTarget,
    onResult: (status: String?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Checkout", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close checkout")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { padding ->
            CheckoutWebView(
                target = target,
                onResult = onResult,
                onDismiss = onDismiss,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun CheckoutWebView(
    target: CheckoutTarget,
    onResult: (status: String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val uri = request.url
                    return if (isReturnUrl(uri)) {
                        onResult(uri.getQueryParameter("status"))
                        true
                    } else {
                        false
                    }
                }
            }
            when (target) {
                is CheckoutTarget.Url -> loadUrl(target.url)
                is CheckoutTarget.Cashfree -> loadDataWithBaseURL(
                    "https://auracode.checkout/",
                    cashfreeLauncherHtml(target.sessionId, if (target.sandbox) "sandbox" else "production"),
                    "text/html",
                    "utf-8",
                    null
                )
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(factory = { webView }, onRelease = { it.destroy() })
    }

    BackHandler(enabled = true) {
        if (webView.canGoBack()) webView.goBack() else onDismiss()
    }
}

/** Backend success/cancel URLs point at the web app's /billing route with a `status` query. */
private fun isReturnUrl(uri: Uri): Boolean {
    val hasStatus = uri.getQueryParameter("status") != null
    val isBilling = uri.path?.contains("billing", ignoreCase = true) == true
    return hasStatus && isBilling
}

private fun cashfreeLauncherHtml(sessionId: String, mode: String): String = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<script src="https://sdk.cashfree.com/js/v3/cashfree.js"></script>
</head>
<body>
<script>
  try {
    var cashfree = Cashfree({ mode: "$mode" });
    cashfree.checkout({ paymentSessionId: "$sessionId", redirectTarget: "_self" });
  } catch (e) {
    document.body.innerText = "Unable to start Cashfree checkout: " + (e && e.message ? e.message : e);
  }
</script>
</body>
</html>
""".trimIndent()
