package com.example.docscanner

import android.app.Application
import android.content.Context
import com.example.docscanner.data.db.AppDatabase
import com.example.docscanner.data.db.DocumentDao
import com.example.docscanner.data.pref.ScannerPreferences
import com.example.docscanner.data.repository.DocumentRepository
import com.example.docscanner.di.appModule
import com.example.docscanner.di.sharedModule
import com.example.docscanner.service.DocumentScannerService
import com.example.docscanner.service.FileStorageService
import com.example.docscanner.service.OcrService
import com.example.docscanner.service.PdfGenerator
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class AppContainer(val context: Context) {
    val preferences: ScannerPreferences by lazy { ScannerPreferences(context) }
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

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@DocScannerApp)
            modules(listOf(sharedModule, appModule))
        }
    }
}

