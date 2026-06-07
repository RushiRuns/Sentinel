package com.rushi.sentinel.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricKeyManager @Inject constructor() {
    companion object {
        private const val KEY_ALIAS = "sentinel_biometric_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }

    /**
     * Retrieve or generate the AES secret key in Android Keystore,
     * enforcing biometric/credential authentication for usage.
     */
    fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val secretKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (secretKey != null) return secretKey

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
        .setUserAuthenticationRequired(true)
        .setInvalidatedByBiometricEnrollment(true) // Invalidate if new fingers are registered
        .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Deletes the biometric key from the keystore.
     */
    fun deleteBiometricKey() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }

    /**
     * Instantiates and initializes the Cipher in ENCRYPT_MODE.
     */
    fun getInitializedCipherForEncryption(): Cipher {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        val secretKey = getOrCreateSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher
    }

    /**
     * Instantiates and initializes the Cipher in DECRYPT_MODE using the original IV.
     */
    fun getInitializedCipherForDecryption(iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        val secretKey = getOrCreateSecretKey()
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        return cipher
    }

    /**
     * Encrypts the master password and zeroes intermediate buffers.
     */
    fun encryptMasterPassword(cipher: Cipher, password: CharArray): ByteArray {
        val charset = Charsets.UTF_8
        val charBuffer = java.nio.CharBuffer.wrap(password)
        val byteBuffer = charset.encode(charBuffer)
        val passwordBytes = ByteArray(byteBuffer.remaining())
        try {
            byteBuffer.get(passwordBytes)
            return cipher.doFinal(passwordBytes)
        } finally {
            // Memory safety: Clear sensitive byte arrays
            passwordBytes.fill(0)
            if (byteBuffer.hasArray()) {
                byteBuffer.array().fill(0)
            }
        }
    }

    /**
     * Decrypts the master password ciphertext and zeroes intermediate buffers.
     */
    fun decryptMasterPassword(cipher: Cipher, ciphertext: ByteArray): CharArray {
        val decryptedBytes = cipher.doFinal(ciphertext)
        try {
            val charset = Charsets.UTF_8
            val byteBuffer = java.nio.ByteBuffer.wrap(decryptedBytes)
            val charBuffer = charset.decode(byteBuffer)
            val passwordChars = CharArray(charBuffer.remaining())
            charBuffer.get(passwordChars)
            
            if (byteBuffer.hasArray()) {
                byteBuffer.array().fill(0)
            }
            return passwordChars
        } finally {
            // Memory safety: Clear sensitive decrypted bytes
            decryptedBytes.fill(0)
        }
    }
}
