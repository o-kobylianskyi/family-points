package com.olehkobylianskyi.familypoints.android.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Download only a manifest-verified signed APK into private app cache. */
object AndroidApkInstaller {
    private const val URL_APK = "https://family-point.com/downloads/familypoints-beta.apk"
    private const val AUTHORITY = "com.olehkobylianskyi.familypoints.android.updateprovider"

    suspend fun download(
        context: Context,
        update: AvailableAndroidUpdate,
        onProgress: (Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val temp = File(directory, "incoming.tmp")
        val target = File(directory, "familypoints-beta-${update.versionCode}.apk")
        val digest = MessageDigest.getInstance("SHA-256")
        val connection = (URL(URL_APK).openConnection() as HttpURLConnection)
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "Download failed: HTTP ${connection.responseCode}" }
            val declared = connection.contentLengthLong
            check(declared < 0 || declared == update.sizeBytes) { "APK size differs from manifest" }
            var downloaded = 0L
            connection.inputStream.use { input ->
                temp.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        downloaded += n
                        check(downloaded <= update.sizeBytes) { "APK exceeds expected size" }
                        output.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        onProgress((downloaded * 100L / update.sizeBytes).toInt().coerceIn(0, 100))
                    }
                }
            }
            check(downloaded == update.sizeBytes) { "Incomplete APK" }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            check(actual == update.sha256) { "APK checksum mismatch" }
            if (target.exists()) target.delete()
            check(temp.renameTo(target)) { "Cannot prepare APK installer" }
            target
        } finally {
            connection.disconnect()
            temp.delete()
        }
    }

    fun install(context: Context, apk: File) {
        val uri: Uri = FileProvider.getUriForFile(context, AUTHORITY, apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
