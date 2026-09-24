package it.socialblock.ui.components

import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun InstalledAppIcon(packageName: String, displayName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val iconSizePixels = with(LocalDensity.current) { ICON_LOAD_SIZE.roundToPx() }
    val icon by produceState<ImageBitmap?>(null, context, packageName, iconSizePixels) {
        value =
            withContext(Dispatchers.IO) {
                try {
                    context.packageManager.getApplicationIcon(packageName)
                        .toBitmap(
                            width = iconSizePixels,
                            height = iconSizePixels,
                            config = Bitmap.Config.ARGB_8888,
                        ).asImageBitmap()
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                } catch (_: RuntimeException) {
                    null
                }
            }
    }

    Box(
        modifier =
            modifier.clip(RoundedCornerShape(ICON_CORNER_RADIUS))
                .testTag("app_icon_$packageName"),
        contentAlignment = Alignment.Center,
    ) {
        val loadedIcon = icon
        if (loadedIcon == null) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    displayName.trim().take(1).ifEmpty { "?" }.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            Image(
                painter = BitmapPainter(loadedIcon),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

private val ICON_LOAD_SIZE = 64.dp
private val ICON_CORNER_RADIUS = 14.dp
