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
    data object Scanner : Screen("scanner")
    data object DocumentDetail : Screen("document_detail/{documentId}") {
        fun createRoute(documentId: String) = "document_detail/$documentId"
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
                            fileStorageService = appContainer.fileStorageService
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
                    navController.navigate(Screen.DocumentDetail.createRoute(id))
                    scanViewModel.resetState()
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                DocumentListScreen(
                    onNavigateToDocument = { id ->
                        navController.navigate(Screen.DocumentDetail.createRoute(id))
                    },
                    onStartScan = {
                        navController.navigate(Screen.Scanner.route)
                    },
                    onImportGallery = { uris ->
                        scanViewModel.onScanComplete(uris)
                    },
                    viewModel = listViewModel,
                    preferences = appContainer.preferences
                )

                if (scanState.isProcessing) {
                    ProcessingScreen(step = scanState.processingStep)
                }
            }
        }

        composable(Screen.Scanner.route) {
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
                    navController.navigate(Screen.DocumentDetail.createRoute(documentId)) {
                        popUpTo(Screen.DocumentList.route)
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModel = scanViewModel
            )
        }

        composable(
            route = Screen.DocumentDetail.route,
            arguments = listOf(navArgument("documentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId") ?: ""
            val detailViewModel: DocumentDetailViewModel = viewModel(
                factory = DocumentDetailViewModel.provideFactory(
                    repository = appContainer.repository,
                    fileStorageService = appContainer.fileStorageService,
                    pdfGenerator = appContainer.pdfGenerator,
                    documentId = documentId
                )
            )
            DocumentDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = detailViewModel
            )
        }
    }
}
