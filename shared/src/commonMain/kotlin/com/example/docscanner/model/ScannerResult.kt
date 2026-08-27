package com.example.docscanner.model

/**
 * Sealed result hierarchy for type-safe cross-platform operations in DocScanner.
 * Encapsulates success values and typed domain failures.
 */
sealed interface ScannerResult<out T> {

    data class Success<out T>(val data: T) : ScannerResult<T>

    sealed interface Failure : ScannerResult<Nothing> {
        val message: String
        val cause: Throwable?

        data class DatabaseError(
            override val message: String,
            override val cause: Throwable? = null
        ) : Failure

        data class StorageError(
            override val message: String,
            override val cause: Throwable? = null
        ) : Failure

        data class OcrError(
            override val message: String,
            override val cause: Throwable? = null
        ) : Failure

        data class PdfGenerationError(
            override val message: String,
            override val cause: Throwable? = null
        ) : Failure

        data class SecurityError(
            override val message: String,
            override val cause: Throwable? = null
        ) : Failure

        data class GeneralError(
            override val message: String,
            override val cause: Throwable? = null
        ) : Failure
    }

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
}

fun <T> ScannerResult<T>.getOrNull(): T? = when (this) {
    is ScannerResult.Success -> data
    is ScannerResult.Failure -> null
}

fun <T> ScannerResult<T>.getOrDefault(defaultValue: T): T = when (this) {
    is ScannerResult.Success -> data
    is ScannerResult.Failure -> defaultValue
}

fun <T> ScannerResult<T>.getOrThrow(): T = when (this) {
    is ScannerResult.Success -> data
    is ScannerResult.Failure -> throw cause ?: IllegalStateException(message)
}

inline fun <T, R> ScannerResult<T>.map(transform: (T) -> R): ScannerResult<R> = when (this) {
    is ScannerResult.Success -> ScannerResult.Success(transform(data))
    is ScannerResult.Failure -> this
}

inline fun <T> ScannerResult<T>.onSuccess(action: (T) -> Unit): ScannerResult<T> {
    if (this is ScannerResult.Success) action(data)
    return this
}

inline fun <T> ScannerResult<T>.onFailure(action: (ScannerResult.Failure) -> Unit): ScannerResult<T> {
    if (this is ScannerResult.Failure) action(this)
    return this
}


