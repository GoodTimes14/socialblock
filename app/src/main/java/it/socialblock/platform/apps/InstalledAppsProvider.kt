package it.socialblock.platform.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class InstalledApp(val packageName: String, val displayName: String)

@Singleton
class InstalledAppsProvider @Inject constructor(@param:ApplicationContext private val context: Context) {
    fun launcherApps(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val packageManager = context.packageManager
        val resolved =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, 0)
            }

        return resolved.asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .map {
                InstalledApp(
                    packageName = it.activityInfo.packageName,
                    displayName = it.loadLabel(packageManager).toString(),
                )
            }.distinctBy(InstalledApp::packageName)
            .sortedBy { it.displayName.lowercase() }
            .toList()
    }
}
