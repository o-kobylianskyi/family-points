package com.olehkobylianskyi.familypoints.android.ui.screens

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Canvas
import com.caverock.androidsvg.SVG
import java.io.ByteArrayInputStream
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.gson.JsonObject
import com.olehkobylianskyi.familypoints.android.data.TemplateRepository
import kotlinx.coroutines.CancellationException

/** Display-only mapping; never changes the user's private media bindings. */
internal fun templateCategoryMediaKey(kind: String, definition: JsonObject?): String {
    val category = definition?.get("category")?.takeUnless { it.isJsonNull }
        ?.takeIf { it.isJsonPrimitive }?.asString?.lowercase()
    val keys = mapOf(
        "hygiene" to "category-self-care",
        "routine" to "category-planning",
        "chores" to "category-chores",
        "school" to "category-learning",
        "reading" to "category-learning",
        "activity" to "category-activity",
        "creativity" to "category-creativity",
        "social" to "category-family-communication",
        "habits" to "category-initiative",
        "family" to "category-home",
        "adult" to "category-adult-tasks",
        "time" to "category-screen-time",
        "privilege" to "category-privilege",
        "items" to "category-material-reward"
    )
    return keys[category] ?: if (kind == "REWARD") "reward-star" else "task-default"
}

/** Authenticated preview, scoped to workspace and key; no public image URL. */
@Composable
internal fun TemplateCategoryIcon(
    workspaceId: Long,
    key: String,
    repository: TemplateRepository
) {
    val image = produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null, workspaceId, key
    ) {
        value = try {
            val bytes = repository.categoryIcon(workspaceId, key)
            require(bytes.isNotEmpty() && bytes.size <= 2 * 1024 * 1024) { "Invalid media preview size" }
            val bitmap = if (bytes.take(256).toByteArray()
                    .toString(Charsets.UTF_8).contains("<svg")) {
                val picture = SVG.getFromInputStream(ByteArrayInputStream(bytes)).renderToPicture()
                val width = picture.width.coerceAtLeast(1)
                val height = picture.height.coerceAtLeast(1)
                val scale = minOf(1f, 512f / maxOf(width, height).toFloat())
                val boundedWidth = (width * scale).toInt().coerceAtLeast(1)
                val boundedHeight = (height * scale).toInt().coerceAtLeast(1)
                val target = Bitmap.createBitmap(boundedWidth, boundedHeight, Bitmap.Config.ARGB_8888)
                Canvas(target).apply {
                    scale(scale, scale)
                    drawPicture(picture)
                }
                target
            } else {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            bitmap?.asImageBitmap()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }
    if (image.value != null) {
        Image(
            bitmap = image.value!!,
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
    } else {
        Text(if (key.startsWith("category-")) "◇" else "•")
    }
}
