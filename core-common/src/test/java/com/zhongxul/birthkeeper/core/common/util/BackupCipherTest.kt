package com.zhongxul.birthkeeper.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupCipherTest {

    @Test
    fun encryptAndDecrypt_shouldKeepPlainText() {
        val plainText = """{"name":"张三","birthday":"1990-01-01"}"""
        val cipherText = BackupCipher.encrypt(plainText)
        val decrypted = BackupCipher.decrypt(cipherText)

        assertNotEquals(plainText, cipherText)
        assertEquals(plainText, decrypted)
    }

    @Test
    fun encrypt_whenSameInputTwice_shouldProduceDifferentCipherText() {
        val plainText = "same-input"
        val first = BackupCipher.encrypt(plainText)
        val second = BackupCipher.encrypt(plainText)

        assertNotEquals(first, second)
    }
}
