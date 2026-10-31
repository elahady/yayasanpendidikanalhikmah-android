package com.alhikmah.tpq.domain

import com.alhikmah.tpq.domain.api.ApiError
import com.alhikmah.tpq.domain.api.ApiException
import com.alhikmah.tpq.domain.api.ErrorCodes
import com.alhikmah.tpq.domain.util.Outcome
import com.alhikmah.tpq.domain.util.attempt
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ApiErrorTest {

    @Test
    fun `body error dari server dipakai apa adanya`() {
        val error = ApiError.fromResponse(
            403,
            """{"error":{"code":"SELF_ATTENDANCE_FORBIDDEN","message":"Tidak boleh mengisi absensi sendiri."}}""",
        )
        assertEquals(ErrorCodes.SELF_ATTENDANCE_FORBIDDEN, error.code)
        assertEquals("Tidak boleh mengisi absensi sendiri.", error.message)
        assertEquals(403, error.httpStatus)
        assertTrue(error.isSelfAttendance)
    }

    @Test
    fun `validasi 422 membawa pesan server`() {
        val error = ApiError.fromResponse(422, """{"error":{"code":"VALIDATION","message":"Tanggal tidak valid."}}""")
        assertEquals("VALIDATION", error.code)
        assertEquals("Tanggal tidak valid.", error.message)
    }

    @Test
    fun `body kosong atau bukan json jatuh ke pesan bawaan menurut status`() {
        assertEquals(ErrorCodes.UNAUTHENTICATED, ApiError.fromResponse(401, null).code)
        assertEquals(ErrorCodes.FORBIDDEN, ApiError.fromResponse(403, "").code)
        assertEquals(ErrorCodes.NOT_FOUND, ApiError.fromResponse(404, "<html>Not Found</html>").code)
        assertEquals(ErrorCodes.RATE_LIMITED, ApiError.fromResponse(429, "oops").code)
        val server = ApiError.fromResponse(502, "<html>Bad Gateway</html>")
        assertEquals(ErrorCodes.SERVER, server.code)
        assertEquals("Server sedang bermasalah. Coba lagi nanti.", server.message)
    }

    @Test
    fun `json tanpa objek error atau dengan field kosong tetap aman`() {
        assertEquals(ErrorCodes.NOT_FOUND, ApiError.fromResponse(404, """{"message":"x"}""").code)
        val partial = ApiError.fromResponse(422, """{"error":{"code":null,"message":""}}""")
        assertEquals(ErrorCodes.VALIDATION, partial.code)
        assertEquals("Data yang dikirim tidak valid.", partial.message)
    }

    @Test
    fun `401 dikenali sebagai tidak terautentikasi`() {
        assertTrue(ApiError.fromResponse(401, null).isUnauthenticated)
        assertFalse(ApiError.fromResponse(403, null).isUnauthenticated)
    }

    @Test
    fun `kegagalan jaringan ditandai connectivity`() {
        assertTrue(ApiError.network().isConnectivity)
        assertTrue(ApiError.timeout().isConnectivity)
        assertFalse(ApiError.fromResponse(500, null).isConnectivity)
    }

    @Test
    fun `attempt memetakan ApiException ke Err`() = runTest {
        val err = ApiError.fromResponse(404, null)
        val outcome = attempt<Int> { throw ApiException(err) }
        assertEquals(Outcome.Err(err), outcome)
        assertIs<Outcome.Ok<Int>>(attempt { 7 })
    }
}
