package com.example.docscanner.bridge

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlin.coroutines.resume

actual class PlatformBiometrics(private val context: Context) {

    private data class PromptCryptoPayload(
        val cryptoObject: BiometricPrompt.CryptoObject,
        val challenge: ByteArray
    )

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .build()
        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }

    private fun getCipher(): Cipher {
        return Cipher.getInstance(
            "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"
        )
    }

    private fun createPromptCryptoObject(): PromptCryptoPayload {
        val cipher = getCipher()
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val challenge = "docscanner-auth-challenge".toByteArray(Charsets.UTF_8)
        return PromptCryptoPayload(BiometricPrompt.CryptoObject(cipher), challenge)
    }

    private companion object {
        private const val KEY_ALIAS = "DocScannerBiometricKey"
    }

    actual fun isAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    actual suspend fun authenticate(title: String, subtitle: String): Boolean {
        if (!isAvailable()) return false

        val activity = context as? FragmentActivity ?: return true

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                val cryptoPayload = try {
                    createPromptCryptoObject()
                } catch (_: Exception) {
                    null
                }

                if (cryptoPayload == null) {
                    if (cont.isActive) cont.resume(false)
                    return@suspendCancellableCoroutine
                }

                val executor = ContextCompat.getMainExecutor(activity)
                val callback = object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        val authenticated = try {
                            val cipher = result.cryptoObject?.cipher ?: return
                            cipher.doFinal(cryptoPayload.challenge)
                            true
                        } catch (_: Exception) {
                            false
                        }
                        if (cont.isActive) cont.resume(authenticated)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        if (cont.isActive) cont.resume(false)
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                    }
                }

                val prompt = BiometricPrompt(activity, executor, callback)
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title.ifBlank { "Unlock DocScanner" })
                    .setSubtitle(subtitle.ifBlank { "Authenticate using Fingerprint, Face, or PIN" })
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                    .build()

                try {
                    prompt.authenticate(promptInfo, cryptoPayload.cryptoObject)
                } catch (_: Exception) {
                    try {
                        val fallbackInfo = BiometricPrompt.PromptInfo.Builder()
                            .setTitle(title.ifBlank { "Unlock DocScanner" })
                            .setSubtitle(subtitle.ifBlank { "Scan fingerprint or face" })
                            .setNegativeButtonText("Cancel")
                            .build()
                        prompt.authenticate(fallbackInfo, cryptoPayload.cryptoObject)
                    } catch (_: Exception) {
                        if (cont.isActive) cont.resume(false)
                    }
                }

                cont.invokeOnCancellation {
                    try {
                        prompt.cancelAuthentication()
                    } catch (_: Exception) {}
                }
            }
        }
    }
}

