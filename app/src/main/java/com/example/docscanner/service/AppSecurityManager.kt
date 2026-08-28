package com.example.docscanner.service

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.docscanner.data.pref.ScannerPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages process-level security, session unlock states, and background lock grace periods.
 * Ensures screen rotation, system dialogs (scanner, image picker), and short app switches
 * do not cause unexpected lockouts.
 */
class AppSecurityManager(
    private val context: Context,
    private val preferences: ScannerPreferences,
    val biometricAuthManager: BiometricAuthManager
) {

    companion object {
        /**
         * Grace period in milliseconds before locking the app after moving to background.
         * Allows quick image picking, Google Document Scanner flows, and phone call handling
         * without re-locking mid-action.
         */
        const val LOCK_GRACE_PERIOD_MS = 30_000L // 30 seconds
    }

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var backgroundTimestamp: Long = 0L

    init {
        // Observe application process lifecycle across all activities
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                handleAppForeground()
            }

            override fun onStop(owner: LifecycleOwner) {
                super.onStop(owner)
                handleAppBackground()
            }
        })
    }

    /**
     * Called when the app process enters the background.
     */
    private fun handleAppBackground() {
        backgroundTimestamp = System.currentTimeMillis()
    }

    /**
     * Called when the app process returns to the foreground.
     */
    private fun handleAppForeground() {
        val isLockEnabled = preferences.settings.value.isBiometricLockEnabled
        if (!isLockEnabled) {
            _isUnlocked.value = true
            return
        }

        if (backgroundTimestamp > 0L) {
            val timeInBackground = System.currentTimeMillis() - backgroundTimestamp
            if (timeInBackground > LOCK_GRACE_PERIOD_MS) {
                // Session expired beyond grace period -> re-lock
                _isUnlocked.value = false
            }
            // Reset timestamp
            backgroundTimestamp = 0L
        }
    }

    /**
     * Checks if authentication is required right now.
     */
    fun isAuthenticationRequired(): Boolean {
        val isLockEnabled = preferences.settings.value.isBiometricLockEnabled
        return isLockEnabled && !_isUnlocked.value
    }

    /**
     * Manually mark the session as unlocked.
     */
    fun unlock() {
        _isUnlocked.value = true
    }

    /**
     * Manually lock the session immediately.
     */
    fun lock() {
        _isUnlocked.value = false
    }

    /**
     * Prompts the user to authenticate using biometrics or device PIN/pattern.
     */
    fun authenticate(
        activity: FragmentActivity,
        onResult: (BiometricAuthResult) -> Unit
    ) {
        biometricAuthManager.authenticate(
            activity = activity,
            title = "Unlock DocScanner",
            subtitle = "Authenticate to access your documents",
            onResult = { result ->
                if (result.isSuccess) {
                    unlock()
                }
                onResult(result)
            }
        )
    }
}
