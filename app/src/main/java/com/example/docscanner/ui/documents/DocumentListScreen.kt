package com.example.docscanner.ui.documents

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import com.example.docscanner.model.Document
import com.example.docscanner.model.DocumentCategory
import com.example.docscanner.model.Folder
import com.example.docscanner.model.SortOrder
import com.example.docscanner.data.pref.ScannerPreferences
import com.example.docscanner.service.BackupRestoreService
import com.example.docscanner.service.BiometricAuthManager
import com.example.docscanner.service.FileStorageService
import com.example.docscanner.ui.components.DocScannerBrandLogo
import com.example.docscanner.ui.components.UpdateAvailableDialog
import com.example.docscanner.ui.settings.SettingsDialog
import com.example.docscanner.ui.util.HapticHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DocumentListScreen(
    onNavigateToDocument: (String, String?) -> Unit,
    onStartScan: () -> Unit,
    onImportGallery: (List<Uri>) -> Unit = {},
    viewModel: DocumentListViewModel,
    preferences: ScannerPreferences,
    backupRestoreService: BackupRestoreService? = null,
    biometricAuthManager: BiometricAuthManager? = null,
    onProcessIdCard: ((Uri, Uri) -> Unit)? = null
) {
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    val isVaultMode by viewModel.isVaultMode.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val selectedDocIds by viewModel.selectedDocIds.collectAsStateWithLifecycle()
    val totalStorageBytes by viewModel.totalStorageBytes.collectAsStateWithLifecycle()
    val updateCheckState by viewModel.updateCheckState.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val isBannerDismissed by viewModel.isBannerDismissed.collectAsStateWithLifecycle()

    val isSelectionMode = selectedDocIds.isNotEmpty()
    val haptic = LocalHapticFeedback.current

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportGallery(uris)
        }
    }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showBatchCategoryDialog by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var showBatchMergeDialog by remember { mutableStateOf(false) }
    var showBatchExportDialog by remember { mutableStateOf(false) }
    var documentToDelete by remember { mutableStateOf<Document?>(null) }
    var documentToRename by remember { mutableStateOf<Document?>(null) }
    var showIdCardDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    LaunchedEffect(updateCheckState) {
        if (updateCheckState is UpdateCheckState.Available && showSettingsDialog) {
            showSettingsDialog = false
            showUpdateDialog = true
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedDocIds.size} selected",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (selectedDocIds.size == documents.size) {
                                    viewModel.clearSelection()
                                } else {
                                    viewModel.selectAll(documents.map { it.id })
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selectedDocIds.size == documents.size) Icons.Default.CheckCircle else Icons.Default.SelectAll,
                                contentDescription = "Select All"
                            )
                        }
                        IconButton(onClick = { viewModel.togglePinForSelected(pin = true) }) {
                            Icon(Icons.Default.PushPin, contentDescription = "Pin selected")
                        }
                        IconButton(onClick = { showBatchCategoryDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Change category")
                        }
                        IconButton(
                            onClick = { showBatchMergeDialog = true },
                            enabled = selectedDocIds.size >= 2
                        ) {
                            Icon(Icons.AutoMirrored.Filled.CallMerge, contentDescription = "Merge selected documents")
                        }
                        IconButton(onClick = { showBatchExportDialog = true }) {
                            Icon(Icons.Default.Archive, contentDescription = "Export selected as ZIP")
                        }
                        IconButton(onClick = { showBatchDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                LargeTopAppBar(
                    title = { Text("DocScanner") },
                    actions = {
                        // Sort Menu
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort documents"
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOrder.entries.forEach { order ->
                                    val isSelected = order == sortOrder
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = order.displayName,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        trailingIcon = {
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.setSortOrder(order)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Import from gallery
                        IconButton(onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Import from Gallery"
                            )
                        }

                        // View mode toggle
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = if (isGridView) "List view" else "Grid view"
                            )
                        }

                        // Settings
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (!isSelectionMode) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Import from Gallery", modifier = Modifier.size(20.dp))
                    }

                    SmallFloatingActionButton(
                        onClick = { showIdCardDialog = true },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Text("🪪", style = MaterialTheme.typography.titleMedium)
                    }

                    FloatingActionButton(
                        onClick = onStartScan,
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Scan document")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar (only when not in selection mode)
            if (!isSelectionMode) {
                DockedSearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = viewModel::setSearchQuery,
                            onSearch = {},
                            expanded = false,
                            onExpandedChange = {},
                            placeholder = { Text("Search documents...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }
                            }
                        )
                    },
                    expanded = false,
                    onExpandedChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {}
            }

            // In-App Update Alert Banner (when update available and not dismissed)
            val availableUpdate = (updateCheckState as? UpdateCheckState.Available)?.updateInfo
            if (availableUpdate != null && !isBannerDismissed) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { showUpdateDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DocScanner v${availableUpdate.latestVersion} Available!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Tap to view changelog & update",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.dismissUpdateBanner() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Folders & Vault Filter Bar
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            ) {
                // All Documents
                item {
                    FilterChip(
                        selected = selectedFolderId == null && !isVaultMode,
                        onClick = {
                            viewModel.selectFolder(null)
                            viewModel.setVaultMode(false)
                        },
                        label = { Text("📁 All") }
                    )
                }

                // Folders list
                items(folders, key = { it.id }) { folder ->
                    FilterChip(
                        selected = selectedFolderId == folder.id && !isVaultMode,
                        onClick = { viewModel.selectFolder(folder.id) },
                        label = { Text("📁 ${folder.name}") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete folder",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { folderToDelete = folder }
                            )
                        }
                    )
                }

                // New Folder Button
                item {
                    AssistChip(
                        onClick = { showCreateFolderDialog = true },
                        label = { Text("+ Folder") }
                    )
                }

                // Encrypted Vault Chip
                item {
                    FilterChip(
                        selected = isVaultMode,
                        onClick = {
                            if (isVaultMode) {
                                viewModel.setVaultMode(false)
                            } else {
                                val activity = context as? androidx.fragment.app.FragmentActivity
                                if (activity != null && biometricAuthManager != null) {
                                    biometricAuthManager.authenticate(
                                        activity = activity,
                                        title = "Encrypted Vault",
                                        subtitle = "Authenticate with Fingerprint, Face, or PIN to open vault"
                                    ) { result ->
                                        if (result.isSuccess) {
                                            viewModel.setVaultMode(true)
                                        } else {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Vault authentication failed")
                                            }
                                        }
                                    }
                                } else {
                                    viewModel.setVaultMode(true)
                                }
                            }
                        },
                        label = { Text(if (isVaultMode) "🔒 Vault (Unlocked)" else "🔒 Vault") }
                    )
                }
            }

            // Vault Active Alert Banner
            if (isVaultMode) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Encrypted Document Vault",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "AES-256 GCM authenticated encryption at rest",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        TextButton(onClick = { viewModel.setVaultMode(false) }) {
                            Text("Lock / Exit", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Category filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(DocumentCategory.entries.toTypedArray()) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { viewModel.setCategory(category) },
                        label = {
                            Text("${category.emoji} ${category.displayName}")
                        }
                    )
                }
            }

            // Document count & Storage Stats
            if (documents.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${documents.size} document${if (documents.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (totalStorageBytes > 0L) {
                        Text(
                            text = FileStorageService.formatFileSize(totalStorageBytes),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Empty state
            if (documents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        DocScannerBrandLogo(size = 96.dp)
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No results for \"$searchQuery\""
                                   else "DocScanner Offline AI",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "Tap + to scan your first document with AI edge detection"
                                   else "Try a different search keyword",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(documents, key = { it.id }) { doc ->
                        val isSelected = doc.id in selectedDocIds
                        DocumentGridCard(
                            document = doc,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            modifier = Modifier.animateItem(),
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleSelection(doc.id)
                                } else {
                                    onNavigateToDocument(doc.id, searchQuery)
                                }
                            },
                            onLongClick = {
                                viewModel.toggleSelection(doc.id)
                            },
                            onDelete = { documentToDelete = doc },
                            onRename = { documentToRename = doc },
                            onTogglePin = { viewModel.togglePin(doc) }
                        )
                    }
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(documents, key = { it.id }) { doc ->
                        val isSelected = doc.id in selectedDocIds
                        DocumentListCard(
                            document = doc,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            modifier = Modifier.animateItem(),
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleSelection(doc.id)
                                } else {
                                    onNavigateToDocument(doc.id, searchQuery)
                                }
                            },
                            onLongClick = {
                                viewModel.toggleSelection(doc.id)
                            },
                            onDelete = { documentToDelete = doc },
                            onRename = { documentToRename = doc },
                            onTogglePin = { viewModel.togglePin(doc) }
                        )
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            preferences = preferences,
            updateCheckState = updateCheckState,
            backupRestoreService = backupRestoreService,
            onCheckForUpdates = {
                viewModel.checkForUpdates(isManual = true)
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // In-App Update Dialog
    val currentUpdate = (updateCheckState as? UpdateCheckState.Available)?.updateInfo
    if (showUpdateDialog && currentUpdate != null) {
        UpdateAvailableDialog(
            updateInfo = currentUpdate,
            downloadProgress = downloadProgress,
            onDownloadAndInstall = {
                viewModel.downloadAndInstall(currentUpdate, context)
            },
            onDismiss = { showUpdateDialog = false }
        )
    }

    // Delete dialog
    documentToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { documentToDelete = null },
            title = { Text("Delete Document") },
            text = { Text("Delete \"${doc.title}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDocument(doc)
                        documentToDelete = null
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { documentToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // ── Batch Delete Dialog ───────────────────────────────────────────────────
    if (showBatchDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text("Delete ${selectedDocIds.size} Documents") },
            text = { Text("Are you sure you want to delete ${selectedDocIds.size} selected documents? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelectedDocuments(documents)
                        showBatchDeleteDialog = false
                    }
                ) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Batch Category Dialog ─────────────────────────────────────────────────
    if (showBatchCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showBatchCategoryDialog = false },
            title = { Text("Move ${selectedDocIds.size} Documents to Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DocumentCategory.entries.filter { it != DocumentCategory.ALL }.forEach { cat ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                viewModel.changeCategoryForSelected(cat)
                                showBatchCategoryDialog = false
                            },
                            label = { Text("${cat.emoji} ${cat.displayName}") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBatchCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Batch Merge Dialog ────────────────────────────────────────────────────
    if (showBatchMergeDialog) {
        AlertDialog(
            onDismissRequest = { showBatchMergeDialog = false },
            title = { Text("Merge ${selectedDocIds.size} Documents") },
            text = {
                Text(
                    "Combine ${selectedDocIds.size} selected documents into one new document? " +
                        "Pages will be joined in order of creation and the originals will be removed."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBatchMergeDialog = false
                        viewModel.mergeSelectedDocuments(documents) { newId ->
                            if (newId.isNotBlank()) {
                                onNavigateToDocument(newId, null)
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Merge failed. No changes were made.")
                                }
                            }
                        }
                    }
                ) { Text("Merge") }
            },
            dismissButton = {
                TextButton(onClick = { showBatchMergeDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Batch ZIP Export Dialog ───────────────────────────────────────────────
    if (showBatchExportDialog) {
        AlertDialog(
            onDismissRequest = { showBatchExportDialog = false },
            title = { Text("Export ${selectedDocIds.size} Documents") },
            text = { Text("Create a ZIP archive containing the PDF of each selected document to share or save?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBatchExportDialog = false
                        viewModel.exportSelectedAsZip(
                            documents = documents,
                            context = context,
                            onReady = { zipFile ->
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    zipFile
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share ZIP Archive"))
                            },
                            onError = { message ->
                                scope.launch {
                                    snackbarHostState.showSnackbar("Export failed: $message")
                                }
                            }
                        )
                    }
                ) { Text("Export") }
            },
            dismissButton = {
                TextButton(onClick = { showBatchExportDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Rename dialog
    documentToRename?.let { doc ->
        var newTitle by remember(doc.id) { mutableStateOf(doc.title) }
        AlertDialog(
            onDismissRequest = { documentToRename = null },
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            viewModel.renameDocument(doc.id, newTitle.trim())
                        }
                        documentToRename = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { documentToRename = null }) { Text("Cancel") }
            }
        )
    }

    // ── Dual-Sided ID Card Dialog ─────────────────────────────────────────────
    if (showIdCardDialog) {
        var frontUri by remember { mutableStateOf<Uri?>(null) }
        var backUri by remember { mutableStateOf<Uri?>(null) }
        var isPickingFront by remember { mutableStateOf(true) }

        val idCardPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                if (isPickingFront) {
                    frontUri = uri
                } else {
                    backUri = uri
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showIdCardDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🪪 ", style = MaterialTheme.typography.titleLarge)
                    Text("Dual-Sided ID Card Mode", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Select or snap photos of the front and back of your ID card. They will be symmetrically composited onto a single A4 page with rounded borders.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Front side card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isPickingFront = true
                                idCardPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (frontUri != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("1. FRONT SIDE", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Text(if (frontUri != null) "Photo selected ✓" else "Tap to choose front photo", style = MaterialTheme.typography.bodySmall)
                            }
                            if (frontUri != null) {
                                Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // Back side card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isPickingFront = false
                                idCardPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (backUri != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("2. BACK SIDE", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Text(if (backUri != null) "Photo selected ✓" else "Tap to choose back photo", style = MaterialTheme.typography.bodySmall)
                            }
                            if (backUri != null) {
                                Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = frontUri != null && backUri != null,
                    onClick = {
                        if (frontUri != null && backUri != null) {
                            showIdCardDialog = false
                            onProcessIdCard?.invoke(frontUri!!, backUri!!)
                        }
                    }
                ) {
                    Text("Create ID Card")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIdCardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Create Folder Dialog ──────────────────────────────────────────────────
    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create Folder") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name (e.g. Invoices, Personal)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            viewModel.createFolder(folderName.trim())
                            showCreateFolderDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Delete Folder Dialog ──────────────────────────────────────────────────
    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Delete Folder") },
            text = { Text("Delete folder \"${folder.name}\"? Documents inside will be moved to Root.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFolder(folder.id)
                        folderToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentGridCard(
    document: Document,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onTogglePin: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (isSelectionMode) onClick() else {
                        showMenu = true
                    }
                }
            ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
            ) {
                if (document.thumbnailPath.isNotBlank()) {
                    AsyncImage(
                        model = document.thumbnailPath,
                        contentDescription = document.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📄", style = MaterialTheme.typography.headlineLarge)
                    }
                }

                // Top row: Pin indicator on left, Page count or Checkbox on right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (document.isPinned) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text("📌", style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Spacer(modifier = Modifier.size(1.dp))
                    }

                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onClick() }
                        )
                    } else {
                        Badge {
                            Text("${document.pageCount}p")
                        }
                    }
                }

                // Category chip
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        document.category.emoji,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (document.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        document.tags.take(2).forEach { tag ->
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDate(document.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(if (document.isPinned) "Unpin Document" else "Pin to Top") },
                leadingIcon = { Icon(Icons.Default.PushPin, null) },
                onClick = { showMenu = false; onTogglePin() }
            )
            DropdownMenuItem(
                text = { Text("Select") },
                leadingIcon = { Icon(Icons.Default.CheckCircle, null) },
                onClick = { showMenu = false; onLongClick() }
            )
            DropdownMenuItem(
                text = { Text("Rename") },
                leadingIcon = { Icon(Icons.Default.Edit, null) },
                onClick = { showMenu = false; onRename() }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                leadingIcon = { Icon(Icons.Default.Delete, null) },
                onClick = { showMenu = false; onDelete() }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentListCard(
    document: Document,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onTogglePin: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (isSelectionMode) onClick() else {
                        showMenu = true
                    }
                }
            ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (document.thumbnailPath.isNotBlank()) {
                    AsyncImage(
                        model = document.thumbnailPath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("📄", style = MaterialTheme.typography.titleLarge)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (document.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📌", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${document.category.emoji} ${document.category.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("·", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = "${document.pageCount} page${if (document.pageCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                if (document.tags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        document.tags.take(3).forEach { tag ->
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1
                            )
                        }
                    }
                }
                Text(
                    text = formatDate(document.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text(if (document.isPinned) "Unpin Document" else "Pin to Top") },
                    leadingIcon = { Icon(Icons.Default.PushPin, null) },
                    onClick = { showMenu = false; onTogglePin() }
                )
                DropdownMenuItem(
                    text = { Text("Select") },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, null) },
                    onClick = { showMenu = false; onLongClick() }
                )
                DropdownMenuItem(
                    text = { Text("Rename") },
                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                    onClick = { showMenu = false; onRename() }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                    onClick = { showMenu = false; onDelete() }
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
