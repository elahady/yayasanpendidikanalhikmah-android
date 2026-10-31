package com.alhikmah.tpq.domain.util

import com.alhikmah.tpq.domain.api.ApiError
import com.alhikmah.tpq.domain.api.ApiException
import kotlinx.coroutines.CancellationException

/** Status pemuatan data untuk layar: memuat, berhasil, atau gagal dengan pesan siap tampil. */
sealed interface Load<out T> {
    data object Loading : Load<Nothing>

    data class Success<T>(val data: T) : Load<T>

    data class Failure(val error: ApiError) : Load<Nothing>
}

/** Hasil sederhana; sengaja bukan kotlin.Result supaya jenis errornya jelas (ApiError). */
sealed interface Outcome<out T> {
    data class Ok<T>(val value: T) : Outcome<T>

    data class Err(val error: ApiError) : Outcome<Nothing>
}

/** Menjalankan [block] dan memetakan [ApiException] ke [Outcome.Err]; pembatalan coroutine tidak ditelan. */
suspend fun <T> attempt(block: suspend () -> T): Outcome<T> = try {
    Outcome.Ok(block())
} catch (e: ApiException) {
    Outcome.Err(e.error)
} catch (e: CancellationException) {
    throw e
}

fun <T> Outcome<T>.toLoad(): Load<T> = when (this) {
    is Outcome.Ok -> Load.Success(value)
    is Outcome.Err -> Load.Failure(error)
}
