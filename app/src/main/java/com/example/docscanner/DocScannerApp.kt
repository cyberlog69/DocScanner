package com.example.docscanner

import android.app.Application
import android.content.Context
import com.example.docscanner.data.db.AppDatabase
import com.example.docscanner.data.db.DocumentDao
import com.example.docscanner.data.repository.DocumentRepository
import com.example.docscanner.service.DocumentScannerService
import com.example.docscanner.service.FileStorageService
import com.example.docscanner.service.OcrService
import com.example.docscanner.service.PdfGenerator

class AppContainer(val context: Context) {
    val database: AppDatabase by lazy { AppDatabase(context) }
    val documentDao: DocumentDao by lazy { database.documentDao }
    val repository: DocumentRepository by lazy { DocumentRepository(documentDao) }
    val ocrService: OcrService by lazy { OcrService(context) }
    val documentScannerService: DocumentScannerService by lazy { DocumentScannerService(context) }
    val pdfGenerator: PdfGenerator by lazy { PdfGenerator() }
    val fileStorageService: FileStorageService by lazy { FileStorageService(context) }
}

class DocScannerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
