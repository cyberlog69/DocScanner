package com.example.docscanner

import android.app.Application
import com.example.docscanner.di.appModule
import com.example.docscanner.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class DocScannerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@DocScannerApp)
            modules(listOf(sharedModule, appModule))
        }
    }
}
