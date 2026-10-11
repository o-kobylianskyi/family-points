package com.olehkobylianskyi.familypoints.android.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.Gson
import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypted account snapshot. It is for identity display and a future
 * locally authenticated offline session; reading it does NOT authenticate anyone.
 */
class OfflineAccountStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("fp_offline_accounts", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val alias = "family_points_offline_profile_key"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
        }.generateKey()
    }

    fun save(user: CurrentUserResponse) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(gson.toJson(user).toByteArray(Charsets.UTF_8))
        val encoded = Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
        prefs.edit().putString("profile", encoded).apply()
    }

    fun load(): CurrentUserResponse? = try {
        val stored = prefs.getString("profile", null) ?: return null
        val data = Base64.decode(stored, Base64.NO_WRAP)
        if (data.size < 29) return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, data.copyOfRange(0, 12)))
        gson.fromJson(String(cipher.doFinal(data.copyOfRange(12, data.size)), Charsets.UTF_8),
            CurrentUserResponse::class.java)
    } catch (_: Exception) {
        null
    }

    fun clear() {
        prefs.edit().remove("profile").apply()
    }
}
