package it.socialblock.monitoring

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import it.socialblock.domain.shortform.ShortFormBlockReason
import it.socialblock.domain.shortform.ShortFormFeature
import it.socialblock.domain.shortform.ShortFormPanels
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Closes Reels, Shorts and X's video feed when the always-on block is set or their app has reached its
 * limit. It only looks up the panel's view ID in Instagram, YouTube and X: no text or content is read,
 * and nothing is stored.
 */
@AndroidEntryPoint
class ShortFormAccessibilityService : AccessibilityService() {
    @Inject lateinit var blockState: ShortFormBlockState

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lastBackAtMillis = 0L
    private var blockedPackages = emptyMap<String, ShortFormBlockReason>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        blockState.setBlockerConnected(true)
        serviceScope.launch {
            blockState.blockedPackages.collect { blocked ->
                blockedPackages = blocked
                // A block can start while a panel is already open, without any new event.
                closeBlockedPanel()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName?.toString() in blockedPackages) {
            closeBlockedPanel()
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        blockState.setBlockerConnected(false)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        blockState.setBlockerConnected(false)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun closeBlockedPanel() {
        val nowMillis = SystemClock.elapsedRealtime()
        if (nowMillis - lastBackAtMillis < BACK_COOLDOWN_MILLIS) return
        val (feature, reason) = openBlockedPanel() ?: return

        lastBackAtMillis = nowMillis
        performGlobalAction(GLOBAL_ACTION_BACK)
        val detail =
            when (reason) {
                ShortFormBlockReason.ALWAYS -> "blocco sempre attivo"
                ShortFormBlockReason.LIMIT_REACHED -> "limite di oggi raggiunto"
            }
        Toast.makeText(this, "${feature.name} bloccati: $detail", Toast.LENGTH_SHORT).show()
    }

    private fun openBlockedPanel(): Pair<ShortFormFeature, ShortFormBlockReason>? {
        val root = rootInActiveWindow ?: return null
        val packageName = root.packageName?.toString().orEmpty()
        val reason = blockedPackages[packageName]
        val feature = ShortFormPanels.forPackage(packageName)
        return if (reason != null && feature != null && isOpen(feature, root)) feature to reason else null
    }

    private fun isOpen(feature: ShortFormFeature, root: AccessibilityNodeInfo): Boolean =
        feature.viewIds.any { viewId -> root.findAccessibilityNodeInfosByViewId(viewId).any { isOpenPanel(feature, it) } }

    // Instagram keeps the Reels pager inflated in other tabs, so only a visible panel counts.
    private fun isOpenPanel(feature: ShortFormFeature, node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect().also(node::getBoundsInScreen)
        val screen = resources.displayMetrics
        return node.isVisibleToUser &&
            feature.coversScreen(bounds.width(), bounds.height(), screen.widthPixels, screen.heightPixels)
    }

    private companion object {
        // Leaves time for the Back transition, so a single panel never triggers a second Back.
        const val BACK_COOLDOWN_MILLIS = 1_000L
    }
}
