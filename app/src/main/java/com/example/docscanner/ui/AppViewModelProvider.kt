package com.example.docscanner.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.docscanner.DocScannerApp
import com.example.docscanner.ui.camera.ScanViewModel
import com.example.docscanner.ui.documents.DocumentDetailViewModel
import com.example.docscanner.ui.documents.DocumentListViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            val app = docScannerApp()
            ScanViewModel(
                repository = app.container.repository,
                ocrService = app.container.ocrService,
                fileStorageService = app.container.fileStorageService,
                pdfGenerator = app.container.pdfGenerator
            )
        }
        initializer {
            val app = docScannerApp()
            DocumentListViewModel(
                repository = app.container.repository,
                fileStorageService = app.container.fileStorageService
            )
        }
    }
}

fun CreationExtras.docScannerApp(): DocScannerApp =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as DocScannerApp)
