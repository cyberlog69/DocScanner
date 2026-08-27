package com.example.docscanner.bridge

expect class PlatformBiometrics {
    fun isAvailable(): Boolean
    suspend fun authenticate(title: String, subtitle: String): Boolean
}
