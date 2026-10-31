package com.alhikmah.tpq.domain.api

import org.json.JSONException
import org.json.JSONObject

/** Kode error dari kontrak API v1 (plus beberapa kode lokal untuk kegagalan sebelum/sesudah server menjawab). */
object ErrorCodes {
    const val UNAUTHENTICATED = "UNAUTHENTICATED"
    const val FORBIDDEN = "FORBIDDEN"
    const val SELF_ATTENDANCE_FORBIDDEN = "SELF_ATTENDANCE_FORBIDDEN"
    const val NOT_FOUND = "NOT_FOUND"
    const val VALIDATION = "VALIDATION"
    const val RATE_LIMITED = "RATE_LIMITED"

    // Lokal (bukan dari server).
    const val NETWORK = "NETWORK"
    const val TIMEOUT = "TIMEOUT"
    const val SERVER = "SERVER"
    const val BAD_RESPONSE = "BAD_RESPONSE"
    const val UNKNOWN = "UNKNOWN"
}

/** Error dari server (body `{error:{code,message}}`) atau kegagalan jaringan, selalu dengan pesan siap tampil. */
data class ApiError(
    val code: String,
    val message: String,
    /** 0 bila belum ada jawaban HTTP (jaringan putus, timeout). */
    val httpStatus: Int = 0,
) {
    val isUnauthenticated: Boolean get() = httpStatus == 401 || code == ErrorCodes.UNAUTHENTICATED
    val isSelfAttendance: Boolean get() = code == ErrorCodes.SELF_ATTENDANCE_FORBIDDEN
    val isConnectivity: Boolean get() = code == ErrorCodes.NETWORK || code == ErrorCodes.TIMEOUT

    companion object {
        fun network() = ApiError(ErrorCodes.NETWORK, "Tidak bisa terhubung ke server. Periksa koneksi internet lalu coba lagi.")

        fun timeout() = ApiError(ErrorCodes.TIMEOUT, "Server terlalu lama menjawab. Coba lagi sebentar lagi.")

        fun badResponse(httpStatus: Int = 0) = ApiError(ErrorCodes.BAD_RESPONSE, "Jawaban server tidak dikenali. Coba lagi nanti.", httpStatus)

        /**
         * Membentuk error dari jawaban HTTP non-2xx. Bila body berisi `error.message` dari server, pesan itu dipakai apa adanya;
         * bila body kosong/bukan JSON (mis. halaman error proxy), dipakai pesan bawaan menurut status.
         */
        fun fromResponse(httpStatus: Int, body: String?): ApiError {
            val parsed = parseBody(body)
            val code = parsed?.first?.takeIf { it.isNotBlank() } ?: defaultCode(httpStatus)
            val message = parsed?.second?.takeIf { it.isNotBlank() } ?: defaultMessage(httpStatus)
            return ApiError(code, message, httpStatus)
        }

        private fun parseBody(body: String?): Pair<String?, String?>? {
            if (body.isNullOrBlank()) return null
            return try {
                val error = JSONObject(body).optJSONObject("error") ?: return null
                val code = if (error.isNull("code")) null else error.optString("code")
                val message = if (error.isNull("message")) null else error.optString("message")
                code to message
            } catch (_: JSONException) {
                null
            }
        }

        private fun defaultCode(status: Int): String = when {
            status == 401 -> ErrorCodes.UNAUTHENTICATED
            status == 403 -> ErrorCodes.FORBIDDEN
            status == 404 -> ErrorCodes.NOT_FOUND
            status == 422 -> ErrorCodes.VALIDATION
            status == 429 -> ErrorCodes.RATE_LIMITED
            status >= 500 -> ErrorCodes.SERVER
            else -> ErrorCodes.UNKNOWN
        }

        private fun defaultMessage(status: Int): String = when {
            status == 401 -> "Sesi berakhir atau username/password salah. Silakan masuk lagi."
            status == 403 -> "Anda tidak punya izin untuk tindakan ini."
            status == 404 -> "Data tidak ditemukan."
            status == 422 -> "Data yang dikirim tidak valid."
            status == 429 -> "Terlalu banyak percobaan. Tunggu sebentar lalu coba lagi."
            status >= 500 -> "Server sedang bermasalah. Coba lagi nanti."
            else -> "Terjadi kesalahan (kode $status)."
        }
    }
}

/** Dilempar repository saat permintaan gagal; UI cukup menampilkan [error].message. */
class ApiException(val error: ApiError, cause: Throwable? = null) : Exception(error.message, cause)
