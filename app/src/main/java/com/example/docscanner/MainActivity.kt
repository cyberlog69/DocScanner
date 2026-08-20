package com.example.docscanner

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.docscanner.ui.navigation.DocScannerNavHost
import com.example.docscanner.ui.theme.DocScannerTheme

class MainActivity : FragmentActivity() {

    private var isUnlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { false }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DocScannerApp

        // Re-lock whenever the entire app leaves the foreground (HOME, task switch, etc.).
        // ProcessLifecycleOwner is used so the in-app ML Kit scanner (a sub-activity in the
        // same process) does NOT trigger a re-lock mid-scan.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                super.onStop(owner)
                if (isUnlocked && app.container.preferences.settings.value.isBiometricLockEnabled) {
                    isUnlocked = false
                }
            }
        })

        setContent {
            val settings by app.container.preferences.settings.collectAsStateWithLifecycle()
            val requiresAuth = settings.isBiometricLockEnabled && !isUnlocked

            LaunchedEffect(settings.isBiometricLockEnabled) {
                if (settings.isBiometricLockEnabled && !isUnlocked) {
                    authenticateUser()
                }
            }

            DocScannerTheme(themeMode = settings.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (requiresAuth) {
                        LockScreen(onUnlockClick = { authenticateUser() })
                    } else {
                        DocScannerNavHost(appContainer = app.container)
                    }
                }
            }
        }
    }

    private fun authenticateUser() {
        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isUnlocked = true
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // Stay locked until authenticated
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        }

        val biometricPrompt = BiometricPrompt(this, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock DocScanner")
            .setSubtitle("Authenticate using your Fingerprint, Face, or PIN")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (_: Exception) {
            // Fallback for devices where DEVICE_CREDENTIAL is not supported together with BIOMETRIC
            try {
                val fallbackPrompt = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock DocScanner")
                    .setSubtitle("Scan fingerprint or face")
                    .setNegativeButtonText("Cancel")
                    .build()
                biometricPrompt.authenticate(fallbackPrompt)
            } catch (_: Exception) {}
        }
    }
}

@Composable
private fun LockScreen(onUnlockClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "DocScanner is Locked",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Authentication required to protect your scanned documents and sensitive files.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onUnlockClick,
            shape = CircleShape,
            modifier = Modifier.height(50.dp)
        ) {
            Icon(Icons.Default.Fingerprint, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Unlock with Biometrics / PIN")
        }
    }
}
