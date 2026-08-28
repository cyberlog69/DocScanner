package com.example.docscanner

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.docscanner.service.BiometricAuthResult
import com.example.docscanner.service.BiometricStatus
import com.example.docscanner.ui.navigation.DocScannerNavHost
import com.example.docscanner.ui.theme.DocScannerTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { false }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as DocScannerApp
        val securityManager = app.container.appSecurityManager
        val biometricAuthManager = app.container.biometricAuthManager

        setContent {
            val settings by app.container.preferences.settings.collectAsStateWithLifecycle()
            val isUnlocked by securityManager.isUnlocked.collectAsStateWithLifecycle()
            val requiresAuth = settings.isBiometricLockEnabled && !isUnlocked

            var authErrorMessage by remember { mutableStateOf<String?>(null) }

            // Automatically launch biometric prompt once when lock screen appears
            LaunchedEffect(requiresAuth) {
                if (requiresAuth) {
                    val status = biometricAuthManager.canAuthenticate()
                    if (status.isAvailable) {
                        securityManager.authenticate(this@MainActivity) { result ->
                            when (result) {
                                is BiometricAuthResult.Success -> {
                                    authErrorMessage = null
                                }
                                is BiometricAuthResult.Error -> {
                                    authErrorMessage = result.message
                                }
                                is BiometricAuthResult.Failed -> {
                                    authErrorMessage = "Biometric not recognized. Please try again."
                                }
                                is BiometricAuthResult.Canceled -> {
                                    // User dismissed the prompt, remain on lock screen
                                }
                            }
                        }
                    }
                }
            }

            DocScannerTheme(themeMode = settings.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (requiresAuth) {
                        LockScreen(
                            biometricStatus = biometricAuthManager.canAuthenticate(),
                            errorMessage = authErrorMessage,
                            onUnlockClick = {
                                authErrorMessage = null
                                securityManager.authenticate(this@MainActivity) { result ->
                                    when (result) {
                                        is BiometricAuthResult.Success -> {
                                            authErrorMessage = null
                                        }
                                        is BiometricAuthResult.Error -> {
                                            authErrorMessage = result.message
                                        }
                                        is BiometricAuthResult.Failed -> {
                                            authErrorMessage = "Biometric not recognized. Please try again."
                                        }
                                        is BiometricAuthResult.Canceled -> {}
                                    }
                                }
                            },
                            onOpenSecuritySettings = {
                                try {
                                    startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                                } catch (_: Exception) {
                                    startActivity(Intent(Settings.ACTION_SETTINGS))
                                }
                            },
                            onDisableLock = {
                                app.container.preferences.setBiometricLockEnabled(false)
                                securityManager.unlock()
                            }
                        )
                    } else {
                        DocScannerNavHost(appContainer = app.container)
                    }
                }
            }
        }
    }
}

@Composable
private fun LockScreen(
    biometricStatus: BiometricStatus,
    errorMessage: String?,
    onUnlockClick: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
    onDisableLock: () -> Unit
) {
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

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        when (biometricStatus) {
            is BiometricStatus.NotEnrolled -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "No Screen Lock Configured",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Please set up a fingerprint, face, or PIN in Android Settings to use App Lock.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onOpenSecuritySettings,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Open Device Settings")
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = onDisableLock,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Disable App Lock")
                        }
                    }
                }
            }
            else -> {
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
    }
}
