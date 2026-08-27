package com.example.docscanner.ui.documents

import android.content.Intent
import android.graphics.BitmapFactory
import android.print.PrintManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.docscanner.model.DocumentCategory
import com.example.docscanner.model.DocumentMetricsCalculator
import com.example.docscanner.pref.PdfQuality
import com.example.docscanner.service.FileStorageService
import com.example.docscanner.service.PdfPrintDocumentAdapter
import com.example.docscanner.ui.components.ZoomableImageDialog
import com.example.docscanner.ui.util.HapticHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private fun printDocument(context: android.content.Context, docTitle: String, pdfPath: String) {
    val file = File(pdfPath)
    if (file.exists() && file.length() > 0) {
        val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as? PrintManager
        if (printManager != null) {
            val printAdapter = PdfPrintDocumentAdapter(context, file, docTitle)
            printManager.print("DocScanner_$docTitle", printAdapter, android.print.PrintAttributes.Builder().build())
        } else {
            Toast.makeText(context, "Printing service unavailable on this device", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "PDF file is not available for printing", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DocumentDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: DocumentDetailViewModel,
    searchQuery: String = "",
    autoRename: Boolean = false,
    onOpenDocument: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeletePageDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showPdfQualityDialog by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var showSplitDialog by remember { mutableStateOf(false) }
    var zoomPagePath by remember { mutableStateOf<String?>(null) }
    var selectedPdfQuality by remember { mutableStateOf(PdfQuality.UHD_4K) }
    var selectedPageIndex by remember { mutableIntStateOf(0) }

    var isCopiedPage by remember { mutableStateOf(false) }
    var isCopiedAll by remember { mutableStateOf(false) }

    LaunchedEffect(isCopiedPage) {
        if (isCopiedPage) {
            kotlinx.coroutines.delay(2000)
            isCopiedPage = false
        }
    }

    LaunchedEffect(isCopiedAll) {
        if (isCopiedAll) {
            kotlinx.coroutines.delay(2000)
            isCopiedAll = false
        }
    }

    // Automatically surface the rename dialog right after a fresh scan/import.
    LaunchedEffect(Unit) {
        if (autoRename) showRenameDialog = true
    }

    val doc = state.document
    val pages = state.pages

    // Ensure selectedPageIndex stays within bounds
    if (pages.isNotEmpty() && selectedPageIndex >= pages.size) {
        selectedPageIndex = pages.size - 1
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = doc?.title ?: "Document",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (doc?.isPinned == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📌", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        HapticHelper.confirm(haptic)
                        viewModel.togglePin()
                    }) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = if (doc?.isPinned == true) "Unpin" else "Pin",
                            tint = if (doc?.isPinned == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "More actions")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (doc?.isPinned == true) "Unpin Document" else "Pin to Top") },
                            leadingIcon = { Icon(Icons.Default.PushPin, null) },
                            onClick = {
                                showMenu = false
                                HapticHelper.confirm(haptic)
                                viewModel.togglePin()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Print Document (Wi-Fi)") },
                            leadingIcon = { Icon(Icons.Default.Print, null) },
                            onClick = {
                                showMenu = false
                                HapticHelper.click(haptic)
                                doc?.pdfPath?.let { path ->
                                    printDocument(context, doc.title, path)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export UHD PDF (600 DPI)") },
                            leadingIcon = { Icon(Icons.Default.HighQuality, null) },
                            onClick = {
                                showMenu = false
                                viewModel.exportPdfWithQuality(PdfQuality.UHD_4K) { file ->
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share UHD PDF"))
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { showMenu = false; showRenameDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Change Category") },
                            leadingIcon = { Text("🏷️") },
                            onClick = { showMenu = false; showCategoryDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Retry OCR (this page)") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                            enabled = pages.getOrNull(selectedPageIndex) != null && !state.isOcrRunning,
                            onClick = {
                                showMenu = false
                                pages.getOrNull(selectedPageIndex)?.let { viewModel.rerunOcrOnPage(it) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Retry OCR (all pages)") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                            enabled = pages.isNotEmpty() && !state.isOcrRunning,
                            onClick = {
                                showMenu = false
                                viewModel.rerunOcrAll()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Split document at page ${selectedPageIndex + 1}") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.RotateRight, null) },
                            enabled = pages.size >= 2 && selectedPageIndex >= 1,
                            onClick = {
                                showMenu = false
                                showSplitDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Document") },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { showMenu = false; showDeleteDialog = true }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Share / Export PDF with Quality Choice
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { showPdfQualityDialog = true }
                    ) {
                        Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Export PDF")
                    }

                    // Share Image
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            pages.getOrNull(selectedPageIndex)?.imagePath?.let { path ->
                                val file = File(path)
                                if (file.exists()) {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/jpeg"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Image"))
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Share Page")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Page Image Carousel / Main Preview
                if (pages.isNotEmpty()) {
                    val currentPage = pages.getOrElse(selectedPageIndex) { pages.first() }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .clickable { zoomPagePath = currentPage.imagePath },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = currentPage.imagePath,
                                contentDescription = "Page ${selectedPageIndex + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )

                            // Pinch to zoom helper badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                        CircleShape
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = "Pinch to zoom",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Pinch to Zoom",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Page indicator badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                        CircleShape
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${selectedPageIndex + 1} / ${pages.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Multi-page thumbnail strip
                    if (pages.size > 1) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            itemsIndexed(pages) { index, page ->
                                val isSelected = index == selectedPageIndex
                                Card(
                                    modifier = Modifier
                                        .size(60.dp, 80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedPageIndex = index },
                                    border = if (isSelected) {
                                        androidx.compose.foundation.BorderStroke(
                                            2.dp,
                                            MaterialTheme.colorScheme.primary
                                        )
                                    } else null
                                ) {
                                    AsyncImage(
                                        model = page.imagePath,
                                        contentDescription = "Page thumbnail ${index + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    // Page action buttons (Rotate & Delete page)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                pages.getOrNull(selectedPageIndex)?.let { page ->
                                    HapticHelper.click(haptic)
                                    viewModel.rotatePage(page, 90f)
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rotate 90°")
                        }

                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                HapticHelper.heavy(haptic)
                                showDeletePageDialog = true
                            },
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (pages.size <= 1) "Delete Doc" else "Delete Page")
                        }
                    }
                }

                // Document Metadata Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Document Info",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            doc?.let {
                                Text(
                                    text = "${it.category.emoji} ${it.category.displayName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        doc?.let {
                            MetadataRow("Pages", "${it.pageCount}")
                            MetadataRow("File Size", FileStorageService.formatFileSize(state.storageBytes))
                            MetadataRow("Pinned", if (it.isPinned) "Yes 📌" else "No")
                            MetadataRow("Created", formatDate(it.createdAt))
                            MetadataRow("Modified", formatDate(it.modifiedAt))
                        }
                    }
                }

                // Page Dimensions, Format & DPI Diagnostics Card
                val currentPage = pages.getOrNull(selectedPageIndex)
                if (currentPage != null) {
                    val (imgWidth, imgHeight) = remember(currentPage.imagePath) {
                        try {
                            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeFile(currentPage.imagePath, options)
                            Pair(options.outWidth, options.outHeight)
                        } catch (_: Exception) { Pair(0, 0) }
                    }
                    val pageMetrics = remember(imgWidth, imgHeight) {
                        DocumentMetricsCalculator.calculate(imgWidth, imgHeight)
                    }
                    val pageFileSize = remember(currentPage.imagePath) {
                        val f = File(currentPage.imagePath)
                        if (f.exists()) {
                            val mb = f.length() / (1024.0 * 1024.0)
                            "${((mb * 10).roundToInt()) / 10.0} MB"
                        } else ""
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Page ${selectedPageIndex + 1} Specs & Dimensions",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                AssistChip(
                                    onClick = {},
                                    label = { Text(pageMetrics.format.badge, fontWeight = FontWeight.Bold) }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            MetadataRow("Paper Standard", "${pageMetrics.format.displayName} (${pageMetrics.format.physicalDimensions})")
                            MetadataRow("Resolution", "${pageMetrics.resolutionString} • ${pageMetrics.megapixelsString}")
                            MetadataRow("Scan Fidelity", "${pageMetrics.dpiBadgeString} High-Fidelity")
                            if (pageFileSize.isNotBlank()) {
                                MetadataRow("Page Size", pageFileSize)
                            }
                        }
                    }
                }

                // Document Tags & Labels Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tags & Labels",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            AssistChip(
                                onClick = { showAddTagDialog = true },
                                label = { Text("Add Tag") },
                                leadingIcon = {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }

                        val tags = doc?.tags ?: emptyList()
                        if (tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                tags.forEach { tag ->
                                    InputChip(
                                        selected = false,
                                        onClick = {},
                                        label = { Text("#$tag") },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove tag",
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable { viewModel.removeTag(tag) }
                                            )
                                        }
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No tags added yet. Tap 'Add Tag' to organize.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // OCR Extracted Text Card
                val currentExtractedText = pages.getOrNull(selectedPageIndex)?.extractedText
                    ?: doc?.extractedText ?: ""
                val allPagesExtractedText = remember(pages) {
                    pages.mapIndexed { idx, p -> "--- Page ${idx + 1} ---\n${p.extractedText}" }
                        .filter { it.isNotBlank() }
                        .joinToString("\n\n")
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Extracted Text (Page ${selectedPageIndex + 1})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (currentExtractedText.isNotBlank() || allPagesExtractedText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        HapticHelper.confirm(haptic)
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Page Text", currentExtractedText)
                                        clipboard.setPrimaryClip(clip)
                                        isCopiedPage = true
                                        Toast.makeText(context, "Page text copied", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isCopiedPage) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isCopiedPage) "Copied!" else "Copy Page")
                                }

                                if (pages.size > 1) {
                                    OutlinedButton(
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            HapticHelper.confirm(haptic)
                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("All Pages Text", allPagesExtractedText)
                                            clipboard.setPrimaryClip(clip)
                                            isCopiedAll = true
                                            Toast.makeText(context, "All ${pages.size} pages text copied", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isCopiedAll) Icons.Default.Check else Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isCopiedAll) "Copied All!" else "Copy All")
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        HapticHelper.click(haptic)
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, doc?.title ?: "Scanned Text")
                                            putExtra(Intent.EXTRA_TEXT, if (pages.size > 1) allPagesExtractedText else currentExtractedText)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Extracted Text"))
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share text",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        if (currentExtractedText.isNotBlank()) {
                            SelectionContainer {
                                Text(
                                    text = if (searchQuery.isNotBlank()) {
                                        highlightSearchMatches(currentExtractedText, searchQuery)
                                    } else {
                                        AnnotatedString(currentExtractedText)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            Text(
                                text = "No text detected on this page.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            // OCR re-run progress overlay
            AnimatedVisibility(
                visible = state.isOcrRunning,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(32.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = state.ocrProgressText.ifEmpty { "Running OCR..." },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Exporting progress overlay
            AnimatedVisibility(
                visible = state.isExporting,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(32.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = state.exportProgressText.ifEmpty { "Generating PDF..." },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    // ── PDF Quality Selector Dialog ───────────────────────────────────────────
    if (showPdfQualityDialog) {
        AlertDialog(
            onDismissRequest = { showPdfQualityDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select PDF Quality", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PdfQuality.entries.forEach { quality ->
                        val isSelected = quality == selectedPdfQuality
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPdfQuality = quality },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedPdfQuality = quality }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = quality.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Badge(
                                            containerColor = if (quality == PdfQuality.UHD_4K) {
                                                MaterialTheme.colorScheme.tertiary
                                            } else {
                                                MaterialTheme.colorScheme.secondary
                                            }
                                        ) {
                                            Text(quality.badge)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = quality.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPdfQualityDialog = false
                        viewModel.exportPdfWithQuality(selectedPdfQuality) { file ->
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share ${selectedPdfQuality.badge} PDF"))
                        }
                    }
                ) {
                    Text("Generate & Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPdfQualityDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Delete Document Dialog ───────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Document") },
            text = { Text("Delete \"${doc?.title}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteDocument(onComplete = onNavigateBack)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Delete Page Dialog ───────────────────────────────────────────────────
    if (showDeletePageDialog) {
        val isLastPage = pages.size <= 1
        AlertDialog(
            onDismissRequest = { showDeletePageDialog = false },
            title = { Text(if (isLastPage) "Delete Entire Document?" else "Delete Page ${selectedPageIndex + 1}?") },
            text = {
                Text(
                    if (isLastPage)
                        "This is the only page in \"${doc?.title}\". Deleting this page will delete the entire document."
                    else
                        "Are you sure you want to delete page ${selectedPageIndex + 1} of ${pages.size}?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeletePageDialog = false
                        pages.getOrNull(selectedPageIndex)?.let { page ->
                            viewModel.deletePage(page, onDocumentDeleted = onNavigateBack)
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePageDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Split Document Dialog ─────────────────────────────────────────────────
    if (showSplitDialog) {
        AlertDialog(
            onDismissRequest = { showSplitDialog = false },
            title = { Text("Split Document") },
            text = {
                Text(
                    "Split this document into two at page ${selectedPageIndex + 1}? " +
                        "Pages 1–$selectedPageIndex stay in this document, pages " +
                        "${selectedPageIndex + 1}–${pages.size} move to a new document."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSplitDialog = false
                        viewModel.splitDocument(selectedPageIndex) { newId ->
                            if (newId != null) {
                                onOpenDocument(newId)
                            } else {
                                Toast.makeText(context, "Split failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) { Text("Split") }
            },
            dismissButton = {
                TextButton(onClick = { showSplitDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Rename Dialog ─────────────────────────────────────────────────────────
    if (showRenameDialog && doc != null) {
        var newTitle by remember { mutableStateOf(doc.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            viewModel.renameDocument(newTitle.trim())
                            showRenameDialog = false
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Category Dialog ───────────────────────────────────────────────────────
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Select Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DocumentCategory.entries.filter { it != DocumentCategory.ALL }.forEach { cat ->
                        FilterChip(
                            selected = doc?.category == cat,
                            onClick = {
                                viewModel.updateCategory(cat)
                                showCategoryDialog = false
                            },
                            label = { Text("${cat.emoji} ${cat.displayName}") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Add Tag Dialog ────────────────────────────────────────────────────────
    if (showAddTagDialog) {
        var tagInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = { Text("Add Tag / Label") },
            text = {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = { Text("Tag Name (e.g. tax-2024, urgent)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tagInput.isNotBlank()) {
                            viewModel.addTag(tagInput)
                            showAddTagDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Fullscreen Zoomable Image Lightbox ────────────────────────────────────
    if (zoomPagePath != null) {
        ZoomableImageDialog(
            imagePath = zoomPagePath!!,
            title = doc?.title ?: "Page Preview",
            pageIndex = selectedPageIndex,
            pageCount = pages.size,
            onDismiss = { zoomPagePath = null }
        )
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Highlights every case-insensitive match of [query] tokens within [text]
 * using the theme's primary container color.
 */
@Composable
private fun highlightSearchMatches(text: String, query: String): AnnotatedString {
    val tokens = query.split(Regex("\\s+")).filter { it.length >= 2 }
    val backgroundColor = MaterialTheme.colorScheme.primaryContainer
    val highlightColor = MaterialTheme.colorScheme.onPrimaryContainer

    if (tokens.isEmpty()) return AnnotatedString(text)

    val lowerText = text.lowercase(Locale.getDefault())
    val matches = mutableListOf<IntRange>()
    tokens.forEach { token ->
        val lowerToken = token.lowercase(Locale.getDefault())
        var index = lowerText.indexOf(lowerToken)
        while (index >= 0) {
            matches += index until (index + token.length)
            index = lowerText.indexOf(lowerToken, index + token.length)
        }
    }

    if (matches.isEmpty()) return AnnotatedString(text)

    return buildAnnotatedString {
        var cursor = 0
        matches.sortedBy { it.first }.forEach { range ->
            if (range.first >= cursor) {
                append(text.substring(cursor, range.first))
                withStyle(SpanStyle(background = backgroundColor, color = highlightColor, fontWeight = FontWeight.Bold)) {
                    append(text.substring(range.first, range.last + 1))
                }
                cursor = range.last + 1
            }
        }
        if (cursor < text.length) append(text.substring(cursor))
    }
}
