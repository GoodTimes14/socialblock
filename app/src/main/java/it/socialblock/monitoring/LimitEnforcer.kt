package it.socialblock.monitoring

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LimitEnforcer @Inject constructor(@param:ApplicationContext private val context: Context) {
    fun returnToHome() {
        val homeIntent =
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        try {
            context.startActivity(homeIntent)
        } catch (_: RuntimeException) {
            // Some OEMs restrict background launches; the next monitoring pass retries.
        }
    }
}
