package com.rushi.sentinel.crypto

import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

object BackupCrypto {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BITS = 128
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12

    data class EncryptedPayload(
        val salt: ByteArray,
        val iv: ByteArray,
        val ciphertext: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as EncryptedPayload

            if (!salt.contentEquals(other.salt)) return false
            if (!iv.contentEquals(other.iv)) return false
            if (!ciphertext.contentEquals(other.ciphertext)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = salt.contentHashCode()
            result = 31 * result + iv.contentHashCode()
            result = 31 * result + ciphertext.contentHashCode()
            return result
        }
    }

    /**
     * Encrypts the provided plaintext bytes using AES-GCM with a key derived from the password.
     * Generates a fresh salt and IV.
     */
    fun encrypt(data: ByteArray, password: CharArray): EncryptedPayload {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES).apply { random.nextBytes(this) }
        val iv = ByteArray(IV_LENGTH_BYTES).apply { random.nextBytes(this) }

        val derivedKey = KeyDerivation.deriveKey(password, salt)
        try {
            val keySpec = SecretKeySpec(derivedKey, "AES")
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
            val ciphertext = cipher.doFinal(data)
            return EncryptedPayload(salt, iv, ciphertext)
        } finally {
            // Memory safety: Zero out the derived key immediately after use
            derivedKey.fill(0)
        }
    }

    /**
     * Decrypts the provided payload using AES-GCM with a key derived from the password.
     */
    fun decrypt(payload: EncryptedPayload, password: CharArray): ByteArray {
        val derivedKey = KeyDerivation.deriveKey(password, payload.salt)
        try {
            val keySpec = SecretKeySpec(derivedKey, "AES")
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, payload.iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
            return cipher.doFinal(payload.ciphertext)
        } finally {
            // Memory safety: Zero out the derived key immediately after use
            derivedKey.fill(0)
        }
    }
}
