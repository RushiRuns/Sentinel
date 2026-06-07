package com.rushi.sentinel.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.security.GeneralSecurityException

class BackupCryptoTest {

    @Test
    fun testBackupCrypto_roundtrip() {
        val originalData = "Super secret vault content data".toByteArray(Charsets.UTF_8)
        val password = "BackupPassword123!".toCharArray()

        val payload = BackupCrypto.encrypt(originalData, password)
        assertNotNull("Payload must not be null", payload)
        assertFalse("Salt must not be empty", payload.salt.isEmpty())
        assertFalse("IV must not be empty", payload.iv.isEmpty())
        assertFalse("Ciphertext must not be empty", payload.ciphertext.isEmpty())

        val decryptedData = BackupCrypto.decrypt(payload, password)
        assertArrayEquals("Decrypted data must match original", originalData, decryptedData)
    }

    @Test
    fun testBackupCrypto_wrongPasswordFails() {
        val originalData = "My passwords listing".toByteArray(Charsets.UTF_8)
        val correctPassword = "CorrectPassword!".toCharArray()
        val wrongPassword = "WrongPassword!".toCharArray()

        val payload = BackupCrypto.encrypt(originalData, correctPassword)

        try {
            BackupCrypto.decrypt(payload, wrongPassword)
            fail("Decryption with wrong password should fail and throw an exception")
        } catch (e: Exception) {
            // Usually throws javax.crypto.AEADBadTagException or GeneralSecurityException due to tag validation failure
            assertTrue("Expected cryptographic exception", e is GeneralSecurityException || e.cause is GeneralSecurityException)
        }
    }

    @Test
    fun testBackupCrypto_randomnessOfSaltAndIv() {
        val data = "Fixed data to encrypt".toByteArray(Charsets.UTF_8)
        val password = "ConstantPassword".toCharArray()

        val payload1 = BackupCrypto.encrypt(data, password)
        val payload2 = BackupCrypto.encrypt(data, password)

        // Multiple encryptions must use different salts and IVs (and thus produce different ciphertexts)
        assertFalse("Salts should be different", payload1.salt.contentEquals(payload2.salt))
        assertFalse("IVs should be different", payload1.iv.contentEquals(payload2.iv))
        assertFalse("Ciphertexts should be different", payload1.ciphertext.contentEquals(payload2.ciphertext))
    }
}
