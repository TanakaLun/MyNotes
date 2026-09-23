package io.github.tanakalun.mynotes.data

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val TAG_BITS = 128

    private val kf = charArrayOf(
        'M', 'y', 'N', 'o', 't', 'e', 's',
        '_', 'B', 'k', '_', '2', '0', '2',
        '6', '_', 'S', 'e', 'c', 'r', 'e',
        't', '_', 'K', '3', 'y', '_', '9',
        'f', 'A', '3', 'd', 'L', 'x', '7',
        'Q', 'p', 'Z', 'r', '4', 'W', 'm',
        'H', 'n', 'B', '8', 'v', 'T', 'c',
        'E', 's', 'U', 'j', '5', 'y', 'R',
    )

    private val ivF = charArrayOf(
        'N', 'V', 'E', 'c', 'T', 'V', 'k',
        'Q', '3', 'R', 'h', 'Z', 'W', 'x',
        'v', 'M', '0', 'F', 'u', 'T', 'V',
        '9', 'w', 'c', '3', 'B', 'v', 'Y',
    )

    private fun keyBytes(): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(String(kf).toByteArray(Charsets.UTF_8))

    private fun ivBytes(): ByteArray = String(ivF).toByteArray(Charsets.UTF_8).copyOf(IV_SIZE)

    fun encrypt(data: ByteArray): ByteArray {
        val c = Cipher.getInstance(TRANSFORMATION)
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes(), "AES"), GCMParameterSpec(TAG_BITS, ivBytes()))
        return c.doFinal(data)
    }

    fun decrypt(data: ByteArray): ByteArray {
        val c = Cipher.getInstance(TRANSFORMATION)
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes(), "AES"), GCMParameterSpec(TAG_BITS, ivBytes()))
        return c.doFinal(data)
    }
}
