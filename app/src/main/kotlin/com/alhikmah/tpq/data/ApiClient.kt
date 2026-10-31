package com.alhikmah.tpq.data

import com.alhikmah.tpq.domain.api.ApiError
import com.alhikmah.tpq.domain.api.ApiException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Klien HTTP untuk API v1 (OkHttp; HttpURLConnection bawaan Android tidak mendukung PATCH).
 *
 * - [baseUrl] datang dari satu konstanta BuildConfig.API_BASE_URL.
 * - Header `Authorization: Bearer` dipasang bila [token] mengembalikan nilai.
 * - Jawaban non-2xx diubah menjadi [ApiException] dari body `{error:{code,message}}`.
 * - 401 pada permintaan yang membawa token memanggil [onUnauthorized] (app kembali ke login).
 * - Isi body/token tidak pernah ditulis ke log.
 */
class ApiClient(
    private val baseUrl: String,
    private val token: () -> String?,
    private val onUnauthorized: () -> Unit,
    private val http: OkHttpClient = defaultClient(),
) {

    suspend fun get(vararg segments: String, query: Map<String, String?> = emptyMap()): String =
        execute(Request.Builder().url(url(segments, query)).get())

    suspend fun post(vararg segments: String, body: JSONObject): String =
        execute(Request.Builder().url(url(segments)).post(body.toRequestBody()))

    suspend fun put(vararg segments: String, body: JSONObject): String =
        execute(Request.Builder().url(url(segments)).put(body.toRequestBody()))

    suspend fun patch(vararg segments: String, body: JSONObject): String =
        execute(Request.Builder().url(url(segments)).patch(body.toRequestBody()))

    private fun url(segments: Array<out String>, query: Map<String, String?> = emptyMap()): HttpUrl {
        val builder = baseUrl.toHttpUrl().newBuilder()
        segments.forEach { builder.addPathSegment(it) }
        query.forEach { (key, value) -> if (value != null) builder.addQueryParameter(key, value) }
        return builder.build()
    }

    private fun JSONObject.toRequestBody() = toString().toRequestBody(JSON)

    private suspend fun execute(builder: Request.Builder): String {
        val bearer = token()
        builder.header("Accept", "application/json")
        if (bearer != null) builder.header("Authorization", "Bearer $bearer")
        val response = try {
            http.newCall(builder.build()).await()
        } catch (_: SocketTimeoutException) {
            throw ApiException(ApiError.timeout())
        } catch (e: IOException) {
            throw ApiException(ApiError.network(), e)
        }
        val status: Int
        val retryAfter: String?
        val body: String
        try {
            response.use {
                status = it.code
                retryAfter = it.header("Retry-After")
                body = it.body.string()
            }
        } catch (e: IOException) {
            throw ApiException(ApiError.network(), e)
        }
        if (status in 200..299) return body
        if (status == 401 && bearer != null) onUnauthorized()
        throw ApiException(ApiError.fromResponse(status, body, retryAfter))
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(
            object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }

                override fun onFailure(call: Call, e: IOException) {
                    if (!continuation.isCancelled) continuation.resumeWithException(e)
                }
            },
        )
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            // API tidak mengalihkan; pengalihan bisa membawa token ke host lain.
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }
}
