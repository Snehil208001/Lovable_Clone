package com.snehil.auracode

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snehil.auracode.mainui.workspace.preview.PreviewHtml
import com.snehil.auracode.mainui.workspace.preview.SandpackBuilder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class PreviewWebViewInstrumentedTest {

    @Test
    fun sandpackPreviewIframe_hasNonZeroHeight() {
        val payload = SandpackBuilder.build(sampleExpenseTrackerFiles())
        val payloadJson = Json { encodeDefaults = true }.encodeToString(payload)

        val readyLatch = CountDownLatch(1)
        val probeRef = AtomicReference("")
        val errorRef = AtomicReference<String?>(null)

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            val webView = WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(1080, 1920)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript(
                            "(function(){ return document.body ? document.body.innerHTML.length : 0; })();",
                            null
                        )
                    }
                }
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onPreviewReady(detail: String) {
                            probeRef.set(detail)
                            readyLatch.countDown()
                        }

                        @JavascriptInterface
                        fun postError(message: String) {
                            errorRef.set(message)
                        }
                    },
                    "AndroidPreview"
                )
                measure(1080, 1920)
                layout(0, 0, 1080, 1920)
                loadDataWithBaseURL(
                    "https://auracode.preview/",
                    PreviewHtml.render(payloadJson),
                    "text/html",
                    "utf-8",
                    null
                )
            }
            // Keep a strong reference until the latch completes.
            holderView = webView
        }

        assertTrue(
            "Preview never became ready within 120s (error=${errorRef.get()} probe=${probeRef.get()})",
            readyLatch.await(120, TimeUnit.SECONDS)
        )

        val probe = probeRef.get()
        assertFalse("Sandpack iframe missing: $probe", probe.contains("iframe=none"))
        val height = probe.substringAfter("x").toIntOrNull() ?: 0
        assertTrue("Expected iframe height > 48px but got $probe", height > 48)
    }

    @Test
    fun sandpackPreviewIframe_rendersVisiblePixels() {
        val payload = SandpackBuilder.build(sampleExpenseTrackerFiles())
        val payloadJson = Json { encodeDefaults = true }.encodeToString(payload)

        val readyLatch = CountDownLatch(1)
        val bitmapRef = AtomicReference<Bitmap?>(null)

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            val webView = WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(1080, 1920)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webChromeClient = WebChromeClient()
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onPreviewReady(detail: String) {
                            val bmp = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                            draw(Canvas(bmp))
                            bitmapRef.set(bmp)
                            readyLatch.countDown()
                        }
                    },
                    "AndroidPreview"
                )
                measure(1080, 1920)
                layout(0, 0, 1080, 1920)
                loadDataWithBaseURL(
                    "https://auracode.preview/",
                    PreviewHtml.render(payloadJson),
                    "text/html",
                    "utf-8",
                    null
                )
            }
            holderView = webView
        }

        assertTrue("Preview never became ready for screenshot test", readyLatch.await(120, TimeUnit.SECONDS))

        val bitmap = bitmapRef.get()
        assertTrue("Could not capture WebView bitmap", bitmap != null && bitmap.width > 0 && bitmap.height > 0)

        var nonDarkPixels = 0
        val sampleStep = 8
        for (y in 0 until bitmap!!.height step sampleStep) {
            for (x in 0 until bitmap.width step sampleStep) {
                val color = bitmap.getPixel(x, y)
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF
                if (r + g + b > 60) nonDarkPixels++
            }
        }
        assertTrue(
            "WebView looks blank (non-dark sample pixels=$nonDarkPixels)",
            nonDarkPixels > 20
        )
    }

    private companion object {
        @Volatile
        var holderView: WebView? = null

        fun sampleExpenseTrackerFiles(): List<Pair<String, String>> = listOf(
            "/src/main.tsx" to """
                import React from "react";
                import ReactDOM from "react-dom/client";
                import App from "./App";
                ReactDOM.createRoot(document.getElementById("root")!).render(
                  <React.StrictMode><App /></React.StrictMode>
                );
            """.trimIndent(),
            "/src/App.tsx" to """
                import { Routes, Route, Link } from "react-router-dom";

                export default function App() {
                  return (
                    <div className="min-h-screen bg-emerald-500 text-white p-6">
                      <nav className="flex gap-4 mb-6 text-lg">
                        <Link to="/">Dashboard</Link>
                        <Link to="/add">Add Expense</Link>
                      </nav>
                      <Routes>
                        <Route path="/" element={<h1 className="text-3xl font-bold">Expense Dashboard</h1>} />
                        <Route path="/add" element={<h1 className="text-3xl font-bold">Add Expense</h1>} />
                      </Routes>
                    </div>
                  );
                }
            """.trimIndent(),
            "/src/index.css" to "body { margin: 0; font-family: sans-serif; }",
            "/index.html" to """<!DOCTYPE html><html><body><div id="root"></div></body></html>"""
        )
    }
}
