package com.rushi.sentinel.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.security.SecureRandom
import java.util.Arrays

class KeyDerivationTest {

    @Test
    fun testKeyDerivation_consistency() {
        val password = "StrongMasterPassword123!".toCharArray()
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }

        val key1 = KeyDerivation.deriveKey(password, salt)
        val key2 = KeyDerivation.deriveKey(password, salt)

        try {
            assertArrayEquals("Deriving key twice with same password and salt must yield identical keys", key1, key2)
            assertEquals("Key length must be 32 bytes (256 bits)", 32, key1.size)
        } finally {
            // Clean up derived keys in memory
            key1.fill(0)
            key2.fill(0)
        }
    }

    @Test
    fun testKeyDerivation_differentPasswordsProduceDifferentKeys() {
        val password1 = "PasswordOne".toCharArray()
        val password2 = "PasswordTwo".toCharArray()
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }

        val key1 = KeyDerivation.deriveKey(password1, salt)
        val key2 = KeyDerivation.deriveKey(password2, salt)

        try {
            assertFalse("Different passwords must yield different keys", Arrays.equals(key1, key2))
        } finally {
            key1.fill(0)
            key2.fill(0)
        }
    }

    @Test
    fun testKeyDerivation_differentSaltsProduceDifferentKeys() {
        val password = "SamePassword".toCharArray()
        val salt1 = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val salt2 = ByteArray(16).apply { SecureRandom().nextBytes(this) }

        val key1 = KeyDerivation.deriveKey(password, salt1)
        val key2 = KeyDerivation.deriveKey(password, salt2)

        try {
            assertFalse("Different salts must yield different keys", Arrays.equals(key1, key2))
        } finally {
            key1.fill(0)
            key2.fill(0)
        }
    }
}
