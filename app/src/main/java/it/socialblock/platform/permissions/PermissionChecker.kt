package it.socialblock.platform.permissions

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import it.socialblock.monitoring.ShortFormAccessibilityService
import javax.inject.Inject
import javax.inject.Singleton

data class PermissionState(
    val usageAccess: Boolean = false,
    val overlay: Boolean = false,
    val notifications: Boolean = false,
    val shortFormBlocker: Boolean = false,
) {
    // The Reels/Shorts blocker is optional: it is only needed by the dashboard switches that use it.
    val allRequiredGranted: Boolean = usageAccess && overlay && notifications
}

@Singleton
class PermissionChecker @Inject constructor(@param:ApplicationContext private val context: Context) {
    fun current(): PermissionState = PermissionState(
        usageAccess = hasUsageAccess(),
        overlay = Settings.canDrawOverlays(context),
        notifications = hasNotificationPermission(),
        shortFormBlocker = hasShortFormBlocker(),
    )

    private fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode =
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName,
            )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun hasShortFormBlocker(): Boolean = context.getSystemService(AccessibilityManager::class.java)
        .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { service ->
            service.resolveInfo.serviceInfo.packageName == context.packageName &&
                service.resolveInfo.serviceInfo.name == ShortFormAccessibilityService::class.java.name
        }

    private fun hasNotificationPermission(): Boolean {
        val runtimePermissionGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) ==
                PackageManager.PERMISSION_GRANTED
        return runtimePermissionGranted &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
