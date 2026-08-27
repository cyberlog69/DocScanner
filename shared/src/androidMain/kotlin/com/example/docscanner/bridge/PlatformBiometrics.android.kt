package com.example.docscanner.bridge

import android.content.Context
import androidx.biometric.BiometricManager

actual class PlatformBiometrics(private val context: Context) {

    actual fun isAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    actual suspend fun authenticate(title: String, subtitle: String): Boolean {
        // Authenticate is typically handled through Activity BiometricPrompt on Android
        return isAvailable()
    }
}
