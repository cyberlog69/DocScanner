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
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Provides hardware-backed AES-256 GCM encryption and decryption for Vault documents.
 *
 * File format: `[1-byte IV length][IV][ciphertext + auth tag]`.
 * Encryption and in-place decryption stream through the cipher, so large PDFs are
 * never fully buffered in memory.
 */
object VaultEncryptionService {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "DocScannerVaultMasterKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12
    private const val MAX_IV_LENGTH = 32

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
     * Returns true when [file] starts with a plausible IV-length header.
     *
     * Page images are JPEG (0xFF), PDFs start with '%' (0x25) and PNGs with 0x89,
     * all of which are outside the valid 1..32 IV-length range, so plaintext files
     * are never mistaken for ciphertext.
     */
    fun isEncrypted(file: File): Boolean {
        if (!file.exists() || file.length() <= IV_LENGTH + 1) return false
        return try {
            FileInputStream(file).use { fis ->
                val header = fis.read()
                header in 1..MAX_IV_LENGTH
            }
        } catch (e: Exception) {
            Log.e("VaultEncryptionService", "Failed to inspect ${file.name}", e)
            false
        }
    }

    /**
     * Encrypts a file in place using AES-256 GCM. The original is replaced only after
     * the ciphertext is fully written.
     */
    fun encryptFile(file: File): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        val tempEncFile = File(file.parentFile, "${file.name}.tmp")
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv

            FileOutputStream(tempEncFile).use { fos ->
                fos.write(iv.size)
                fos.write(iv)
                CipherOutputStream(fos, cipher).use { cos ->
                    file.inputStream().use { input -> input.copyTo(cos) }
                }
            }

            if (!tempEncFile.renameTo(file)) {
                tempEncFile.delete()
                Log.e("VaultEncryptionService", "Failed to replace ${file.name} with encrypted output")
                false
            } else {
                true
            }
        } catch (e: Exception) {
            tempEncFile.delete()
            Log.e("VaultEncryptionService", "Failed to encrypt file ${file.name}", e)
            false
        }
    }

    /**
     * Encrypts [file] unless it is already ciphertext.
     */
    fun encryptFileIfNeeded(file: File): Boolean = !file.exists() || isEncrypted(file) || encryptFile(file)

    /**
     * Decrypts a file encrypted with [encryptFile] into memory.
     * Used for bitmaps, which must be decoded from a byte array anyway.
     */
    fun decryptFileToBytes(file: File): ByteArray? {
        if (!file.exists() || file.length() <= IV_LENGTH + 1) return null
        return try {
            FileInputStream(file).use { fis ->
                val ivSize = fis.read()
                if (ivSize <= 0 || ivSize > MAX_IV_LENGTH) return null
                val iv = ByteArray(ivSize)
                if (fis.read(iv) != ivSize) return null

                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
                CipherInputStream(fis, cipher).use { it.readBytes() }
            }
        } catch (e: Exception) {
            Log.e("VaultEncryptionService", "Failed to decrypt file ${file.name}", e)
            null
        }
    }

    /**
     * Decrypts a file in place back to plaintext. Writes to a temporary file first so a
     * failed authentication tag never destroys the ciphertext.
     */
    fun decryptFileInPlace(file: File): Boolean {
        if (!file.exists() || file.length() <= IV_LENGTH + 1) return false
        val tempPlainFile = File(file.parentFile, "${file.name}.tmp")
        return try {
            FileInputStream(file).use { fis ->
                val ivSize = fis.read()
                if (ivSize <= 0 || ivSize > MAX_IV_LENGTH) {
                    Log.e("VaultEncryptionService", "Invalid IV header in ${file.name}")
                    return false
                }
                val iv = ByteArray(ivSize)
                if (fis.read(iv) != ivSize) {
                    Log.e("VaultEncryptionService", "Truncated IV in ${file.name}")
                    return false
                }

                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
                FileOutputStream(tempPlainFile).use { fos ->
                    CipherInputStream(fis, cipher).use { cis -> cis.copyTo(fos) }
                }
            }

            if (!tempPlainFile.renameTo(file)) {
                tempPlainFile.delete()
                Log.e("VaultEncryptionService", "Failed to replace ${file.name} with decrypted output")
                false
            } else {
                true
            }
        } catch (e: Exception) {
            tempPlainFile.delete()
            Log.e("VaultEncryptionService", "Failed to decrypt file in place ${file.name}", e)
            false
        }
    }

    /**
     * Decrypts [file] unless it is already plaintext.
     */
    fun decryptFileIfNeeded(file: File): Boolean = !file.exists() || !isEncrypted(file) || decryptFileInPlace(file)

    /**
     * Decrypts an encrypted image file directly into a Bitmap without writing plaintext to disk.
     */
    fun loadDecryptedBitmap(file: File): Bitmap? {
        val bytes = decryptFileToBytes(file) ?: return null
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}
