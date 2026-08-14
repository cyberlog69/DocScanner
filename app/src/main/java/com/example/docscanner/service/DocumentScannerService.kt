package com.example.docscanner.service

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wraps the ML Kit Document Scanner API.
 * Provides automatic document edge detection and perspective correction.
 */
class DocumentScannerService(
    private val context: Context
) {
    companion object {
        const val SCAN_REQUEST_CODE = 9001
    }

    private val scannerOptions = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)
        .setPageLimit(20)
        .setResultFormats(
            GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
            GmsDocumentScannerOptions.RESULT_FORMAT_PDF
        )
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .build()

    private val scanner = GmsDocumentScanning.getClient(scannerOptions)

    /**
     * Gets an IntentSender to launch the document scanner UI.
     * The caller must use [Activity.startIntentSenderForResult] with [SCAN_REQUEST_CODE].
     */
    suspend fun getScannerIntentSender(activity: Activity): IntentSender =
        suspendCancellableCoroutine { cont ->
            scanner.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    cont.resume(intentSender)
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }

    /**
     * Parses the result from [Activity.onActivityResult] for the scanner.
     */
    fun parseScanResult(resultCode: Int, data: android.content.Intent?): GmsDocumentScanningResult? {
        return GmsDocumentScanningResult.fromActivityResultIntent(data)
    }
}
