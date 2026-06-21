package com.familyguard.app.security

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

data class EncryptedPayload(
    val ciphertext: ByteArray,
    val iv: ByteArray,
    val timestamp: Long
) {
    fun toTransmitFormat(): String {
        val combined = iv + ciphertext
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    companion object {
        private const val IV_SIZE = 12

        fun fromTransmitFormat(data: String): EncryptedPayload {
            val decoded = Base64.decode(data, Base64.NO_WRAP)
            val iv = decoded.sliceArray(0 until IV_SIZE)
            val ciphertext = decoded.sliceArray(IV_SIZE until decoded.size)
            return EncryptedPayload(ciphertext, iv, System.currentTimeMillis())
        }
    }
}

@Singleton
class E2EEncryptionManager @Inject constructor(
    private val keyManager: KeyManager
) {
    companion object {
        private const val TIMESTAMP_TOLERANCE_MS = 5 * 60 * 1000L // 5 minutes
    }

    fun encrypt(plaintext: ByteArray): EncryptedPayload {
        val key = keyManager.getDataEncryptionKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)

        return EncryptedPayload(
            ciphertext = ciphertext,
            iv = iv,
            timestamp = System.currentTimeMillis()
        )
    }

    fun decrypt(payload: EncryptedPayload): ByteArray {
        if (System.currentTimeMillis() - payload.timestamp > TIMESTAMP_TOLERANCE_MS) {
            throw SecurityException("Payload timestamp too old - possible replay attack")
        }

        val key = keyManager.getDataEncryptionKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, payload.iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        return cipher.doFinal(payload.ciphertext)
    }

    fun computeHmac(data: ByteArray): ByteArray {
        val hmacKey = keyManager.getHmacKey()
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(hmacKey)
        return mac.doFinal(data)
    }

    fun verifyHmac(data: ByteArray, receivedHmac: ByteArray): Boolean {
        val computedHmac = computeHmac(data)
        return constantTimeEquals(computedHmac, receivedHmac)
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].toInt() xor b[i].toInt())
        }
        return diff == 0
    }
}
