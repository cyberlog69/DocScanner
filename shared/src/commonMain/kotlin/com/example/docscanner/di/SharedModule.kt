package com.example.docscanner.di

import com.example.docscanner.model.CategoryClassifier
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Multiplatform shared Koin module providing pure domain services.
 */
val sharedModule: Module = module {
    single { CategoryClassifier }
}

