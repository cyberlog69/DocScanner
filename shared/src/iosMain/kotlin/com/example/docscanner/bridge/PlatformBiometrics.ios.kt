package com.example.docscanner.bridge

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
actual class PlatformBiometrics {

    actual fun isAvailable(): Boolean {
        val context = LAContext()
        return context.canEvaluatePolicy(
            LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            null
        )
    }

    actual suspend fun authenticate(title: String, subtitle: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            val context = LAContext()
            if (!context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            context.evaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                localizedReason = "$title — $subtitle"
            ) { success: Boolean, _: NSError? ->
                continuation.resume(success)
            }
        }
}
