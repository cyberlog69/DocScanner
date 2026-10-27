package com.example.docscanner.service

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Result status of biometric hardware / credential enrollment on the device.
 */
sealed interface BiometricStatus {
    object Available : BiometricStatus
    object NotEnrolled : BiometricStatus
    object NoHardware : BiometricStatus
    object SecurityUpdateRequired : BiometricStatus
    data class Unavailable(val reason: String) : BiometricStatus

    val isAvailable: Boolean get() = this is Available
}

/**
 * Result of an individual biometric / credential authentication session.
 */
sealed interface BiometricAuthResult {
    object Success : BiometricAuthResult
    object Failed : BiometricAuthResult
    data class Canceled(val byUser: Boolean) : BiometricAuthResult
    data class Error(val errorCode: Int, val message: String) : BiometricAuthResult

    val isSuccess: Boolean get() = this is Success
}

/**
 * Production-ready Biometric and Device Credential Authentication Manager.
 * Supports Biometric Strong (Class 3), Weak (Class 2 / 2D Face), and Device PIN / Pattern.
 */
class BiometricAuthManager(private val context: Context) {

    private val isPromptActive = AtomicBoolean(false)

    companion object {
        // Broad authenticators: Strong biometrics, weak biometrics (2D Face), and Device PIN/Pattern
        private const val AUTHENTICATORS_ALL =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

        private const val AUTHENTICATORS_BIOMETRIC_ONLY =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
    }

    /**
     * Checks if biometric hardware or secure device credentials (PIN/Pattern/Password) are available and enrolled.
     */
    fun canAuthenticate(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)

        return when (val code = biometricManager.canAuthenticate(AUTHENTICATORS_ALL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Available
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NotEnrolled
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NoHardware
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.SecurityUpdateRequired
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                // Check if device credentials (PIN) might still be usable
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
                        BiometricStatus.Available
                    } else {
                        BiometricStatus.Unavailable("Biometric hardware temporarily unavailable")
                    }
                } else {
                    BiometricStatus.Unavailable("Biometric hardware temporarily unavailable")
                }
            }
            else -> BiometricStatus.Unavailable("Authentication not supported (code: $code)")
        }
    }

    /**
     * Prompts the user to authenticate using Fingerprint, Face ID, or Device PIN/Pattern.
     * Prevents duplicate dialogs via atomic state tracking.
     */
    private val keyStoreAlias = "DocScannerBiometricKey"
    private val androidKeyStore = "AndroidKeyStore"
    private val transformation =
        "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(androidKeyStore).apply { load(null) }
        val existingKey = keyStore.getKey(keyStoreAlias, null) as? SecretKey
        if (existingKey != null) return existingKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, androidKeyStore)
        val keySpec = KeyGenParameterSpec.Builder(
            keyStoreAlias,
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

    private fun createCryptoObject(): BiometricPrompt.CryptoObject {
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        return BiometricPrompt.CryptoObject(cipher)
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock DocScanner",
        subtitle: String = "Scan biometric or enter PIN to access documents",
        onResult: (BiometricAuthResult) -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            onResult(BiometricAuthResult.Error(-1, "Activity is no longer valid"))
            return
        }

        if (isPromptActive.get()) {
            // A prompt is already in flight, prevent duplicate dialogs
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isPromptActive.set(false)

                val cipher = result.cryptoObject?.cipher
                if (cipher == null) {
                    onResult(BiometricAuthResult.Error(-3, "Missing cryptographic authentication result"))
                    return
                }

                try {
                    cipher.doFinal("DocScannerAuth".toByteArray())
                    onResult(BiometricAuthResult.Success)
                } catch (e: Exception) {
                    onResult(BiometricAuthResult.Error(-4, e.localizedMessage ?: "Cryptographic verification failed"))
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                isPromptActive.set(false)

                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                        onResult(BiometricAuthResult.Canceled(byUser = true))
                    }
                    BiometricPrompt.ERROR_CANCELED -> {
                        onResult(BiometricAuthResult.Canceled(byUser = false))
                    }
                    else -> {
                        onResult(BiometricAuthResult.Error(errorCode, errString.toString()))
                    }
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(BiometricAuthResult.Failed)
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        // Configure authenticators based on Android API level
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptInfoBuilder.setAllowedAuthenticators(AUTHENTICATORS_ALL)
        } else {
            // Android 10 (API 29) and below compatibility
            @Suppress("DEPRECATION")
            try {
                promptInfoBuilder.setDeviceCredentialAllowed(true)
            } catch (_: Exception) {
                promptInfoBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                promptInfoBuilder.setNegativeButtonText("Cancel")
            }
        }

        try {
            val cryptoObject = createCryptoObject()
            isPromptActive.set(true)
            biometricPrompt.authenticate(promptInfoBuilder.build(), cryptoObject)
        } catch (e: Exception) {
            isPromptActive.set(false)
            // Fallback to biometric-only with explicit cancel button if device credential failed
            try {
                val fallbackPrompt = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(AUTHENTICATORS_BIOMETRIC_ONLY)
                    .setNegativeButtonText("Cancel")
                    .build()

                val fallbackCryptoObject = createCryptoObject()
                isPromptActive.set(true)
                biometricPrompt.authenticate(fallbackPrompt, fallbackCryptoObject)
            } catch (fallbackError: Exception) {
                isPromptActive.set(false)
                onResult(BiometricAuthResult.Error(-2, fallbackError.localizedMessage ?: "Failed to launch authentication"))
            }
        }
    }
}
