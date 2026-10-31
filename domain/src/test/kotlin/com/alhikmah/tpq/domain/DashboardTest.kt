package com.alhikmah.tpq.domain

import com.alhikmah.tpq.domain.api.ApiException
import com.alhikmah.tpq.domain.api.ApiJson
import com.alhikmah.tpq.domain.api.ErrorCodes
import com.alhikmah.tpq.domain.fake.FakeData
import com.alhikmah.tpq.domain.fake.FakeTpqRepository
import com.alhikmah.tpq.domain.menu.MenuId
import com.alhikmah.tpq.domain.model.AksiPerhatian
import com.alhikmah.tpq.domain.model.Jenjang
import com.alhikmah.tpq.domain.model.Jilid
import com.alhikmah.tpq.domain.model.Role
import com.alhikmah.tpq.domain.model.StatusHadir
import com.alhikmah.tpq.domain.model.User
import com.alhikmah.tpq.domain.model.pilih
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DashboardTest {

    private val contohAdmin = """
        {"tanggal":"2026-10-31",
         "ringkasan":{
           "santri":{"total":86,"hadir":71,"tidakHadir":9,"belumDicatat":6,
                     "perJenjang":{"ULA":61,"WUSTHO":25},
                     "perJilid":{"PRA_NAHDLIYAH":9,"JILID_1":14,"JILID_2":17,"JILID_3":12,"JILID_4":10,"JILID_5":7,"JILID_6":5,"AL_QURAN":12}},
           "ustadz":{"total":9,"hadir":7,"tidakHadir":1,"belumDicatat":1},
           "tren":[{"tanggal":"2026-10-18","hadir":76,"tercatat":86}],
           "perhatian":[{"kode":"SANTRI_BELUM_DICATAT","jumlah":6,"judul":"6 santri belum dicatat absensinya",
                         "keterangan":"Aisyah, Fadhil, Rafi dan 3 lainnya","aksi":"ABSENSI_SANTRI"},
                        {"kode":"KODE_BARU","jumlah":1,"judul":"Sesuatu yang baru","keterangan":null,"aksi":"AKSI_BARU"}],
           "aktivitas":[{"waktu":"2026-10-31T01:12:00.000Z","oleh":"Ust. Hasan","jenis":"ABSENSI_SANTRI",
                         "ringkas":"Ust. Hasan mencatat absensi santri (12 santri)"}]},
         "anak":null}
    """.trimIndent()

    private val contohWali = """
        {"tanggal":"2026-10-31","ringkasan":null,
         "anak":[{"santri":{"id":"s1","nama":"Aisyah","jenjang":"ULA","kelas":"2","jilidSaatIni":"JILID_3","catatanProgressMadin":null},
                  "diperbaruiPada":"2026-10-29T09:20:00.000Z",
                  "kehadiran":{"hadir":1,"tidakHadir":1,"belumDicatat":1,
                    "hari":[{"tanggal":"2026-10-29","status":"HADIR"},{"tanggal":"2026-10-30","status":"TIDAK_HADIR"},{"tanggal":"2026-10-31","status":"BELUM_DICATAT"}]}}]}
    """.trimIndent()

    @Test
    fun `parse dashboard admin lengkap`() {
        val d = ApiJson.parseDashboard(contohAdmin)
        assertEquals("2026-10-31", d.tanggal)
        assertNull(d.anak)
        val r = assertNotNull(d.ringkasan)
        assertEquals(86, r.santri.total)
        assertEquals(71, r.santri.hadir)
        assertEquals(61, r.santri.perJenjang[Jenjang.ULA])
        assertEquals(12, r.santri.perJilid[Jilid.AL_QURAN])
        assertEquals(7, r.ustadz.hadir)
        assertEquals(8, r.ustadz.tercatat)
        assertEquals(76, r.tren.single().hadir)
        assertEquals(2, r.perhatian.size)
        assertEquals(AksiPerhatian.ABSENSI_SANTRI, r.perhatian[0].aksi)
        assertEquals("Aisyah, Fadhil, Rafi dan 3 lainnya", r.perhatian[0].keterangan)
        assertEquals("Ust. Hasan", r.aktivitas.single().oleh)
    }

    @Test
    fun `aksi tak dikenal menjadi null dan item tidak bisa diketuk`() {
        val r = assertNotNull(ApiJson.parseDashboard(contohAdmin).ringkasan)
        val baru = r.perhatian[1]
        assertNull(baru.aksi)
        assertNull(baru.keterangan)
        assertNull(baru.tujuan(MenuId.entries.toSet()))
    }

    @Test
    fun `tujuan perhatian hanya bila menunya tersedia untuk role`() {
        val item = assertNotNull(ApiJson.parseDashboard(contohAdmin).ringkasan).perhatian[0]
        assertEquals(MenuId.ABSENSI_SANTRI, item.tujuan(setOf(MenuId.ABSENSI_SANTRI, MenuId.REKAP_HARIAN)))
        assertNull(item.tujuan(setOf(MenuId.ABSENSI_USTADZ, MenuId.DAFTAR_SANTRI)))
    }

    @Test
    fun `persen dan bagian hadir`() {
        val s = assertNotNull(ApiJson.parseDashboard(contohAdmin).ringkasan).santri
        assertEquals(83, s.persenHadir) // 71/86 = 82,56%
        assertEquals(71f / 86f, s.bagianHadir)
        val kosong = s.copy(total = 0, hadir = 0)
        assertEquals(0, kosong.persenHadir)
        assertEquals(0f, kosong.bagianHadir)
    }

    @Test
    fun `field opsional hilang tidak membuat parsing gagal`() {
        val d = ApiJson.parseDashboard(
            """{"tanggal":"2026-10-31","ringkasan":{"santri":{"total":2,"hadir":1,"tidakHadir":0,"belumDicatat":1},
               "ustadz":{"total":1,"hadir":1,"tidakHadir":0,"belumDicatat":0}},"anak":null}""",
        )
        val r = assertNotNull(d.ringkasan)
        assertTrue(r.tren.isEmpty() && r.perhatian.isEmpty() && r.aktivitas.isEmpty())
        assertTrue(r.santri.perJilid.values.all { it == 0 })
        assertEquals(Jilid.entries.size, r.santri.perJilid.size)
    }

    @Test
    fun `field inti hilang menjadi BAD_RESPONSE`() {
        val e = assertFailsWith<ApiException> { ApiJson.parseDashboard("""{"tanggal":"2026-10-31","ringkasan":{"santri":{}}}""") }
        assertEquals(ErrorCodes.BAD_RESPONSE, e.error.code)
    }

    @Test
    fun `parse dashboard wali dengan status hari`() {
        val d = ApiJson.parseDashboard(contohWali)
        assertNull(d.ringkasan)
        val anak = assertNotNull(d.anak).single()
        assertEquals("Aisyah", anak.santri.nama)
        assertEquals("2026-10-29T09:20:00.000Z", anak.diperbaruiPada)
        assertEquals(
            listOf(StatusHadir.HADIR, StatusHadir.TIDAK_HADIR, StatusHadir.BELUM_DICATAT),
            anak.kehadiran.hari.map { it.status },
        )
    }

    @Test
    fun `status tak dikenal dianggap belum dicatat`() {
        assertEquals(StatusHadir.BELUM_DICATAT, StatusHadir.fromApi("LIBUR"))
        assertEquals(StatusHadir.BELUM_DICATAT, StatusHadir.fromApi(null))
    }

    @Test
    fun `pilih anak sesuai id lalu jatuh ke anak pertama`() {
        val daftar = listOf(FakeData.anakAisyah, FakeData.anakHanif)
        assertEquals("s6", daftar.pilih("s6")?.santri?.id)
        assertEquals("s1", daftar.pilih("tidak-ada")?.santri?.id)
        assertEquals("s1", daftar.pilih(null)?.santri?.id)
        assertNull(emptyList<com.alhikmah.tpq.domain.model.AnakDashboard>().pilih(null))
    }

    @Test
    fun `user baca field baru dan tetap jalan tanpa field itu`() {
        val baru = ApiJson.parseUser(
            """{"id":"1","username":"hasan","nama":"Ust. Hasan","roles":["PIKET","USTADZ"],"ustadzId":"u3","ustadzNama":"Hasan Basri","dibuatPada":"2026-10-27T03:12:00.000Z"}""",
        )
        assertEquals("hasan", baru.username)
        assertEquals("Hasan Basri", baru.ustadzNama)
        assertEquals("2026-10-27T03:12:00.000Z", baru.dibuatPada)
        val lama = ApiJson.parseUser("""{"id":"1","nama":"A","roles":["TU"],"ustadzId":null}""")
        assertNull(lama.username)
        assertNull(lama.ustadzNama)
        assertNull(lama.dibuatPada)
    }

    // ---- fake meniru server ----

    @Test
    fun `fake dashboard menghitung ringkasan dari data dan perhatian belum dicatat`() = runTest {
        val repo = FakeTpqRepository(user = FakeData.userAdmin)
        repo.simpanAbsensiSantri("s1", "2026-10-30", true, null)
        repo.simpanAbsensiSantri("s2", "2026-10-30", false, null)
        repo.simpanAbsensiUstadz("u1", "2026-10-30", true, null)
        val d = repo.dashboard("2026-10-30")
        val r = assertNotNull(d.ringkasan)
        assertNull(d.anak)
        assertEquals(FakeData.santri.size, r.santri.total)
        assertEquals(1, r.santri.hadir)
        assertEquals(1, r.santri.tidakHadir)
        assertEquals(FakeData.santri.size - 2, r.santri.belumDicatat)
        assertEquals(1, r.ustadz.hadir)
        assertEquals(FakeData.ustadz.size - 1, r.ustadz.belumDicatat)
        assertEquals(listOf("SANTRI_BELUM_DICATAT", "USTADZ_BELUM_DICATAT"), r.perhatian.map { it.kode })
        assertEquals(1, r.tren.size)
        assertEquals(2, r.tren.single().tercatat)
        assertTrue(r.santri.perJilid.getValue(Jilid.JILID_4) == 1)
    }

    @Test
    fun `fake piket murni tidak mendapat perhatian atau aktivitas santri`() = runTest {
        val piket = User("p", "Piket", setOf(Role.PIKET), null)
        val repo = FakeTpqRepository(user = piket, seedSantri = listOf(FakeData.entry("s1", "2026-10-30", true)))
        val r = assertNotNull(repo.dashboard("2026-10-30").ringkasan)
        assertTrue(r.perhatian.none { it.kode.startsWith("SANTRI") })
        assertTrue(r.aktivitas.none { it.jenis == "ABSENSI_SANTRI" })
    }

    @Test
    fun `fake wali murni hanya mendapat anak, role campuran mendapat keduanya`() = runTest {
        val wali = FakeTpqRepository(user = FakeData.userWali, anakIds = setOf("s1"))
        val dw = wali.dashboard("2026-10-31")
        assertNull(dw.ringkasan)
        assertEquals("s1", assertNotNull(dw.anak).single().santri.id)

        val campur = FakeTpqRepository(user = FakeData.userWaliUstadz, anakIds = setOf("s1", "s2"))
        val dc = campur.dashboard("2026-10-31")
        assertNotNull(dc.ringkasan)
        assertEquals(2, assertNotNull(dc.anak).size)

        val admin = FakeTpqRepository(user = FakeData.userAdmin).dashboard("2026-10-31")
        assertNull(admin.anak)
    }

    @Test
    fun `fake kehadiran anak hanya memuat hari belajar dan tanggal yang diminta`() = runTest {
        val seed = listOf(
            FakeData.entry("s1", "2026-10-29", true),
            FakeData.entry("s1", "2026-10-30", false),
            FakeData.entry("s2", "2026-10-28", true), // hari belajar karena santri lain punya entri; s1 belum dicatat
        )
        val repo = FakeTpqRepository(user = FakeData.userWali, seedSantri = seed, anakIds = setOf("s1"))
        val k = assertNotNull(repo.dashboard("2026-11-02").anak).single().kehadiran
        assertEquals(
            listOf("2026-10-28", "2026-10-29", "2026-10-30", "2026-11-02"),
            k.hari.map { it.tanggal },
        )
        assertEquals(1, k.hadir)
        assertEquals(1, k.tidakHadir)
        assertEquals(2, k.belumDicatat)
        assertEquals(k.hari.size, k.hadir + k.tidakHadir + k.belumDicatat)
    }

    @Test
    fun `fake dashboard menolak tanggal tidak valid dan mengikuti failWith`() = runTest {
        val repo = FakeTpqRepository(user = FakeData.userAdmin)
        assertEquals(ErrorCodes.VALIDATION, assertFailsWith<ApiException> { repo.dashboard("2026-02-31") }.error.code)
        repo.failWith = com.alhikmah.tpq.domain.api.ApiError.network()
        assertEquals(ErrorCodes.NETWORK, assertFailsWith<ApiException> { repo.dashboard() }.error.code)
    }

    // ---- ubah password ----

    @Test
    fun `fake ubah password berhasil lalu password lama tidak berlaku`() = runTest {
        val repo = FakeTpqRepository(user = FakeData.userAdmin)
        repo.ubahPassword(FakeData.PASSWORD, "passwordbaru1")
        val e = assertFailsWith<ApiException> { repo.ubahPassword(FakeData.PASSWORD, "yanglain123") }
        assertEquals("Password sekarang salah.", e.error.message)
        repo.ubahPassword("passwordbaru1", "yanglain123")
    }

    @Test
    fun `password lama salah adalah 422 VALIDATION bukan 401`() = runTest {
        val e = assertFailsWith<ApiException> { FakeTpqRepository().ubahPassword("salahsekali", "passwordbaru1") }
        assertEquals(422, e.error.httpStatus)
        assertEquals(ErrorCodes.VALIDATION, e.error.code)
        assertTrue(!e.error.isUnauthenticated)
    }

    @Test
    fun `password baru harus 8 sampai 72 karakter dan berbeda`() = runTest {
        val repo = FakeTpqRepository()
        assertEquals(422, assertFailsWith<ApiException> { repo.ubahPassword(FakeData.PASSWORD, "pendek") }.error.httpStatus)
        assertEquals(422, assertFailsWith<ApiException> { repo.ubahPassword(FakeData.PASSWORD, "x".repeat(73)) }.error.httpStatus)
        assertEquals(422, assertFailsWith<ApiException> { repo.ubahPassword(FakeData.PASSWORD, FakeData.PASSWORD) }.error.httpStatus)
        repo.ubahPassword(FakeData.PASSWORD, "x".repeat(72))
    }

    @Test
    fun `lima kali salah memicu 429 dengan Retry-After`() = runTest {
        val repo = FakeTpqRepository()
        repeat(5) { assertFailsWith<ApiException> { repo.ubahPassword("salahsekali", "passwordbaru1") } }
        val e = assertFailsWith<ApiException> { repo.ubahPassword(FakeData.PASSWORD, "passwordbaru1") }
        assertEquals(ErrorCodes.RATE_LIMITED, e.error.code)
        assertEquals(900, e.error.retryAfterSeconds)
    }

    @Test
    fun `body password sesuai kontrak`() {
        val body = ApiJson.passwordBody("lama123", "baru12345")
        assertEquals("lama123", body.getString("passwordLama"))
        assertEquals("baru12345", body.getString("passwordBaru"))
    }
}
