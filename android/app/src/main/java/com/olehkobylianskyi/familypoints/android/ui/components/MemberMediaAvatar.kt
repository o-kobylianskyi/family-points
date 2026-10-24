package com.olehkobylianskyi.familypoints.android.ui.components

import android.graphics.BitmapFactory
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
    var bitmap by remember(workspaceId, bearer, image?.mediaId, image?.version) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }
    LaunchedEffect(workspaceId, bearer, image?.mediaId, image?.version) {
        bitmap = null
        val asset = image?.mediaId ?: return@LaunchedEffect
        if (bearer.isNullOrBlank()) return@LaunchedEffect
        try {
            // Backend verifies that this asset is bound to an authorized target in this workspace.
            ApiClient.mediaApi.imageVariant(bearer, workspaceId, asset).use { body ->
                val bytes = body.bytes()
                if (bytes.size <= 2_000_000) {
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
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
