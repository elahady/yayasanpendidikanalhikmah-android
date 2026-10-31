package com.alhikmah.tpq.domain

import com.alhikmah.tpq.domain.api.ApiJson
import com.alhikmah.tpq.domain.beranda.AktivitasBeranda
import com.alhikmah.tpq.domain.beranda.TrenKehadiran
import com.alhikmah.tpq.domain.beranda.UstadzHariIniBeranda
import com.alhikmah.tpq.domain.beranda.WaktuAktivitas
import com.alhikmah.tpq.domain.fake.FakeData
import com.alhikmah.tpq.domain.fake.FakeTpqRepository
import com.alhikmah.tpq.domain.model.Aktivitas
import com.alhikmah.tpq.domain.model.Role
import com.alhikmah.tpq.domain.model.StatusHadir
import com.alhikmah.tpq.domain.model.TrenHarian
import com.alhikmah.tpq.domain.model.User
import com.alhikmah.tpq.domain.model.UstadzHariIni
import com.alhikmah.tpq.domain.model.UstadzRingkas
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UstadzDaftarParsingTest {

    private fun dashboard(ustadzJson: String) = ApiJson.parseDashboard(
        """{"tanggal":"2026-10-31","ringkasan":{"santri":{"total":2,"hadir":1,"tidakHadir":0,"belumDicatat":1},
           "ustadz":$ustadzJson},"anak":null}""",
    ).ringkasan!!.ustadz

    @Test
    fun `daftar ustadz diparse lengkap dengan status`() {
        val u = dashboard(
            """{"total":3,"hadir":1,"tidakHadir":1,"belumDicatat":1,"daftar":[
                {"id":"u1","nama":"Ust. Kholil","mapelDiajar":"Tahfidz","status":"HADIR"},
                {"id":"u2","nama":"Bu Nur","mapelDiajar":null,"status":"TIDAK_HADIR"},
                {"id":"u3","nama":"Zainal","mapelDiajar":"Fiqih","status":"BELUM_DICATAT"}]}""",
        )
        assertEquals(listOf("u1", "u2", "u3"), u.daftar.map { it.id })
        assertEquals("Tahfidz", u.daftar[0].mapelDiajar)
        assertNull(u.daftar[1].mapelDiajar)
        assertEquals(listOf(StatusHadir.HADIR, StatusHadir.TIDAK_HADIR, StatusHadir.BELUM_DICATAT), u.daftar.map { it.status })
    }

    @Test
    fun `server lama tanpa daftar menghasilkan daftar kosong`() {
        val u = dashboard("""{"total":9,"hadir":7,"tidakHadir":1,"belumDicatat":1}""")
        assertTrue(u.daftar.isEmpty())
        assertEquals(9, u.total)
    }

    @Test
    fun `status tak dikenal pada daftar dianggap belum dicatat`() {
        val u = dashboard("""{"total":1,"hadir":0,"tidakHadir":0,"belumDicatat":1,"daftar":[{"id":"u1","nama":"A","mapelDiajar":null,"status":"LIBUR"}]}""")
        assertEquals(StatusHadir.BELUM_DICATAT, u.daftar.single().status)
    }

    @Test
    fun `fake menghitung daftar ustadz urut nama dari absensi`() = runTest {
        val repo = FakeTpqRepository(user = FakeData.userAdmin)
        repo.simpanAbsensiUstadz("u3", "2026-10-31", true, null)
        repo.simpanAbsensiUstadz("u1", "2026-10-31", false, null)
        val u = repo.dashboard("2026-10-31").ringkasan!!.ustadz
        assertEquals(FakeData.ustadz.sortedBy { it.nama.lowercase() }.map { it.id }, u.daftar.map { it.id })
        assertEquals(StatusHadir.TIDAK_HADIR, u.daftar.first { it.id == "u1" }.status)
        assertEquals(StatusHadir.HADIR, u.daftar.first { it.id == "u3" }.status)
        assertEquals(StatusHadir.BELUM_DICATAT, u.daftar.first { it.id == "u2" }.status)
        // Angka dan daftar harus konsisten.
        assertEquals(u.hadir, u.daftar.count { it.status == StatusHadir.HADIR })
        assertEquals(u.tidakHadir, u.daftar.count { it.status == StatusHadir.TIDAK_HADIR })
        assertEquals(u.belumDicatat, u.daftar.count { it.status == StatusHadir.BELUM_DICATAT })
    }

    @Test
    fun `data contoh preview konsisten dengan angka`() {
        for (r in listOf(FakeData.dashboardAdmin.ringkasan!!, FakeData.dashboardTugas.ringkasan!!)) {
            val u = r.ustadz
            assertEquals(u.total, u.daftar.size)
            assertEquals(u.hadir, u.daftar.count { it.status == StatusHadir.HADIR })
            assertEquals(u.tidakHadir, u.daftar.count { it.status == StatusHadir.TIDAK_HADIR })
            assertEquals(u.belumDicatat, u.daftar.count { it.status == StatusHadir.BELUM_DICATAT })
        }
        assertEquals(14, FakeData.ringkasan.tren.size)
    }
}

class UstadzHariIniBerandaTest {

    private fun ustadz(vararg s: Pair<String, StatusHadir>) = UstadzRingkas(
        total = s.size,
        hadir = s.count { it.second == StatusHadir.HADIR },
        tidakHadir = s.count { it.second == StatusHadir.TIDAK_HADIR },
        belumDicatat = s.count { it.second == StatusHadir.BELUM_DICATAT },
        daftar = s.map { UstadzHariIni(it.first, "Nama ${it.first}", null, it.second) },
    )

    private val piket = User("p", "Piket", setOf(Role.PIKET, Role.USTADZ), "u2")
    private val admin = User("a", "Admin", setOf(Role.TU), "u2")
    private val tanpaTautan = User("x", "Piket", setOf(Role.PIKET), null)

    private val contoh = ustadz("u1" to StatusHadir.HADIR, "u2" to StatusHadir.HADIR, "u3" to StatusHadir.TIDAK_HADIR)

    @Test
    fun `baris ustadz sendiri disembunyikan dan catatan muncul`() {
        val t = UstadzHariIniBeranda.susun(contoh, piket)
        assertEquals(listOf("u3", "u1"), t.baris.map { it.id }) // tidak hadir dulu, lalu hadir
        assertTrue(t.catatanSendiri)
        assertTrue(t.adaDaftar)
    }

    @Test
    fun `admin atau tu melihat baris sendiri tanpa catatan`() {
        val t = UstadzHariIniBeranda.susun(contoh, admin)
        assertEquals(3, t.baris.size)
        assertTrue(t.baris.any { it.id == "u2" })
        assertFalse(t.catatanSendiri)
    }

    @Test
    fun `akun tanpa tautan ustadz tidak menyembunyikan apa pun`() {
        val t = UstadzHariIniBeranda.susun(contoh, tanpaTautan)
        assertEquals(3, t.baris.size)
        assertFalse(t.catatanSendiri)
    }

    @Test
    fun `tidak ada catatan bila ustadz tertaut tidak ada di daftar`() {
        val t = UstadzHariIniBeranda.susun(ustadz("u1" to StatusHadir.HADIR), piket)
        assertFalse(t.catatanSendiri)
    }

    @Test
    fun `belum dicatat di atas lalu tidak hadir lalu hadir, urutan nama terjaga di dalam kelompok`() {
        val t = UstadzHariIniBeranda.susun(
            ustadz("a" to StatusHadir.HADIR, "b" to StatusHadir.BELUM_DICATAT, "c" to StatusHadir.HADIR, "d" to StatusHadir.TIDAK_HADIR, "e" to StatusHadir.BELUM_DICATAT),
            tanpaTautan,
        )
        assertEquals(listOf("b", "e", "d", "a", "c"), t.baris.map { it.id })
    }

    @Test
    fun `dipotong sampai lima baris dan sisanya dihitung`() {
        val sembilan = ustadz(*(1..9).map { "u$it" to StatusHadir.HADIR }.toTypedArray())
        val t = UstadzHariIniBeranda.susun(sembilan, tanpaTautan)
        assertEquals(5, t.baris.size)
        assertEquals(4, t.lainnya)
        val tujuh = UstadzHariIniBeranda.susun(sembilan, User("p", "P", setOf(Role.PIKET), "u1"))
        assertEquals(8, tujuh.baris.size + tujuh.lainnya) // u1 tersembunyi
        assertEquals(3, tujuh.lainnya)
    }

    @Test
    fun `pas lima baris tidak ada sisa`() {
        val t = UstadzHariIniBeranda.susun(ustadz(*(1..5).map { "u$it" to StatusHadir.HADIR }.toTypedArray()), tanpaTautan)
        assertEquals(0, t.lainnya)
    }

    @Test
    fun `server lama tanpa daftar tetap memberi catatan untuk akun tertaut tanpa hak admin`() {
        val lama = UstadzRingkas(9, 7, 1, 1)
        val t = UstadzHariIniBeranda.susun(lama, piket)
        assertFalse(t.adaDaftar)
        assertTrue(t.baris.isEmpty())
        assertTrue(t.catatanSendiri)
        assertFalse(UstadzHariIniBeranda.susun(lama, admin).catatanSendiri)
        assertFalse(UstadzHariIniBeranda.susun(lama, tanpaTautan).catatanSendiri)
    }

    @Test
    fun `data contoh piket+ustadz menyembunyikan Hasan`() {
        val r = assertNotNull(FakeData.dashboardTugas.ringkasan)
        val t = UstadzHariIniBeranda.susun(r.ustadz, FakeData.userPiketUstadz)
        assertTrue(t.baris.none { it.id == "u3" })
        assertTrue(t.catatanSendiri)
        assertEquals(5, t.baris.size)
    }
}

class TrenKehadiranTest {

    @Test
    fun `kosong menghasilkan null`() {
        assertNull(TrenKehadiran.susun(emptyList(), "2026-10-31"))
    }

    @Test
    fun `tinggi batang dan persen = hadir per tercatat`() {
        val t = assertNotNull(TrenKehadiran.susun(listOf(TrenHarian("2026-10-30", 43, 86), TrenHarian("2026-10-31", 80, 80)), "2026-10-31"))
        assertEquals(0.5f, t.batang[0].tinggi)
        assertEquals(50, t.batang[0].persen)
        assertEquals(1f, t.batang[1].tinggi)
        assertEquals(100, t.batang[1].persen)
    }

    @Test
    fun `hari tanpa catatan bertinggi nol dan tidak ikut rata-rata`() {
        val t = assertNotNull(
            TrenKehadiran.susun(listOf(TrenHarian("2026-10-29", 0, 0), TrenHarian("2026-10-30", 80, 100), TrenHarian("2026-10-31", 60, 100)), "2026-10-31"),
        )
        assertEquals(0f, t.batang[0].tinggi)
        assertEquals(70, t.rataRataPersen) // (80% + 60%) / 2
    }

    @Test
    fun `semua hari tanpa catatan tidak punya rata-rata`() {
        val t = assertNotNull(TrenKehadiran.susun(listOf(TrenHarian("2026-10-31", 0, 0)), "2026-10-31"))
        assertNull(t.rataRataPersen)
    }

    @Test
    fun `hadir melebihi tercatat dijepit ke 100 persen`() {
        val t = assertNotNull(TrenKehadiran.susun(listOf(TrenHarian("2026-10-31", 9, 5)), "2026-10-31"))
        assertEquals(1f, t.batang.single().tinggi)
    }

    @Test
    fun `rata-rata dari data contoh dan label awal akhir`() {
        val t = assertNotNull(TrenKehadiran.susun(FakeData.trenContoh, "2026-10-31"))
        assertEquals(14, t.batang.size)
        assertEquals("15 Okt", t.labelAwal)
        assertEquals("Hari ini · 89%", t.labelAkhir)
        assertTrue(t.rataRataPersen in 80..95)
    }

    @Test
    fun `label akhir memakai tanggal bila hari terakhir bukan hari ini`() {
        val t = assertNotNull(TrenKehadiran.susun(listOf(TrenHarian("2026-10-29", 8, 10), TrenHarian("2026-10-30", 9, 10)), "2026-10-31"))
        assertEquals("30 Okt · 90%", t.labelAkhir)
    }

    @Test
    fun `deskripsi pembaca layar memuat rata-rata dan tiap hari`() {
        val t = assertNotNull(TrenKehadiran.susun(listOf(TrenHarian("2026-10-30", 9, 10), TrenHarian("2026-10-31", 71, 80)), "2026-10-31"))
        assertTrue("rata-rata 89 persen" in t.deskripsi, t.deskripsi)
        assertTrue("30 Okt: 90 persen, 9 dari 10 tercatat" in t.deskripsi, t.deskripsi)
        assertTrue("Hari ini: 89 persen, 71 dari 80 tercatat" in t.deskripsi, t.deskripsi)
    }
}

class AktivitasBerandaTest {
    private val hariIni = LocalDate.of(2026, 10, 31)

    @Test
    fun `jam memakai zona Asia Jakarta`() {
        assertEquals("08.12", WaktuAktivitas.jam("2026-10-31T01:12:00.000Z"))
        // 17.30 UTC = 00.30 WIB hari berikutnya
        assertEquals("00.30", WaktuAktivitas.jam("2026-10-30T17:30:00.000Z"))
        assertNull(WaktuAktivitas.jam("2026-10-31"))
        assertNull(WaktuAktivitas.jam(null))
    }

    @Test
    fun `label hari ini, kemarin, dan tanggal lain`() {
        assertEquals("Hari ini, 08.12", WaktuAktivitas.label("2026-10-31T01:12:00.000Z", hariIni))
        assertEquals("Kemarin, 16.20", WaktuAktivitas.label("2026-10-30T09:20:00.000Z", hariIni))
        assertEquals("27 Okt, 09.20", WaktuAktivitas.label("2026-10-27T02:20:00.000Z", hariIni))
    }

    @Test
    fun `tanggal diambil dari zona Jakarta bukan UTC`() {
        // 30 Okt 17.30 UTC sudah 31 Okt 00.30 WIB = hari ini
        assertEquals("Hari ini, 00.30", WaktuAktivitas.label("2026-10-30T17:30:00.000Z", hariIni))
    }

    @Test
    fun `waktu tanpa jam hanya menampilkan hari, waktu rusak apa adanya`() {
        assertEquals("Hari ini", WaktuAktivitas.label("2026-10-31", hariIni))
        assertEquals("kemarin sore", WaktuAktivitas.label("kemarin sore", hariIni))
    }

    @Test
    fun `susun memotong dan memakai ringkas dari server`() {
        val daftar = (1..8).map { Aktivitas("2026-10-31T01:12:00.000Z", "x", "ABSENSI_SANTRI", "ringkas $it") }
        val t = AktivitasBeranda.susun(daftar, hariIni)
        assertEquals(6, t.size)
        assertEquals("ringkas 1", t.first().ringkas)
        assertEquals("Hari ini, 08.12", t.first().waktu)
        assertTrue(AktivitasBeranda.susun(emptyList(), hariIni).isEmpty())
    }
}
