package com.zhongxul.birthkeeper.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IdNumberCryptoTest {

    @Test
    fun encryptAndDecrypt_shouldReturnOriginalText() {
        val plainText = "110101199001011234"
        val encrypted = IdNumberCrypto.encrypt(plainText)
        val decrypted = IdNumberCrypto.decrypt(encrypted)

        assertEquals(plainText, decrypted)
    }

    @Test
    fun decrypt_whenInvalidCipherText_shouldFallbackToOriginal() {
        val invalidCipherText = "not-a-valid-cipher"
        assertEquals(invalidCipherText, IdNumberCrypto.decrypt(invalidCipherText))
    }

    @Test
    fun encrypt_whenBlank_shouldReturnBlank() {
        assertEquals("", IdNumberCrypto.encrypt(""))
    }
}
