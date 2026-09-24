package it.socialblock.monitoring

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import dagger.hilt.android.qualifiers.ApplicationContext
import it.socialblock.domain.model.OverlayPosition
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class WarningOverlayController @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var overlayView: TextView? = null
    private var currentMessage: String? = null
    private var currentPosition: OverlayPosition? = null
    private var currentOpacity: Float? = null

    suspend fun show(message: String, position: OverlayPosition, opacity: Float) = withContext(Dispatchers.Main.immediate) {
        if (!Settings.canDrawOverlays(context)) return@withContext
        val safeOpacity = opacity.coerceIn(MIN_WINDOW_ALPHA, MAX_WINDOW_ALPHA)
        val currentView = overlayView
        if (currentView != null) {
            if (currentMessage != message) {
                currentView.text = message
                currentMessage = message
            }
            if (currentPosition != position || currentOpacity != safeOpacity) {
                try {
                    windowManager.updateViewLayout(currentView, layoutParams(position, safeOpacity))
                    currentPosition = position
                    currentOpacity = safeOpacity
                } catch (_: RuntimeException) {
                    removeViewSafely(currentView)
                    overlayView = null
                    currentPosition = null
                    currentOpacity = null
                }
            }
            if (overlayView != null) return@withContext
        }

        val view =
            TextView(context).apply {
                text = message
                setTextColor(Color.BLACK)
                textSize = WARNING_TEXT_SIZE_SP
                typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
                gravity = Gravity.CENTER
                includeFontPadding = false
                setLineSpacing(0f, WARNING_LINE_SPACING_MULTIPLIER)
                setPadding(dp(HORIZONTAL_PADDING_DP), dp(VERTICAL_PADDING_DP), dp(HORIZONTAL_PADDING_DP), dp(VERTICAL_PADDING_DP))
                background =
                    GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setColor(WARNING_BACKGROUND_COLOR)
                        cornerRadius = 0f
                        setStroke(dp(BORDER_WIDTH_DP), Color.BLACK)
                    }
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        try {
            windowManager.addView(view, layoutParams(position, safeOpacity))
            overlayView = view
            currentMessage = message
            currentPosition = position
            currentOpacity = safeOpacity
        } catch (_: RuntimeException) {
            overlayView = null
            currentMessage = null
            currentPosition = null
            currentOpacity = null
        }
    }

    suspend fun hide() = withContext(Dispatchers.Main.immediate) {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: IllegalArgumentException) {
                // The system may already have removed the overlay after permission changes.
            }
        }
        overlayView = null
        currentMessage = null
        currentPosition = null
        currentOpacity = null
    }

    private fun layoutParams(position: OverlayPosition, opacity: Float): WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity =
            when (position) {
                OverlayPosition.TOP -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
                OverlayPosition.CENTER -> Gravity.CENTER
                OverlayPosition.BOTTOM -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
        x = 0
        y =
            when (position) {
                OverlayPosition.TOP -> dp(TOP_MARGIN_DP)
                OverlayPosition.CENTER -> 0
                OverlayPosition.BOTTOM -> dp(BOTTOM_MARGIN_DP)
            }
        width = context.resources.displayMetrics.widthPixels - dp(HORIZONTAL_MARGIN_DP * 2)
        alpha = opacity
    }

    private fun removeViewSafely(view: View) {
        try {
            windowManager.removeView(view)
        } catch (_: RuntimeException) {
            // The system may have detached the view while the position was changing.
        }
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    private companion object {
        // Android blocks pass-through touches from application overlays above 0.8 opacity.
        const val MIN_WINDOW_ALPHA = 0.2f
        const val MAX_WINDOW_ALPHA = 0.8f
        const val WARNING_TEXT_SIZE_SP = 30f
        const val WARNING_LINE_SPACING_MULTIPLIER = 0.96f
        const val WARNING_BACKGROUND_COLOR = 0xFFF2F2F2.toInt()
        const val HORIZONTAL_MARGIN_DP = 20
        const val HORIZONTAL_PADDING_DP = 22
        const val VERTICAL_PADDING_DP = 22
        const val BORDER_WIDTH_DP = 4
        const val TOP_MARGIN_DP = 64
        const val BOTTOM_MARGIN_DP = 88
    }
}
