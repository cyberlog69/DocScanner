package com.example.docscanner.service

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.docscanner.data.model.Document
import com.example.docscanner.data.model.Page
import com.example.docscanner.data.repository.DocumentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Owns the vault lifecycle.
 *
 * Vault documents are stored under `files/vault/<documentId>/`, outside the library
 * folder, so the system can exclude them from cloud backup and device transfer.
 * Files are ciphertext at rest and are decrypted only while the vault is open:
 * [unlock] before showing vault contents, [lock] whenever the vault closes or the
 * app goes to the background.
 */
class VaultSessionService(
    private val repository: DocumentRepository,
    private val fileStorageService: FileStorageService
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                scope.launch { lock() }
            }
        })
        // Re-encrypt anything left plaintext if the process died while the vault was open.
        scope.launch { lock() }
    }

    /**
     * Decrypts every vault document and returns true when all files were readable.
     */
    suspend fun unlock(): Boolean = mutex.withLock {
        var allOk = true
        val vaultDocuments = repository.getVaultDocumentsSync()
        for (doc in vaultDocuments) {
            ensureVaultLayout(doc)
            forEachVaultFile(doc) { file ->
                if (VaultEncryptionService.isEncrypted(file) &&
                    !VaultEncryptionService.decryptFileInPlace(file)
                ) {
                    allOk = false
                }
            }
        }
        _isUnlocked.value = true
        allOk
    }

    /**
     * Re-encrypts every vault document. Safe to call repeatedly and after a partial
     * [unlock] failure.
     */
    suspend fun lock(): Unit = mutex.withLock {
        for (doc in repository.getVaultDocumentsSync()) {
            forEachVaultFile(doc) { file ->
                if (!VaultEncryptionService.isEncrypted(file) &&
                    !VaultEncryptionService.encryptFile(file)
                ) {
                    Log.e("VaultSessionService", "Failed to re-encrypt ${file.name}")
                }
            }
        }
        _isUnlocked.value = false
    }

    /**
     * Moves a library document into the vault, encrypting its files.
     * Returns false and leaves the document in the library when anything fails.
     */
    suspend fun moveIntoVault(document: Document, pages: List<Page>): Boolean = mutex.withLock {
        val vaultDir = fileStorageService.getVaultDocumentDir(document.id)
        val movedPaths = mutableMapOf<String, String>()

        try {
            for (file in documentFiles(document, pages)) {
                if (!file.exists()) continue
                val destination = uniqueTarget(vaultDir, file)
                if (destination.absolutePath == file.absolutePath) continue
                if (!file.renameTo(destination)) {
                    throw IllegalStateException("Could not move ${file.name} into the vault")
                }
                movedPaths[file.absolutePath] = destination.absolutePath
            }

            for (path in movedPaths.values) {
                val file = File(path)
                if (!VaultEncryptionService.encryptFile(file)) {
                    throw IllegalStateException("Could not encrypt ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e("VaultSessionService", "Vault move failed, rolling back", e)
            rollbackMove(movedPaths)
            return@withLock false
        }

        val updatedPages = pages.map { page ->
            page.copy(
                imagePath = movedPaths[page.imagePath] ?: page.imagePath,
                originalImagePath = movedPaths[page.originalImagePath] ?: page.originalImagePath
            )
        }
        repository.savePages(updatedPages)
        repository.updateDocument(
            document.copy(
                thumbnailPath = movedPaths[document.thumbnailPath] ?: document.thumbnailPath,
                pdfPath = movedPaths[document.pdfPath] ?: document.pdfPath,
                isVault = true
            )
        )
        repository.setVaultStatus(document.id, true)
        true
    }

    /**
     * Moves a vault document back into the library, decrypting its files.
     */
    suspend fun moveOutOfVault(document: Document, pages: List<Page>): Boolean = mutex.withLock {
        for (file in documentFiles(document, pages)) {
            if (!VaultEncryptionService.decryptFileIfNeeded(file)) {
                Log.e("VaultSessionService", "Could not decrypt ${file.name}")
                return@withLock false
            }
        }

        val documentDir = fileStorageService.getDocumentDir(document.id)
        val thumbnailsDir = fileStorageService.thumbnailsDirectory()
        val movedPaths = mutableMapOf<String, String>()

        try {
            for (file in documentFiles(document, pages)) {
                if (!file.exists()) continue
                val isThumbnail = file.absolutePath == document.thumbnailPath
                val destination = if (isThumbnail) {
                    File(thumbnailsDir, file.name)
                } else {
                    uniqueTarget(documentDir, file)
                }
                if (destination.absolutePath == file.absolutePath) continue
                if (!file.renameTo(destination)) {
                    throw IllegalStateException("Could not move ${file.name} out of the vault")
                }
                movedPaths[file.absolutePath] = destination.absolutePath
            }
        } catch (e: Exception) {
            Log.e("VaultSessionService", "Vault restore failed, rolling back", e)
            for ((original, moved) in movedPaths) {
                val file = File(moved)
                if (file.exists()) file.renameTo(File(original))
            }
            forEachVaultFile(document) { file ->
                VaultEncryptionService.encryptFileIfNeeded(file)
            }
            return@withLock false
        }

        val updatedPages = pages.map { page ->
            page.copy(
                imagePath = movedPaths[page.imagePath] ?: page.imagePath,
                originalImagePath = movedPaths[page.originalImagePath] ?: page.originalImagePath
            )
        }
        repository.savePages(updatedPages)
        repository.updateDocument(
            document.copy(
                thumbnailPath = movedPaths[document.thumbnailPath] ?: document.thumbnailPath,
                pdfPath = movedPaths[document.pdfPath] ?: document.pdfPath,
                isVault = false
            )
        )
        repository.setVaultStatus(document.id, false)
        true
    }

    /**
     * Moves legacy vault files from `documents/` into `vault/` without touching their
     * encryption state. Existing installs created vault documents before the split.
     */
    private suspend fun ensureVaultLayout(document: Document) {
        val vaultDir = fileStorageService.getVaultDocumentDir(document.id)
        val pages = repository.getPagesForDocumentSync(document.id)

        fun relocate(path: String): String {
            if (path.isBlank()) return path
            val file = File(path)
            if (!file.exists() || file.parentFile == vaultDir) return path
            val destination = uniqueTarget(vaultDir, file)
            if (!file.renameTo(destination)) {
                Log.e("VaultSessionService", "Could not relocate ${file.name} into the vault")
                return path
            }
            return destination.absolutePath
        }

        val relocatedPages = pages.map { page ->
            page.copy(
                imagePath = relocate(page.imagePath),
                originalImagePath = relocate(page.originalImagePath)
            )
        }
        if (relocatedPages != pages) repository.savePages(relocatedPages)

        val relocatedDocument = document.copy(
            thumbnailPath = relocate(document.thumbnailPath),
            pdfPath = relocate(document.pdfPath)
        )
        if (relocatedDocument.thumbnailPath != document.thumbnailPath ||
            relocatedDocument.pdfPath != document.pdfPath
        ) {
            repository.updateDocument(relocatedDocument)
        }
    }

    private fun documentFiles(document: Document, pages: List<Page>): List<File> = buildList {
        for (page in pages) {
            File(page.imagePath).takeIf { it.path.isNotBlank() }?.let { add(it) }
            File(page.originalImagePath).takeIf {
                it.path.isNotBlank() && it.absolutePath != File(page.imagePath).absolutePath
            }?.let { add(it) }
        }
        File(document.pdfPath).takeIf { it.path.isNotBlank() }?.let { add(it) }
        File(document.thumbnailPath).takeIf { it.path.isNotBlank() }?.let { add(it) }
    }.distinctBy { it.absolutePath }

    private inline fun forEachVaultFile(document: Document, action: (File) -> Unit) {
        val dir = fileStorageService.getVaultDocumentDir(document.id)
        dir.listFiles()?.forEach { file ->
            if (file.isFile) action(file)
        }
    }

    private fun uniqueTarget(directory: File, source: File): File {
        val candidate = File(directory, source.name)
        if (!candidate.exists() || candidate.absolutePath == source.absolutePath) return candidate
        var index = 1
        val baseName = source.nameWithoutExtension
        val extension = source.extension
        while (true) {
            val next = File(directory, "${baseName}_$index${if (extension.isNotEmpty()) ".$extension" else ""}")
            if (!next.exists()) return next
            index++
        }
    }

    private fun rollbackMove(movedPaths: Map<String, String>) {
        for ((original, moved) in movedPaths) {
            val file = File(moved)
            if (!file.exists()) continue
            VaultEncryptionService.decryptFileIfNeeded(file)
            file.renameTo(File(original))
        }
    }
}
