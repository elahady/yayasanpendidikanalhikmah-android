package com.alhikmah.tpq.data

import com.alhikmah.tpq.domain.api.ApiException
import com.alhikmah.tpq.domain.api.ErrorCodes
import com.alhikmah.tpq.domain.model.Jilid
import com.alhikmah.tpq.domain.model.Role
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.json.JSONObject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** GET /dashboard, GET /me (field baru), dan POST /me/password terhadap server palsu. */
class DashboardApiTest {
    private lateinit var server: MockWebServer
    private var unauthorizedCalls = 0

    private fun client() = ApiClient(
        baseUrl = server.url("/api/v1").toString(),
        token = { "tok-123" },
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
    fun `dashboard tanpa tanggal tidak mengirim query dan membawa bearer`() = runTest {
        server.enqueue(json(200, """{"tanggal":"2026-10-31","ringkasan":null,"anak":[]}"""))
        val d = HttpTpqRepository(client()).dashboard()
        assertNull(d.ringkasan)
        assertTrue(assertNotNull(d.anak).isEmpty())
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("Bearer tok-123", request.headers["Authorization"])
        assertEquals("/api/v1/dashboard", request.url.encodedPath)
        assertNull(request.url.encodedQuery)
    }

    @Test
    fun `dashboard dengan tanggal mengirim query tanggal dan mengurai ringkasan`() = runTest {
        server.enqueue(
            json(
                200,
                """{"tanggal":"2026-10-30","anak":null,"ringkasan":{
                  "santri":{"total":10,"hadir":8,"tidakHadir":1,"belumDicatat":1,"perJenjang":{"ULA":6,"WUSTHO":4},"perJilid":{"JILID_1":10}},
                  "ustadz":{"total":3,"hadir":3,"tidakHadir":0,"belumDicatat":0},
                  "tren":[],"perhatian":[],"aktivitas":[]}}""",
            ),
        )
        val d = HttpTpqRepository(client()).dashboard("2026-10-30")
        assertEquals("2026-10-30", server.takeRequest().url.queryParameter("tanggal"))
        assertEquals(8, d.ringkasan?.santri?.hadir)
        assertEquals(10, d.ringkasan?.santri?.perJilid?.get(Jilid.JILID_1))
        assertNull(d.anak)
    }

    @Test
    fun `dashboard 403 menampilkan pesan server tanpa logout`() = runTest {
        server.enqueue(json(403, """{"error":{"code":"FORBIDDEN","message":"Akun Anda tidak punya hak."}}"""))
        val e = assertFailsWith<ApiException> { HttpTpqRepository(client()).dashboard() }
        assertEquals("Akun Anda tidak punya hak.", e.error.message)
        assertEquals(0, unauthorizedCalls)
    }

    @Test
    fun `me membaca field baru`() = runTest {
        server.enqueue(
            json(
                200,
                """{"id":"1","username":"hasan","nama":"Ust. Hasan","roles":["PIKET","USTADZ"],"ustadzId":"u3","ustadzNama":"Hasan Basri","dibuatPada":"2026-10-27T03:12:00.000Z"}""",
            ),
        )
        val user = HttpAuthApi(client()).me()
        assertEquals("hasan", user.username)
        assertEquals("Hasan Basri", user.ustadzNama)
        assertEquals("2026-10-27T03:12:00.000Z", user.dibuatPada)
        assertEquals(setOf(Role.PIKET, Role.USTADZ), user.roles)
    }

    @Test
    fun `ubah password mengirim POST me password dengan body kontrak`() = runTest {
        server.enqueue(json(200, """{"ok":true}"""))
        HttpTpqRepository(client()).ubahPassword("lama123", "baru12345")
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/me/password", request.url.encodedPath)
        assertEquals("Bearer tok-123", request.headers["Authorization"])
        val body = JSONObject(request.body!!.utf8())
        assertEquals("lama123", body.getString("passwordLama"))
        assertEquals("baru12345", body.getString("passwordBaru"))
        assertEquals(2, body.length())
    }

    @Test
    fun `password lama salah 422 tampil sebagai pesan server dan tidak memicu logout`() = runTest {
        server.enqueue(json(422, """{"error":{"code":"VALIDATION","message":"Password sekarang salah."}}"""))
        val e = assertFailsWith<ApiException> { HttpTpqRepository(client()).ubahPassword("salah", "baru12345") }
        assertEquals(ErrorCodes.VALIDATION, e.error.code)
        assertEquals("Password sekarang salah.", e.error.message)
        assertFalse(e.error.isUnauthenticated)
        assertEquals(0, unauthorizedCalls)
    }

    @Test
    fun `ubah password 429 membawa Retry-After dan tidak memicu logout`() = runTest {
        server.enqueue(
            MockResponse.Builder().code(429).addHeader("Retry-After", "900")
                .body("""{"error":{"code":"RATE_LIMITED","message":"Terlalu banyak percobaan."}}""").build(),
        )
        val e = assertFailsWith<ApiException> { HttpTpqRepository(client()).ubahPassword("salah", "baru12345") }
        assertEquals(ErrorCodes.RATE_LIMITED, e.error.code)
        assertEquals(900, e.error.retryAfterSeconds)
        assertTrue(e.error.message.endsWith("Coba lagi dalam 15 menit."))
        assertEquals(0, unauthorizedCalls)
    }

    @Test
    fun `ubah password 401 (token benar-benar habis) tetap memicu onUnauthorized`() = runTest {
        server.enqueue(json(401, """{"error":{"code":"UNAUTHENTICATED","message":"Sesi habis."}}"""))
        assertFailsWith<ApiException> { HttpTpqRepository(client()).ubahPassword("lama123", "baru12345") }
        assertEquals(1, unauthorizedCalls)
    }

    // ---- hari libur (kontrak "Jadwal (hari belajar)") ----

    @Test
    fun `dashboard hari libur membaca hariBelajar false tingkat atas`() = runTest {
        server.enqueue(
            json(
                200,
                """{"tanggal":"2026-10-31","hariBelajar":false,"anak":null,"ringkasan":{
                  "santri":{"total":10,"hadir":0,"tidakHadir":0,"belumDicatat":0},
                  "ustadz":{"total":3,"hadir":0,"tidakHadir":0,"belumDicatat":0},
                  "tren":[],"perhatian":[],"aktivitas":[]}}""",
            ),
        )
        val d = HttpTpqRepository(client()).dashboard("2026-10-31")
        assertEquals("2026-10-31", server.takeRequest().url.queryParameter("tanggal"))
        assertFalse(d.hariBelajar)
        assertEquals(0, d.ringkasan?.santri?.belumDicatat)
    }

    @Test
    fun `dashboard server lama tanpa hariBelajar dianggap hari belajar`() = runTest {
        server.enqueue(json(200, """{"tanggal":"2026-10-30","ringkasan":null,"anak":[]}"""))
        assertTrue(HttpTpqRepository(client()).dashboard().hariBelajar)
    }

    @Test
    fun `rekap hari libur membaca hariBelajar false`() = runTest {
        server.enqueue(json(200, """{"tanggal":"2026-11-01","hariBelajar":false,"santri":{"hadir":0,"total":45,"belumDicatat":0},"ustadz":{"hadir":0,"total":6,"belumDicatat":0}}"""))
        val r = HttpTpqRepository(client()).rekap("2026-11-01")
        assertEquals("/api/v1/rekap", server.takeRequest().url.encodedPath)
        assertFalse(r.hariBelajar)
        assertEquals(0, r.santri.belumDicatat)
    }

    @Test
    fun `put absensi di hari libur 422 tampil apa adanya dan tidak memicu logout`() = runTest {
        val pesan = "TPQ libur pada hari Sabtu dan Minggu. Absensi tidak dapat dicatat."
        server.enqueue(json(422, """{"error":{"code":"VALIDATION","message":"$pesan"}}"""))
        server.enqueue(json(422, """{"error":{"code":"VALIDATION","message":"$pesan"}}"""))
        val repo = HttpTpqRepository(client())
        val santri = assertFailsWith<ApiException> { repo.simpanAbsensiSantri("s1", "2026-10-31", true, null) }
        val ustadz = assertFailsWith<ApiException> { repo.simpanAbsensiUstadz("u1", "2026-11-01", true, null) }
        for (e in listOf(santri, ustadz)) {
            assertEquals(422, e.error.httpStatus)
            assertEquals(ErrorCodes.VALIDATION, e.error.code)
            assertEquals(pesan, e.error.message)
        }
        assertEquals(0, unauthorizedCalls)
    }
}
