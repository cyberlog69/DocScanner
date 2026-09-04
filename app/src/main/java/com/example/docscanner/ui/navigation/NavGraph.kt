package com.example.docscanner.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.docscanner.AppContainer
import com.example.docscanner.ui.camera.ProcessingScreen
import com.example.docscanner.ui.camera.ScanViewModel
import com.example.docscanner.ui.camera.ScannerLaunchScreen
import com.example.docscanner.ui.documents.DocumentDetailScreen
import com.example.docscanner.ui.documents.DocumentDetailViewModel
import com.example.docscanner.ui.documents.DocumentListScreen
import com.example.docscanner.ui.documents.DocumentListViewModel

sealed class Screen(val route: String) {
    data object DocumentList : Screen("document_list")
    data object Scanner : Screen("scanner?documentId={documentId}") {
        fun createRoute(documentId: String? = null): String {
            return if (documentId != null) "scanner?documentId=$documentId" else "scanner"
        }
    }
    data object DocumentDetail : Screen("document_detail/{documentId}?query={query}&rename={rename}") {
        fun createRoute(documentId: String, query: String? = null, rename: Boolean = false): String {
            val args = mutableListOf<String>()
            query?.let { args += "query=$it" }
            if (rename) args += "rename=true"
            val queryString = if (args.isEmpty()) "" else "?${args.joinToString("&")}"
            return "document_detail/$documentId$queryString"
        }
    }
}

@Composable
fun DocScannerNavHost(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.DocumentList.route,
        modifier = modifier
    ) {
        composable(Screen.DocumentList.route) {
            val listViewModel: DocumentListViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return DocumentListViewModel(
                            repository = appContainer.repository,
                            fileStorageService = appContainer.fileStorageService,
                            pdfGenerator = appContainer.pdfGenerator,
                            preferences = appContainer.preferences,
                            appUpdateService = appContainer.appUpdateService
                        ) as T
                    }
                }
            )
            val scanViewModel: ScanViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ScanViewModel(
                            repository = appContainer.repository,
                            ocrService = appContainer.ocrService,
                            fileStorageService = appContainer.fileStorageService,
                            pdfGenerator = appContainer.pdfGenerator,
                            preferences = appContainer.preferences,
                            context = appContainer.context
                        ) as T
                    }
                }
            )
            val scanState by scanViewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(scanState.savedDocumentId) {
                scanState.savedDocumentId?.let { id ->
                    navController.navigate(Screen.DocumentDetail.createRoute(id, rename = true))
                    scanViewModel.resetState()
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                DocumentListScreen(
                    onNavigateToDocument = { id, query ->
                        navController.navigate(Screen.DocumentDetail.createRoute(id, query = query))
                    },
                    onStartScan = {
                        navController.navigate(Screen.Scanner.route)
                    },
                    onImportGallery = { uris ->
                        scanViewModel.onScanComplete(uris)
                    },
                    viewModel = listViewModel,
                    preferences = appContainer.preferences,
                    backupRestoreService = appContainer.backupRestoreService,
                    biometricAuthManager = appContainer.biometricAuthManager,
                    onProcessIdCard = { front, back ->
                        scanViewModel.processIdCard(front, back)
                    }
                )

                if (scanState.isProcessing) {
                    ProcessingScreen(step = scanState.processingStep)
                }
            }
        }

        composable(
            route = Screen.Scanner.route,
            arguments = listOf(
                navArgument("documentId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val existingDocId = backStackEntry.arguments?.getString("documentId")
            val scanViewModel: ScanViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ScanViewModel(
                            repository = appContainer.repository,
                            ocrService = appContainer.ocrService,
                            fileStorageService = appContainer.fileStorageService,
                            pdfGenerator = appContainer.pdfGenerator,
                            preferences = appContainer.preferences,
                            context = appContainer.context
                        ) as T
                    }
                }
            )
            ScannerLaunchScreen(
                scannerService = appContainer.documentScannerService,
                onScanComplete = { documentId ->
                    navController.navigate(Screen.DocumentDetail.createRoute(documentId, rename = existingDocId == null)) {
                        popUpTo(Screen.DocumentList.route)
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModel = scanViewModel,
                existingDocumentId = existingDocId
            )
        }

        composable(
            route = Screen.DocumentDetail.route,
            arguments = listOf(
                navArgument("documentId") { type = NavType.StringType },
                navArgument("query") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                },
                navArgument("rename") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId") ?: ""
            val searchQuery = backStackEntry.arguments?.getString("query").orEmpty()
            val autoRename = backStackEntry.arguments?.getBoolean("rename") ?: false
            val detailViewModel: DocumentDetailViewModel = viewModel(
                factory = DocumentDetailViewModel.provideFactory(
                    repository = appContainer.repository,
                    fileStorageService = appContainer.fileStorageService,
                    pdfGenerator = appContainer.pdfGenerator,
                    ocrService = appContainer.ocrService,
                    preferences = appContainer.preferences,
                    documentId = documentId
                )
            )
            DocumentDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                searchQuery = searchQuery,
                autoRename = autoRename,
                onOpenDocument = { newId ->
                    navController.navigate(Screen.DocumentDetail.createRoute(newId)) {
                        popUpTo(Screen.DocumentList.route)
                    }
                },
                onAddPages = { docId ->
                    navController.navigate(Screen.Scanner.createRoute(docId))
                },
                viewModel = detailViewModel
            )
        }
    }
}
