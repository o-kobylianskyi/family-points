package com.olehkobylianskyi.familypoints.android.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.Gson
import com.olehkobylianskyi.familypoints.android.data.CurrentUserResponse
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

enum class OfflineLoginError {
    NOT_ENROLLED, EXPIRED, INCORRECT_PASSWORD, LOCKED
}

class OfflineLoginException(val reason: OfflineLoginError) : Exception(reason.name)

/**
 * Keystore-encrypted local profiles with salted PBKDF2 password verifiers.
 * No plaintext password, server credential, or usable token is stored here.
 * Enrollment ONLY happens after successful online authentication.
 */
class OfflineAccountStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "fp_offline_accounts", Context.MODE_PRIVATE
    )
    private val gson = Gson()
    private val alias = "family_points_offline_profile_key"
    private val random = SecureRandom()

    private data class AccountRecord(
        val user: CurrentUserResponse,
        val salt: String,
        val verifier: String,
        val lastOnlineAt: Long,
        val failedAttempts: Int,
        val lockedUntil: Long
    )

    companion object {
        private const val ITERATIONS = 210_000
        private const val OFFLINE_VALIDITY_MS = 7L * 24 * 60 * 60 * 1000
        private const val LOCK_AFTER_ATTEMPTS = 5
        private const val LOCK_DURATION_MS = 5L * 60 * 1000
    }

    private fun accountKey(username: String): String {
        val normalized = username.trim().lowercase(Locale.ROOT)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(Charsets.UTF_8))
        return "account_" + Base64.encodeToString(
            digest, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
        )
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
        }.generateKey()
    }

    private fun encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray =
        Base64.decode(value, Base64.NO_WRAP)

    private fun read(username: String): AccountRecord? {
        return try {
        val encoded = prefs.getString(accountKey(username), null) ?: return null
        val bytes = decode(encoded)
        if (bytes.size < 29) return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(
            128, bytes.copyOfRange(0, 12)
        ))
        gson.fromJson(
            String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8),
            AccountRecord::class.java
        )
        } catch (_: Exception) {
            null
        }
    }

    private fun write(username: String, record: AccountRecord) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val data = cipher.iv + cipher.doFinal(
            gson.toJson(record).toByteArray(Charsets.UTF_8)
        )
        check(prefs.edit().putString(accountKey(username), encode(data)).commit()) {
            "Cannot store offline account"
        }
    }

    private fun verifier(password: String, salt: ByteArray): ByteArray {
        val chars = password.toCharArray()
        val spec = PBEKeySpec(chars, salt, ITERATIONS, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }

    @Synchronized
    fun enroll(user: CurrentUserResponse, password: String) {
        require(password.isNotEmpty())
        val salt = ByteArray(32).also(random::nextBytes)
        val proof = verifier(password, salt)
        write(user.username, AccountRecord(
            user = user,
            salt = encode(salt),
            verifier = encode(proof),
            lastOnlineAt = System.currentTimeMillis(),
            failedAttempts = 0,
            lockedUntil = 0L
        ))
        proof.fill(0)
    }

    /** Refresh the profile only if that account was enrolled with a password. */
    @Synchronized
    fun refreshProfile(user: CurrentUserResponse) {
        val existing = read(user.username) ?: return
        write(user.username, existing.copy(user = user, lastOnlineAt = System.currentTimeMillis()))
    }

    @Synchronized
    fun authenticate(username: String, password: String): CurrentUserResponse {
        val record = read(username)
            ?: throw OfflineLoginException(OfflineLoginError.NOT_ENROLLED)
        val now = System.currentTimeMillis()
        if (now < record.lockedUntil) {
            throw OfflineLoginException(OfflineLoginError.LOCKED)
        }
        if (now < record.lastOnlineAt ||
            now - record.lastOnlineAt > OFFLINE_VALIDITY_MS) {
            throw OfflineLoginException(OfflineLoginError.EXPIRED)
        }
        val computed = verifier(password, decode(record.salt))
        val matches = MessageDigest.isEqual(computed, decode(record.verifier))
        computed.fill(0)
        if (!matches) {
            val attempts = record.failedAttempts + 1
            write(username, record.copy(
                failedAttempts = attempts,
                lockedUntil = if (attempts >= LOCK_AFTER_ATTEMPTS)
                    now + LOCK_DURATION_MS else 0L
            ))
            throw OfflineLoginException(
                if (attempts >= LOCK_AFTER_ATTEMPTS) OfflineLoginError.LOCKED
                else OfflineLoginError.INCORRECT_PASSWORD
            )
        }
        write(username, record.copy(failedAttempts = 0, lockedUntil = 0L))
        return record.user
    }

    @Synchronized
    fun remove(username: String) {
        prefs.edit().remove(accountKey(username)).commit()
    }

    @Synchronized
    fun clear() {
        prefs.edit().clear().commit()
    }
}
