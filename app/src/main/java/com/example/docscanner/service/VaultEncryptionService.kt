package com.example.docscanner.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Provides hardware-backed AES-256 GCM encryption and decryption for Vault documents.
 */
object VaultEncryptionService {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "DocScannerVaultMasterKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12

    init {
        ensureKeyExists()
    }

    private fun ensureKeyExists() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val keyGenSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build()
                keyGenerator.init(keyGenSpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            Log.e("VaultEncryptionService", "Failed to initialize vault master key", e)
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * Encrypts a file in place using AES-256 GCM.
     * Output format: [1-byte IV length] + [IV bytes] + [Ciphertext + Auth Tag]
     */
    fun encryptFile(file: File): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        return try {
            val plaintext = file.readBytes()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext)

            val tempEncFile = File(file.parentFile, "${file.name}.tmp")
            FileOutputStream(tempEncFile).use { fos ->
                fos.write(iv.size)
                fos.write(iv)
                fos.write(ciphertext)
            }

            tempEncFile.renameTo(file)
            true
        } catch (e: Exception) {
            Log.e("VaultEncryptionService", "Failed to encrypt file ${file.name}", e)
            false
        }
    }

    /**
     * Decrypts a file encrypted with [encryptFile] into memory.
     */
    fun decryptFileToBytes(file: File): ByteArray? {
        if (!file.exists() || file.length() <= IV_LENGTH + 1) return null
        return try {
            FileInputStream(file).use { fis ->
                val ivSize = fis.read()
                if (ivSize <= 0 || ivSize > 32) return null
                val iv = ByteArray(ivSize)
                fis.read(iv)
                val ciphertext = fis.readBytes()

                val cipher = Cipher.getInstance(TRANSFORMATION)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
                cipher.doFinal(ciphertext)
            }
        } catch (e: Exception) {
            Log.e("VaultEncryptionService", "Failed to decrypt file ${file.name}", e)
            null
        }
    }

    /**
     * Decrypts a file in place back to plaintext.
     */
    fun decryptFileInPlace(file: File): Boolean {
        val decryptedBytes = decryptFileToBytes(file) ?: return false
        return try {
            file.writeBytes(decryptedBytes)
            true
        } catch (e: Exception) {
            Log.e("VaultEncryptionService", "Failed to overwrite with decrypted bytes", e)
            false
        }
    }

    /**
     * Decrypts an encrypted image file directly into a Bitmap without writing plaintext to disk.
     */
    fun loadDecryptedBitmap(file: File): Bitmap? {
        val bytes = decryptFileToBytes(file) ?: return null
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}
