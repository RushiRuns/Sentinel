package com.rushi.sentinel.crypto

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object KeyDerivation {
    private const val ITERATIONS = 310000
    private const val KEY_LENGTH = 256 // in bits

    /**
     * Derives a 256-bit encryption key from a master password and salt using PBKDF2WithHmacSHA256.
     * Ensure the returned ByteArray is zeroed after use when possible.
     */
    fun deriveKey(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        try {
            val key = factory.generateSecret(spec).encoded
            return key
        } finally {
            // Memory safety: wipe the master password from internal buffer of PBEKeySpec
            spec.clearPassword()
        }
    }
}
