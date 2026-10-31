package com.alhikmah.tpq.data

import com.alhikmah.tpq.domain.api.ApiException
import com.alhikmah.tpq.domain.api.ApiJson
import com.alhikmah.tpq.domain.api.ErrorCodes
import com.alhikmah.tpq.domain.model.Jilid
import com.alhikmah.tpq.domain.model.ProgressUpdate
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Menguji klien HTTP terhadap server palsu: header, metode, body, dan pemetaan error sesuai kontrak. */
class ApiClientTest {
    private lateinit var server: MockWebServer
    private var token: String? = "tok-123"
    private var unauthorizedCalls = 0

    private fun client() = ApiClient(
        baseUrl = server.url("/api/v1").toString(),
        token = { token },
        onUnauthorized = { unauthorizedCalls++ },
    )

    private fun json(code: Int, body: String) = MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build()

    @BeforeTest
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @AfterTest
    fun tearDown() {
        server.close()
    }

    @Test
    fun `get membawa bearer token, path dan query yang benar`() = runTest {
        server.enqueue(json(200, """{"items":[]}"""))
        val repo = HttpTpqRepository(client())
        repo.santri("aisy ah")
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("Bearer tok-123", request.headers["Authorization"])
        assertEquals("/api/v1/santri?q=aisy%20ah", request.url.encodedPath + "?" + request.url.encodedQuery)
    }

    @Test
    fun `tanpa token tidak ada header Authorization`() = runTest {
        token = null
        server.enqueue(json(200, """{"accessToken":"jwt","user":{"id":"1","nama":"A","roles":["TU"],"ustadzId":null}}"""))
        val result = HttpAuthApi(client()).login("admin", "rahasia")
        assertEquals("jwt", result.accessToken)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertNull(request.headers["Authorization"])
        assertEquals("/api/v1/auth/login", request.url.encodedPath)
        assertTrue(request.body!!.utf8().contains("\"username\":\"admin\""))
    }

    @Test
    fun `put absensi mengirim body sesuai kontrak`() = runTest {
        server.enqueue(json(200, """{"id":"e1","santriId":"s1","tanggal":"2026-11-02","hadir":false,"keterangan":"Sakit","dicatatOleh":"TU","bisaDiubah":true}"""))
        val entry = HttpTpqRepository(client()).simpanAbsensiSantri("s1", "2026-11-02", false, "Sakit")
        assertFalse(entry.hadir)
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/absensi-santri", request.url.encodedPath)
        val body = org.json.JSONObject(request.body!!.utf8())
        assertEquals("s1", body.getString("santriId"))
        assertEquals("2026-11-02", body.getString("tanggal"))
        assertFalse(body.getBoolean("hadir"))
        assertEquals("Sakit", body.getString("keterangan"))
    }

    @Test
    fun `patch progress memakai metode PATCH`() = runTest {
        server.enqueue(json(200, """{"id":"s1","nama":"A","jenjang":"ULA","kelas":"1","jilidSaatIni":"JILID_3","catatanProgressMadin":null}"""))
        val santri = HttpTpqRepository(client()).updateProgress("s1", ProgressUpdate(jilid = Jilid.JILID_3))
        assertEquals(Jilid.JILID_3, santri.jilidSaatIni)
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/santri/s1/progress", request.url.encodedPath)
    }

    @Test
    fun `error server dipetakan ke pesan dan kode kontrak`() = runTest {
        server.enqueue(json(403, """{"error":{"code":"SELF_ATTENDANCE_FORBIDDEN","message":"Tidak boleh mengabsen diri sendiri."}}"""))
        val e = assertFailsWith<ApiException> { HttpTpqRepository(client()).simpanAbsensiUstadz("u1", "2026-11-02", true, null) }
        assertEquals(ErrorCodes.SELF_ATTENDANCE_FORBIDDEN, e.error.code)
        assertEquals("Tidak boleh mengabsen diri sendiri.", e.error.message)
        assertEquals(0, unauthorizedCalls)
    }

    @Test
    fun `401 dengan token memanggil onUnauthorized`() = runTest {
        server.enqueue(json(401, """{"error":{"code":"UNAUTHENTICATED","message":"Sesi habis."}}"""))
        val e = assertFailsWith<ApiException> { HttpAuthApi(client()).me() }
        assertTrue(e.error.isUnauthenticated)
        assertEquals(1, unauthorizedCalls)
    }

    @Test
    fun `401 saat login (tanpa token) tidak memicu logout otomatis`() = runTest {
        token = null
        server.enqueue(json(401, """{"error":{"code":"UNAUTHENTICATED","message":"Username atau password salah."}}"""))
        val e = assertFailsWith<ApiException> { HttpAuthApi(client()).login("x", "y") }
        assertEquals("Username atau password salah.", e.error.message)
        assertEquals(0, unauthorizedCalls)
    }

    @Test
    fun `429 membaca header Retry-After`() = runTest {
        token = null
        server.enqueue(
            MockResponse.Builder().code(429).addHeader("Retry-After", "30")
                .body("""{"error":{"code":"RATE_LIMITED","message":"Terlalu banyak percobaan masuk."}}""").build(),
        )
        val e = assertFailsWith<ApiException> { HttpAuthApi(client()).login("x", "y") }
        assertEquals(ErrorCodes.RATE_LIMITED, e.error.code)
        assertEquals(30, e.error.retryAfterSeconds)
        assertTrue(e.error.message.endsWith("Coba lagi dalam 30 detik."))
    }

    @Test
    fun `login akun nonaktif 403 menampilkan pesan server dan tidak mengeluarkan sesi`() = runTest {
        token = null
        server.enqueue(json(403, """{"error":{"code":"FORBIDDEN","message":"Akun Anda dinonaktifkan. Hubungi admin."}}"""))
        val e = assertFailsWith<ApiException> { HttpAuthApi(client()).login("x", "benar") }
        assertEquals("Akun Anda dinonaktifkan. Hubungi admin.", e.error.message)
        assertEquals(0, unauthorizedCalls)
    }

    @Test
    fun `500 INTERNAL`() = runTest {
        server.enqueue(json(500, """{"error":{"code":"INTERNAL","message":"Terjadi kesalahan di server."}}"""))
        val e = assertFailsWith<ApiException> { HttpTpqRepository(client()).ustadz() }
        assertEquals(ErrorCodes.INTERNAL, e.error.code)
    }

    @Test
    fun `body bukan json pada error 502 jatuh ke pesan bawaan`() = runTest {
        server.enqueue(MockResponse.Builder().code(502).body("<html>Bad Gateway</html>").build())
        val e = assertFailsWith<ApiException> { HttpTpqRepository(client()).rekap("2026-11-02") }
        assertEquals(ErrorCodes.SERVER, e.error.code)
    }

    @Test
    fun `jawaban sukses yang tidak sesuai kontrak menjadi BAD_RESPONSE`() = runTest {
        server.enqueue(json(200, """{"foo":1}"""))
        val e = assertFailsWith<ApiException> { HttpTpqRepository(client()).rekap("2026-11-02") }
        assertEquals(ErrorCodes.BAD_RESPONSE, e.error.code)
    }

    @Test
    fun `server mati menjadi error jaringan`() = runTest {
        val client = client()
        server.close()
        val e = assertFailsWith<ApiException> { HttpTpqRepository(client).ustadz() }
        assertTrue(e.error.isConnectivity)
        // pastikan helper parse tidak terpakai di sini
        assertEquals("santriId", ApiJson.SUBJECT_SANTRI)
    }
}
