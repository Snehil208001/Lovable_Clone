package com.snehil.auracode.mainui.workspace.preview

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.webkit.WebView

/**
 * WebView that keeps touch/scroll events instead of losing them to Compose parents
 * or Sandpack wrapper layers above the preview iframe.
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
class InteractiveWebView(context: Context) : WebView(context) {

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
        isLongClickable = true
        isHapticFeedbackEnabled = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                requestFocus()
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
        if (clampedX || clampedY) {
            parent?.requestDisallowInterceptTouchEvent(true)
        }
    }
}
