package com.alhikmah.tpq.domain

import com.alhikmah.tpq.domain.api.ApiJson
import com.alhikmah.tpq.domain.api.ApiException
import com.alhikmah.tpq.domain.api.ErrorCodes
import com.alhikmah.tpq.domain.fake.FakeData
import com.alhikmah.tpq.domain.fake.FakeTpqRepository
import com.alhikmah.tpq.domain.model.StatusHadir
import com.alhikmah.tpq.domain.util.Jadwal
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JadwalTest {

    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)

    @Test
    fun `senin sampai jumat hari belajar, sabtu dan minggu libur`() {
        // Pekan 26 Okt - 1 Nov 2026 (31 Okt = Sabtu).
        val pekan = (26..31).map { d(2026, 10, it) } + d(2026, 11, 1)
        assertEquals(
            listOf(true, true, true, true, true, false, false),
            pekan.map { Jadwal.hariBelajar(it) },
        )
        assertEquals(
            listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"),
            pekan.map { Jadwal.namaHari(it) },
        )
        assertEquals(pekan.map { !Jadwal.hariBelajar(it) }, pekan.map { Jadwal.libur(it) })
    }

    @Test
    fun `31 Oktober 2026 Sabtu libur dan 30 Oktober Jumat hari belajar`() {
        assertFalse(Jadwal.hariBelajar(d(2026, 10, 31)))
        assertEquals("Sabtu", Jadwal.namaHari(d(2026, 10, 31)))
        assertTrue(Jadwal.hariBelajar(d(2026, 10, 30)))
        assertEquals("Jumat", Jadwal.namaHari(d(2026, 10, 30)))
        assertFalse(Jadwal.hariBelajar("2026-10-31"))
        assertTrue(Jadwal.hariBelajar("2026-10-30"))
        assertFalse(Jadwal.hariBelajar("2026-11-01")) // Minggu
        assertTrue(Jadwal.hariBelajar("2026-11-02")) // Senin
    }

    @Test
    fun `tanggal API tidak valid atau kosong dianggap hari belajar, server yang memutuskan`() {
        assertTrue(Jadwal.hariBelajar(null as String?))
        assertTrue(Jadwal.hariBelajar(""))
        assertTrue(Jadwal.hariBelajar("2026-02-31"))
        assertTrue(Jadwal.hariBelajar("31/10/2026"))
        assertNull(Jadwal.hariBelajarTerakhir(null as String?))
        assertNull(Jadwal.hariBelajarTerakhir("bukan tanggal"))
    }

    @Test
    fun `hari belajar terakhir pada atau sebelum tanggal`() {
        assertEquals(d(2026, 10, 30), Jadwal.hariBelajarTerakhir(d(2026, 10, 30))) // Jumat tetap Jumat
        assertEquals(d(2026, 10, 30), Jadwal.hariBelajarTerakhir(d(2026, 10, 31))) // Sabtu -> Jumat
        assertEquals(d(2026, 10, 30), Jadwal.hariBelajarTerakhir(d(2026, 11, 1))) // Minggu -> Jumat
        assertEquals(d(2026, 11, 2), Jadwal.hariBelajarTerakhir(d(2026, 11, 2))) // Senin tetap Senin
        assertEquals("2026-10-30", Jadwal.hariBelajarTerakhir("2026-11-01"))
        assertEquals("2026-11-02", Jadwal.hariBelajarTerakhir("2026-11-02"))
    }

    @Test
    fun `mundur melewati batas bulan dan tahun`() {
        // 1 Jan 2027 = Jumat, 2-3 Jan = Sabtu-Minggu.
        assertTrue(Jadwal.hariBelajar(d(2027, 1, 1)))
        assertEquals(d(2027, 1, 1), Jadwal.hariBelajarTerakhir(d(2027, 1, 3)))
        // 1 Mei 2027 = Sabtu, 2 Mei = Minggu -> Jumat 30 Apr (ganti bulan).
        assertEquals(d(2027, 4, 30), Jadwal.hariBelajarTerakhir(d(2027, 5, 2)))
        // 1 Mar 2026 = Minggu -> Jumat 27 Feb.
        assertEquals(d(2026, 2, 27), Jadwal.hariBelajarTerakhir(d(2026, 3, 1)))
        // 1 Jan 2028 = Sabtu -> Jumat 31 Des 2027 (ganti tahun).
        assertEquals(d(2027, 12, 31), Jadwal.hariBelajarTerakhir(d(2028, 1, 1)))
        // Tahun kabisat: 29 Feb 2028 = Selasa.
        assertEquals("Selasa", Jadwal.namaHari(d(2028, 2, 29)))
        assertEquals(d(2028, 2, 29), Jadwal.hariBelajarTerakhir(d(2028, 2, 29)))
    }

    @Test
    fun `hari belajar sebelum selalu lebih awal dari tanggal`() {
        assertEquals(d(2026, 10, 30), Jadwal.hariBelajarSebelum(d(2026, 11, 2))) // Senin -> Jumat
        assertEquals(d(2026, 10, 30), Jadwal.hariBelajarSebelum(d(2026, 10, 31))) // Sabtu -> Jumat
        assertEquals(d(2026, 10, 30), Jadwal.hariBelajarSebelum(d(2026, 11, 1))) // Minggu -> Jumat
        assertEquals(d(2026, 10, 29), Jadwal.hariBelajarSebelum(d(2026, 10, 30))) // Jumat -> Kamis
    }

    @Test
    fun `hari belajar berikutnya dari jumat sabtu minggu adalah senin`() {
        assertEquals(d(2026, 11, 2), Jadwal.hariBelajarBerikutnya(d(2026, 10, 30)))
        assertEquals(d(2026, 11, 2), Jadwal.hariBelajarBerikutnya(d(2026, 10, 31)))
        assertEquals(d(2026, 11, 2), Jadwal.hariBelajarBerikutnya(d(2026, 11, 1)))
        assertEquals(d(2026, 11, 3), Jadwal.hariBelajarBerikutnya(d(2026, 11, 2)))
    }

    @Test
    fun `sifat untuk dua tahun penuh`() {
        var tanggal = d(2026, 1, 1)
        while (tanggal < d(2028, 1, 1)) {
            val terakhir = Jadwal.hariBelajarTerakhir(tanggal)
            assertTrue(Jadwal.hariBelajar(terakhir), "$tanggal -> $terakhir harus hari belajar")
            assertTrue(terakhir <= tanggal)
            assertTrue(ChronoUnit.DAYS.between(terakhir, tanggal) <= 2)
            if (Jadwal.hariBelajar(tanggal)) assertEquals(tanggal, terakhir)
            assertTrue(Jadwal.hariBelajarSebelum(tanggal) < tanggal)
            assertTrue(Jadwal.hariBelajar(Jadwal.hariBelajarBerikutnya(tanggal)))
            assertTrue(Jadwal.hariBelajarBerikutnya(tanggal) > tanggal)
            tanggal = tanggal.plusDays(1)
        }
    }

    @Test
    fun `teks libur`() {
        assertEquals("TPQ libur pada hari Sabtu dan Minggu. Absensi tidak dapat dicatat.", Jadwal.PESAN_SERVER_LIBUR)
        assertEquals("Libur — TPQ belajar Senin–Jumat", Jadwal.BANNER_LIBUR)
        assertEquals("Ke hari belajar terakhir", Jadwal.AKSI_HARI_BELAJAR_TERAKHIR)
        assertEquals("TPQ belajar Senin–Jumat. Kegiatan dilanjutkan hari Senin.", Jadwal.keteranganLibur(d(2026, 10, 31)))
        assertEquals("TPQ belajar Senin–Jumat. Kegiatan dilanjutkan hari Senin.", Jadwal.keteranganLibur(d(2026, 11, 1)))
    }

    // ---- parsing ----

    private val santriHitung = """{"hadir":1,"total":2,"belumDicatat":0}"""
    private val ustadzHitung = """{"hadir":0,"total":1,"belumDicatat":0}"""

    @Test
    fun `rekap membaca hariBelajar dan default true bila server lama`() {
        val libur = ApiJson.parseRekap("""{"tanggal":"2026-10-31","hariBelajar":false,"santri":$santriHitung,"ustadz":$ustadzHitung}""")
        assertFalse(libur.hariBelajar)
        val belajar = ApiJson.parseRekap("""{"tanggal":"2026-10-30","hariBelajar":true,"santri":$santriHitung,"ustadz":$ustadzHitung}""")
        assertTrue(belajar.hariBelajar)
        val lama = ApiJson.parseRekap("""{"tanggal":"2026-10-30","santri":$santriHitung,"ustadz":$ustadzHitung}""")
        assertTrue(lama.hariBelajar)
    }

    @Test
    fun `dashboard membaca hariBelajar tingkat atas dan default true bila server lama`() {
        val ringkas = """"ringkasan":{"santri":{"total":2,"hadir":0,"tidakHadir":0,"belumDicatat":0},"ustadz":{"total":1,"hadir":0,"tidakHadir":0,"belumDicatat":0}},"anak":null"""
        assertFalse(ApiJson.parseDashboard("""{"tanggal":"2026-10-31","hariBelajar":false,$ringkas}""").hariBelajar)
        assertTrue(ApiJson.parseDashboard("""{"tanggal":"2026-10-30","hariBelajar":true,$ringkas}""").hariBelajar)
        assertTrue(ApiJson.parseDashboard("""{"tanggal":"2026-10-30",$ringkas}""").hariBelajar)
    }

    // ---- fake meniru server ----

    private val jumat = "2026-10-30"
    private val sabtu = "2026-10-31"
    private val minggu = "2026-11-01"

    @Test
    fun `fake menolak absensi santri dan ustadz di hari libur dengan 422 dan pesan server`() = runTest {
        val repo = FakeTpqRepository(user = FakeData.userAdmin)
        for (tanggal in listOf(sabtu, minggu)) {
            val santri = assertFailsWith<ApiException> { repo.simpanAbsensiSantri("s1", tanggal, true, null) }
            assertEquals(422, santri.error.httpStatus)
            assertEquals(ErrorCodes.VALIDATION, santri.error.code)
            assertEquals("TPQ libur pada hari Sabtu dan Minggu. Absensi tidak dapat dicatat.", santri.error.message)
            val ustadz = assertFailsWith<ApiException> { repo.simpanAbsensiUstadz("u1", tanggal, true, null) }
            assertEquals(422, ustadz.error.httpStatus)
            assertEquals(Jadwal.PESAN_SERVER_LIBUR, ustadz.error.message)
        }
        assertTrue(repo.absensiSantriByTanggal(sabtu).isEmpty()) // tidak ada yang tersimpan
        repo.simpanAbsensiSantri("s1", jumat, true, null) // hari belajar tetap bisa
    }

    @Test
    fun `fake data lama di hari libur tetap bisa dibaca`() = runTest {
        val repo = FakeTpqRepository(user = FakeData.userAdmin, seedSantri = listOf(FakeData.entry("s1", sabtu, true)))
        assertEquals("s1", repo.absensiSantriByTanggal(sabtu).single().subjectId)
    }

    @Test
    fun `fake rekap hari libur hariBelajar false dan belumDicatat nol`() = runTest {
        val repo = FakeTpqRepository(user = FakeData.userAdmin)
        val libur = repo.rekap(sabtu)
        assertFalse(libur.hariBelajar)
        assertEquals(0, libur.santri.belumDicatat)
        assertEquals(0, libur.ustadz.belumDicatat)
        val belajar = repo.rekap(jumat)
        assertTrue(belajar.hariBelajar)
        assertEquals(FakeData.santri.size, belajar.santri.belumDicatat)
    }

    @Test
    fun `fake dashboard hari libur tanpa belum dicatat dan tanpa item belum dicatat`() = runTest {
        val repo = FakeTpqRepository(user = FakeData.userAdmin)
        val d = repo.dashboard(sabtu)
        assertFalse(d.hariBelajar)
        val r = assertNotNull(d.ringkasan)
        assertEquals(0, r.santri.belumDicatat)
        assertEquals(0, r.ustadz.belumDicatat)
        assertTrue(r.perhatian.none { it.kode.endsWith("BELUM_DICATAT") })
        assertTrue(repo.dashboard(jumat).hariBelajar)
        assertTrue(assertNotNull(repo.dashboard(jumat).ringkasan).perhatian.any { it.kode == "SANTRI_BELUM_DICATAT" })
    }

    @Test
    fun `fake dashboard wali tidak memuat tanggal libur sebagai belum dicatat`() = runTest {
        val seed = listOf(FakeData.entry("s2", "2026-10-29", true), FakeData.entry("s1", "2026-10-29", true))
        val repo = FakeTpqRepository(user = FakeData.userWali, seedSantri = seed, anakIds = setOf("s1"))
        val d = repo.dashboard(sabtu)
        assertFalse(d.hariBelajar)
        val k = assertNotNull(d.anak).single().kehadiran
        assertEquals(listOf("2026-10-29"), k.hari.map { it.tanggal })
        assertEquals(0, k.belumDicatat)
    }

    @Test
    fun `data contoh libur konsisten`() {
        for (libur in listOf(FakeData.dashboardAdminLibur, FakeData.dashboardTugasLibur, FakeData.dashboardWaliLibur)) {
            assertFalse(libur.hariBelajar)
            assertFalse(Jadwal.hariBelajar(libur.tanggal))
        }
        val r = assertNotNull(FakeData.dashboardAdminLibur.ringkasan)
        assertEquals(0, r.santri.belumDicatat)
        assertEquals(0, r.ustadz.belumDicatat)
        assertTrue(r.perhatian.none { it.kode.endsWith("BELUM_DICATAT") })
        // Sampel hari belajar memang hari belajar, dan semua hari tren/wali juga.
        assertTrue(FakeData.dashboardAdmin.hariBelajar)
        assertTrue(Jadwal.hariBelajar(FakeData.dashboardAdmin.tanggal))
        assertTrue(FakeData.trenContoh.all { Jadwal.hariBelajar(it.tanggal) })
        assertTrue(FakeData.anakAisyah.kehadiran.hari.all { Jadwal.hariBelajar(it.tanggal) })
        assertEquals(FakeData.anakAisyah.kehadiran.hari.size, FakeData.anakAisyah.kehadiran.run { hadir + tidakHadir + belumDicatat })
        assertEquals(StatusHadir.BELUM_DICATAT, FakeData.anakAisyah.kehadiran.hari.last().status)
        assertFalse(FakeData.rekap(sabtu).hariBelajar)
        assertTrue(FakeData.rekap(jumat).hariBelajar)
    }
}
