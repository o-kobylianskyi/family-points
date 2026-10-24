package com.olehkobylianskyi.familypoints.android.ui.components

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Canvas
import com.caverock.androidsvg.SVG
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.network.ApiClient
import com.olehkobylianskyi.familypoints.android.network.MediaRefDto
import kotlinx.coroutines.CancellationException

/** No external image-loader, disk cache or public media URL: private V37 bytes require bearer auth. */
@Composable
fun MemberMediaAvatar(workspaceId: Long, bearer: String?, image: MediaRefDto?, name: String) {
    var bitmap by remember(workspaceId, bearer, image?.mediaId, image?.catalogKey, image?.version) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }
    LaunchedEffect(workspaceId, bearer, image?.mediaId, image?.catalogKey, image?.version) {
        bitmap = null
        if (bearer.isNullOrBlank()) return@LaunchedEffect
        val ref = image ?: return@LaunchedEffect
        try {
            // Web uses the authenticated catalog preview path for catalog-bound SVGs.
            // Never build an image URL from untrusted ref.path or use an unauthenticated loader.
            val asset = ref.mediaId
            val catalogKey = ref.catalogKey
            if (asset != null) {
                ApiClient.mediaApi.imageVariant(bearer, workspaceId, asset).use { body ->
                    val bytes = body.bytes()
                    if (bytes.size in 1..2_000_000) {
                        bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                }
            } else if (ref.source != "DEFAULT" &&
                catalogKey != null && catalogKey.matches(Regex("[a-z0-9-]{1,64}"))) {
                ApiClient.mediaApi.catalogPreview(bearer, workspaceId, catalogKey).use { body ->
                    val bytes = body.bytes()
                    if (bytes.size in 1..256_000) {
                        val svg = SVG.getFromString(String(bytes, Charsets.UTF_8))
                        val rendered = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
                        svg.documentWidth = 128f
                        svg.documentHeight = 128f
                        svg.renderToCanvas(Canvas(rendered))
                        bitmap = rendered
                    }
                }
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            // Catalog SVG and denied/unavailable images intentionally retain a neutral placeholder.
        }
    }
    Box(
        modifier = Modifier.size(42.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let { Image(bitmap = it.asImageBitmap(), contentDescription = name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            ?: Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium)
    }
}
