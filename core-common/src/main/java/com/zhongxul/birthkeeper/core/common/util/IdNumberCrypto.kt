package com.zhongxul.birthkeeper.core.common.util

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object IdNumberCrypto {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "birthkeeper_id_number_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128

    fun encrypt(plainText: String): String {
        return runCatching {
            if (plainText.isBlank()) {
                plainText
            } else {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
                val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
                val payload = ByteBuffer.allocate(4 + cipher.iv.size + encryptedBytes.size)
                    .putInt(cipher.iv.size)
                    .put(cipher.iv)
                    .put(encryptedBytes)
                    .array()
                Base64.encodeToString(payload, Base64.NO_WRAP)
            }
        }.getOrElse { plainText }
    }

    fun decrypt(cipherText: String): String {
        return runCatching {
            if (cipherText.isBlank()) {
                cipherText
            } else {
                val payload = Base64.decode(cipherText, Base64.NO_WRAP)
                val buffer = ByteBuffer.wrap(payload)
                val ivLength = buffer.int
                val iv = ByteArray(ivLength)
                buffer.get(iv)
                val encrypted = ByteArray(buffer.remaining())
                buffer.get(encrypted)

                val cipher = Cipher.getInstance(TRANSFORMATION)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
                val plainBytes = cipher.doFinal(encrypted)
                String(plainBytes, StandardCharsets.UTF_8)
            }
        }.getOrElse { cipherText }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) {
            return existing
        }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setUnlockedDeviceRequired(true)
        }
        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }
}
