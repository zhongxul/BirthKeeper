package com.zhongxul.birthkeeper.core.common.util

import android.util.Base64
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCipher {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PBKDF2 = "PBKDF2WithHmacSHA256"
    private const val KEY_LENGTH = 256
    private const val ITERATION = 120_000
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH = 128
    private const val PASSWORD = "BirthKeeper_Backup_V1_File_Level_Secret"

    fun encrypt(plainText: String): String {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val secretKey = deriveKey(salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH, iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

        val payload = ByteBuffer.allocate(8 + salt.size + iv.size + encrypted.size)
            .putInt(salt.size)
            .put(salt)
            .putInt(iv.size)
            .put(iv)
            .put(encrypted)
            .array()
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    fun decrypt(cipherText: String): String {
        val payload = Base64.decode(cipherText, Base64.NO_WRAP)
        val buffer = ByteBuffer.wrap(payload)
        val saltLength = buffer.int
        val salt = ByteArray(saltLength).also { buffer.get(it) }
        val ivLength = buffer.int
        val iv = ByteArray(ivLength).also { buffer.get(it) }
        val encrypted = ByteArray(buffer.remaining()).also { buffer.get(it) }

        val secretKey = deriveKey(salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH, iv))
        val plain = cipher.doFinal(encrypted)
        return String(plain, StandardCharsets.UTF_8)
    }

    private fun deriveKey(salt: ByteArray): SecretKeySpec {
        val keySpec = PBEKeySpec(PASSWORD.toCharArray(), salt, ITERATION, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(PBKDF2)
        val key = factory.generateSecret(keySpec).encoded
        return SecretKeySpec(key, "AES")
    }
}
