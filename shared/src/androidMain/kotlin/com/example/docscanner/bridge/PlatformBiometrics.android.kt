package com.example.docscanner.bridge

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

actual class PlatformBiometrics(private val context: Context) {

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
                val executor = ContextCompat.getMainExecutor(activity)
                val callback = object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        if (cont.isActive) cont.resume(true)
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
                    prompt.authenticate(promptInfo)
                } catch (_: Exception) {
                    try {
                        val fallbackInfo = BiometricPrompt.PromptInfo.Builder()
                            .setTitle(title.ifBlank { "Unlock DocScanner" })
                            .setSubtitle(subtitle.ifBlank { "Scan fingerprint or face" })
                            .setNegativeButtonText("Cancel")
                            .build()
                        prompt.authenticate(fallbackInfo)
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

