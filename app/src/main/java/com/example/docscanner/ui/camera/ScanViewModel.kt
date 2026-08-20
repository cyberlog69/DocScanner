package com.example.docscanner.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.docscanner.data.model.CategoryClassifier
import com.example.docscanner.data.model.Document
import com.example.docscanner.data.model.Page
import com.example.docscanner.data.pref.CameraQuality
import com.example.docscanner.data.pref.ScannerPreferences
import com.example.docscanner.data.repository.DocumentRepository
import com.example.docscanner.service.FileStorageService
import com.example.docscanner.service.OcrService
import com.example.docscanner.service.PageData
import com.example.docscanner.service.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ScanViewModel(
    private val repository: DocumentRepository,
    private val ocrService: OcrService,
    private val fileStorageService: FileStorageService,
    private val pdfGenerator: PdfGenerator,
    private val preferences: ScannerPreferences,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private fun decodeBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (uri.scheme == "content") {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } else {
                uri.path?.let { BitmapFactory.decodeFile(it) }
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Called when ML Kit Document Scanner or Gallery Picker returns page URIs */
    fun onScanComplete(pageUris: List<Uri>, existingDocumentId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val settings = preferences.settings.value
            val imageQuality = when (settings.cameraQuality) {
                CameraQuality.UHD_4K -> 100
                CameraQuality.HIGH -> 92
                CameraQuality.STANDARD -> 80
            }

            _state.update { it.copy(isProcessing = true, processingStep = "Loading ${settings.cameraQuality.badge} images...") }

            try {
                val documentId = existingDocumentId ?: UUID.randomUUID().toString()
                val bitmaps = pageUris.mapNotNull { uri -> decodeBitmapFromUri(uri) }

                if (bitmaps.isEmpty()) {
                    _state.update { it.copy(isProcessing = false, error = "No pages found") }
                    return@launch
                }

                // Save first page as document thumbnail
                val thumbnailPath = fileStorageService.saveThumbnail(bitmaps.first(), documentId)

                // Save all page images with selected camera quality
                val imagePaths = bitmaps.mapIndexed { index, bitmap ->
                    fileStorageService.savePageImage(bitmap, documentId, index, quality = imageQuality)
                }

                // Run OCR on each page using selected OCR Language (if auto OCR is enabled)
                val ocrResults = if (settings.autoOcr) {
                    _state.update { it.copy(processingStep = "Running AI OCR on ${bitmaps.size} pages (${settings.ocrLanguage.displayName})...") }
                    bitmaps.map { bitmap -> ocrService.recognizeText(bitmap, settings.ocrLanguage) }
                } else {
                    emptyList()
                }

                val fullText = ocrResults.joinToString("\n\n--- Page Break ---\n\n") { it.fullText }

                // Smart auto-categorize based on OCR text
                val detectedCategory = CategoryClassifier.classify(fullText)

                // Generate Searchable PDF using configured PDF Quality profile (e.g. UHD / High / Standard)
                _state.update { it.copy(processingStep = "Generating ${settings.pdfQuality.badge} PDF...") }
                val pageDataList = bitmaps.mapIndexed { i, bitmap ->
                    PageData(bitmap = bitmap, extractedText = ocrResults.getOrNull(i)?.fullText ?: "")
                }
                val pdfBytes = pdfGenerator.generatePdf(
                    pages = pageDataList,
                    title = "Scanned Document",
                    quality = settings.pdfQuality
                )
                val pdfPath = fileStorageService.savePdf(pdfBytes, documentId)

                // Create or update document in SQLite
                val document = Document(
                    id = documentId,
                    title = generateTitle(ocrResults.firstOrNull()?.fullText),
                    category = detectedCategory,
                    pageCount = bitmaps.size,
                    thumbnailPath = thumbnailPath,
                    pdfPath = pdfPath,
                    extractedText = fullText
                )
                repository.saveDocument(document)

                // Save pages
                val pages = imagePaths.mapIndexed { index, path ->
                    Page(
                        id = UUID.randomUUID().toString(),
                        documentId = documentId,
                        pageIndex = index,
                        imagePath = path,
                        originalImagePath = path,
                        extractedText = ocrResults.getOrNull(index)?.fullText ?: ""
                    )
                }
                repository.savePages(pages)

                // Recycle bitmaps
                bitmaps.forEach { it.recycle() }

                _state.update {
                    it.copy(
                        isProcessing = false,
                        savedDocumentId = documentId,
                        ocrPreviewText = ocrResults.firstOrNull()?.fullText ?: ""
                    )
                }

            } catch (e: Exception) {
                _state.update {
                    it.copy(isProcessing = false, error = e.message ?: "Scan failed")
                }
            }
        }
    }

    private fun generateTitle(text: String?): String {
        if (text.isNullOrBlank()) return "Document ${System.currentTimeMillis() / 1000}"
        // Use first non-empty line as title, max 40 chars
        val firstLine = text.lines().firstOrNull { it.isNotBlank() } ?: "Document"
        return firstLine.take(40).trim()
    }

    fun clearError() = _state.update { it.copy(error = null) }
    fun setError(message: String) = _state.update { it.copy(error = message) }
    fun resetState() = _state.update { ScanState() }
}

data class ScanState(
    val isProcessing: Boolean = false,
    val processingStep: String = "",
    val savedDocumentId: String? = null,
    val ocrPreviewText: String = "",
    val error: String? = null
)
