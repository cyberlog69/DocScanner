package com.example.docscanner.di

import com.example.docscanner.data.db.AppDatabase
import com.example.docscanner.data.db.DocumentDao
import com.example.docscanner.data.pref.ScannerPreferences
import com.example.docscanner.data.repository.DocumentRepository
import com.example.docscanner.repository.DocumentRepository as IDocumentRepository
import com.example.docscanner.service.DocumentScannerService
import com.example.docscanner.service.FileStorageService
import com.example.docscanner.service.OcrService
import com.example.docscanner.service.PdfGenerator
import com.example.docscanner.ui.camera.ScanViewModel
import com.example.docscanner.ui.documents.DocumentDetailViewModel
import com.example.docscanner.ui.documents.DocumentListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Android Koin module providing database, DAO, repository, platform services, and ViewModels.
 */
val appModule = module {
    // Database & Storage
    single { AppDatabase(get()) }
    single<DocumentDao> { get<AppDatabase>().documentDao }
    single<IDocumentRepository> { DocumentRepository(get()) }
    single { DocumentRepository(get()) }
    single { ScannerPreferences(get()) }
    single { FileStorageService(get()) }
    single { DocumentScannerService(get()) }
    single { OcrService(get()) }
    single { PdfGenerator() }
    single { com.example.docscanner.service.AppUpdateService(get()) }

    // ViewModels
    viewModel { DocumentListViewModel(get(), get(), get(), get(), get()) }
    viewModel { (documentId: String) ->
        DocumentDetailViewModel(get(), get(), get(), get(), get(), documentId)
    }
    viewModel { ScanViewModel(get(), get(), get(), get(), get(), get()) }
}
