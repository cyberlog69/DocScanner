package com.example.docscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.docscanner.ui.navigation.DocScannerNavHost
import com.example.docscanner.ui.theme.DocScannerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DocScannerApp
        setContent {
            DocScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DocScannerNavHost(appContainer = app.container)
                }
            }
        }
    }
}
