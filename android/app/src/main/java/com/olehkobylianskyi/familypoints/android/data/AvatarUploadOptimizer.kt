package com.olehkobylianskyi.familypoints.android.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

/** Size-bounded, sampled avatar upload. Runs on Dispatchers.IO from the caller. */
object AvatarUploadOptimizer {
    private const val MAX_EDGE = 1600
    private const val MAX_BYTES = 2 * 1024 * 1024

    data class Result(val bytes: ByteArray, val mimeType: String, val filename: String)

    fun prepare(resolver: ContentResolver, uri: Uri): Result {
        val mime = resolver.getType(uri)?.lowercase()
        require(mime == "image/jpeg" || mime == "image/png") { "Only JPEG/PNG images are supported" }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri) ?: error("Cannot read image")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Invalid image" }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_EDGE * 2) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Unable to decode image")
        try {
            // PNG compression is lossless: reducing quality does not reduce file size.
            // Retry progressively smaller dimensions instead of rejecting valid photos.
            val transparent = mime == "image/png" && decoded.hasAlpha()
            val format = if (transparent) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            val edges = listOf(1600, 1280, 1024, 800, 640, 480)
            for (edge in edges) {
                val scale = minOf(1f, edge.toFloat() / maxOf(decoded.width, decoded.height))
                val scaled = if (scale < 1f) Bitmap.createScaledBitmap(
                    decoded, maxOf(1, (decoded.width * scale).toInt()),
                    maxOf(1, (decoded.height * scale).toInt()), true
                ) else decoded
                try {
                    val qualities = if (transparent) listOf(100) else listOf(85, 75, 65, 55)
                    for (quality in qualities) {
                        val stream = ByteArrayOutputStream()
                        check(scaled.compress(format, quality, stream)) { "Image compression failed" }
                        val bytes = stream.toByteArray()
                        if (bytes.size <= MAX_BYTES) return Result(bytes,
                            if (transparent) "image/png" else "image/jpeg",
                            if (transparent) "avatar.png" else "avatar.jpg")
                    }
                } finally {
                    if (scaled !== decoded) scaled.recycle()
                }
            }
            error("Image is too large after optimization")
        } finally {
            decoded.recycle()
        }
    }
}
