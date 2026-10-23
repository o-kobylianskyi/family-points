package com.olehkobylianskyi.familypoints.android.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.olehkobylianskyi.familypoints.android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Read-only check of the public, independently published Android APK manifest.
 * Does not use family credentials or invoke any API writes.
 */
data class AvailableAndroidUpdate(
    val versionCode: Int,
    val versionName: String
)

object AndroidUpdateChecker {
    private const val MANIFEST_URL =
        "https://family-point.com/downloads/familypoints-beta.json"
    private const val DOWNLOAD_URL =
        "https://family-point.com/downloads/familypoints-beta.apk"
    private const val PACKAGE_NAME =
        "com.olehkobylianskyi.familypoints.android"

    suspend fun check(): AvailableAndroidUpdate? = withContext(Dispatchers.IO) {
        val connection = (URL(MANIFEST_URL).openConnection() as HttpURLConnection)
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Cache-Control", "no-cache")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            if (connection.contentLengthLong > 64_000L) return@withContext null
            val json = connection.inputStream.bufferedReader(Charsets.UTF_8).use {
                it.readText().trimStart('\uFEFF')
            }
            if (json.length > 64_000) return@withContext null
            val manifest = JSONObject(json)
            if (manifest.optString("applicationId") != PACKAGE_NAME ||
                manifest.optString("file") != "familypoints-beta.apk" ||
                manifest.optString("downloadUrl") != DOWNLOAD_URL
            ) return@withContext null

            val version = manifest.optInt("versionCode", 0)
            val name = manifest.optString("versionName").take(80)
            if (version <= BuildConfig.VERSION_CODE || name.isBlank()) return@withContext null
            AvailableAndroidUpdate(version, name)
        } catch (_: Exception) {
            // Never block login or offline use because the public site is unreachable.
            null
        } finally {
            connection.disconnect()
        }
    }

    fun openDownload(context: Context) {
        // Let the browser download over HTTPS. Android's normal user-confirmed
        // sideload installer handles permissions; no silent install or broad storage access.
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DOWNLOAD_URL))
            .addCategory(Intent.CATEGORY_BROWSABLE)
        context.startActivity(intent)
    }
}
