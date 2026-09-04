package com.example.docscanner.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.docscanner.data.model.CategoryClassifier
import com.example.docscanner.data.model.Document
import com.example.docscanner.data.model.DocumentCategory
import com.example.docscanner.data.model.Page
import com.example.docscanner.data.pref.CameraQuality
import com.example.docscanner.data.pref.ScannerPreferences
import com.example.docscanner.data.repository.DocumentRepository
import com.example.docscanner.service.FileStorageService
import com.example.docscanner.service.IdCardCompositorService
import com.example.docscanner.service.OcrService
import com.example.docscanner.service.PageData
import com.example.docscanner.service.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
                val existingDoc = if (existingDocumentId != null) repository.getDocumentById(existingDocumentId) else null
                val existingPages = if (existingDocumentId != null) repository.getPagesForDocumentSync(existingDocumentId) else emptyList()
                val startPageIndex = existingPages.size

                val documentId = existingDocumentId ?: UUID.randomUUID().toString()

                val bitmaps: List<Bitmap> = pageUris.mapNotNull { decodeBitmapFromUri(it) }
                if (bitmaps.isEmpty()) {
                    _state.update { it.copy(isProcessing = false, error = "Failed to load scanned page images") }
                    return@launch
                }

                // Save or preserve thumbnail
                val thumbnailPath = if (existingDoc?.thumbnailPath.isNullOrBlank()) {
                    fileStorageService.saveThumbnail(bitmaps.first(), documentId)
                } else {
                    existingDoc.thumbnailPath
                }

                // Save new page images with selected camera quality, offset by startPageIndex
                val newImagePaths = bitmaps.mapIndexed { index, bitmap ->
                    fileStorageService.savePageImage(bitmap, documentId, startPageIndex + index, quality = imageQuality)
                }

                // Run OCR on new pages using selected OCR Language (if auto OCR is enabled)
                val newOcrResults = if (settings.autoOcr) {
                    _state.update { it.copy(processingStep = "Running AI OCR on ${bitmaps.size} pages (${settings.ocrLanguage.displayName})...") }
                    bitmaps.map { bitmap -> ocrService.recognizeText(bitmap, settings.ocrLanguage) }
                } else {
                    emptyList()
                }

                val newFullText = newOcrResults.joinToString("\n\n--- Page Break ---\n\n") { it.fullText }
                val combinedText = if (existingDoc != null && existingDoc.extractedText.isNotBlank()) {
                    if (newFullText.isNotBlank()) "${existingDoc.extractedText}\n\n--- Page Break ---\n\n$newFullText" else existingDoc.extractedText
                } else {
                    newFullText
                }

                // Build complete PageData list for all pages (existing + new) to regenerate searchable PDF
                _state.update { it.copy(processingStep = "Generating ${settings.pdfQuality.badge} PDF...") }
                val allPageData = mutableListOf<PageData>()
                for (page in existingPages) {
                    val pageBitmap = fileStorageService.loadBitmap(page.imagePath)
                    if (pageBitmap != null) {
                        allPageData.add(PageData(bitmap = pageBitmap, extractedText = page.extractedText))
                    }
                }
                bitmaps.forEachIndexed { i, bitmap ->
                    allPageData.add(PageData(bitmap = bitmap, extractedText = newOcrResults.getOrNull(i)?.fullText ?: ""))
                }

                val pdfBytes = pdfGenerator.generatePdf(
                    pages = allPageData,
                    title = existingDoc?.title ?: "Scanned Document",
                    quality = settings.pdfQuality
                )
                val pdfPath = fileStorageService.savePdf(pdfBytes, documentId)

                // Clean up any loaded intermediate bitmaps for existing pages
                for (i in 0 until existingPages.size) {
                    allPageData.getOrNull(i)?.bitmap?.recycle()
                }

                // Save or update document in SQLite
                val document = if (existingDoc != null) {
                    existingDoc.copy(
                        pageCount = existingPages.size + bitmaps.size,
                        thumbnailPath = thumbnailPath,
                        pdfPath = pdfPath,
                        extractedText = combinedText,
                        modifiedAt = System.currentTimeMillis()
                    )
                } else {
                    val detectedCategory = CategoryClassifier.classify(combinedText)
                    Document(
                        id = documentId,
                        title = generateTitle(newOcrResults.firstOrNull()?.fullText),
                        category = detectedCategory,
                        createdAt = System.currentTimeMillis(),
                        modifiedAt = System.currentTimeMillis(),
                        pageCount = bitmaps.size,
                        thumbnailPath = thumbnailPath,
                        pdfPath = pdfPath,
                        extractedText = combinedText
                    )
                }
                repository.saveDocument(document)

                // Save new pages
                val newPages = newImagePaths.mapIndexed { index, path ->
                    Page(
                        id = UUID.randomUUID().toString(),
                        documentId = documentId,
                        pageIndex = startPageIndex + index,
                        imagePath = path,
                        originalImagePath = path,
                        extractedText = newOcrResults.getOrNull(index)?.fullText ?: "",
                        createdAt = System.currentTimeMillis()
                    )
                }
                repository.savePages(newPages)

                // Recycle new capture bitmaps
                bitmaps.forEach { it.recycle() }

                _state.update {
                    it.copy(
                        isProcessing = false,
                        savedDocumentId = documentId,
                        ocrPreviewText = newOcrResults.firstOrNull()?.fullText ?: ""
                    )
                }

            } catch (e: Exception) {
                _state.update {
                    it.copy(isProcessing = false, error = e.message ?: "Scan failed")
                }
            }
        }
    }

    /**
     * Composites dual-sided ID card images (front & back) symmetrically onto an A4 page
     * and saves as an ID Card document.
     */
    fun processIdCard(frontUri: Uri, backUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val settings = preferences.settings.value
            val imageQuality = when (settings.cameraQuality) {
                CameraQuality.UHD_4K -> 100
                CameraQuality.HIGH -> 92
                CameraQuality.STANDARD -> 80
            }

            _state.update { it.copy(isProcessing = true, processingStep = "Compositing ID Card (Front + Back)...") }

            try {
                val frontBitmap = decodeBitmapFromUri(frontUri)
                val backBitmap = decodeBitmapFromUri(backUri)
                if (frontBitmap == null || backBitmap == null) {
                    _state.update { it.copy(isProcessing = false, error = "Failed to load ID card images") }
                    return@launch
                }

                val compositeBitmap = IdCardCompositorService.compositeIdCard(frontBitmap, backBitmap)
                frontBitmap.recycle()
                backBitmap.recycle()

                val documentId = UUID.randomUUID().toString()
                val thumbnailPath = fileStorageService.saveThumbnail(compositeBitmap, documentId)
                val pagePath = fileStorageService.savePageImage(compositeBitmap, documentId, 0, quality = imageQuality)

                val ocrResult = if (settings.autoOcr) {
                    _state.update { it.copy(processingStep = "Running AI OCR on ID card...") }
                    ocrService.recognizeText(compositeBitmap, settings.ocrLanguage)
                } else null

                val extractedText = ocrResult?.fullText ?: ""

                _state.update { it.copy(processingStep = "Generating PDF...") }
                val pageData = listOf(PageData(compositeBitmap, extractedText))
                val pdfBytes = pdfGenerator.generatePdf(pageData, "ID Card", settings.pdfQuality)
                val pdfPath = fileStorageService.savePdf(pdfBytes, documentId)

                val doc = Document(
                    id = documentId,
                    title = "ID Card ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())}",
                    category = DocumentCategory.ID_CARD,
                    createdAt = System.currentTimeMillis(),
                    modifiedAt = System.currentTimeMillis(),
                    pageCount = 1,
                    thumbnailPath = thumbnailPath,
                    pdfPath = pdfPath,
                    extractedText = extractedText
                )
                repository.saveDocument(doc)

                val page = Page(
                    id = UUID.randomUUID().toString(),
                    documentId = documentId,
                    pageIndex = 0,
                    imagePath = pagePath,
                    originalImagePath = pagePath,
                    extractedText = extractedText,
                    createdAt = System.currentTimeMillis()
                )
                repository.savePages(listOf(page))

                compositeBitmap.recycle()

                _state.update {
                    it.copy(
                        isProcessing = false,
                        savedDocumentId = documentId,
                        ocrPreviewText = extractedText
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isProcessing = false, error = e.message ?: "ID Card processing failed") }
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
