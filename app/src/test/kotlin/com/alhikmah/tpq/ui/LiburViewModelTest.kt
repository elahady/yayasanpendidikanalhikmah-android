package com.alhikmah.tpq.ui

import com.alhikmah.tpq.domain.api.ApiError
import com.alhikmah.tpq.domain.api.ApiException
import com.alhikmah.tpq.domain.api.ErrorCodes
import com.alhikmah.tpq.domain.api.TpqRepository
import com.alhikmah.tpq.domain.fake.FakeData
import com.alhikmah.tpq.domain.fake.FakeTpqRepository
import com.alhikmah.tpq.domain.model.AbsensiEntry
import com.alhikmah.tpq.domain.model.Dashboard
import com.alhikmah.tpq.domain.model.Rekap
import com.alhikmah.tpq.domain.model.User
import com.alhikmah.tpq.domain.util.Jadwal
import com.alhikmah.tpq.domain.util.Load
import com.alhikmah.tpq.ui.absensi.AbsensiKind
import com.alhikmah.tpq.ui.absensi.AbsensiViewModel
import com.alhikmah.tpq.ui.absensi.isEditable
import com.alhikmah.tpq.ui.beranda.BerandaViewModel
import com.alhikmah.tpq.ui.rekap.RekapViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.time.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Hari libur (Sabtu/Minggu) di layar Absensi, Rekap, dan Beranda. Semua tanggal tetap supaya tes tidak bergantung pada hari ini. */
@OptIn(ExperimentalCoroutinesApi::class)
class LiburViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val jumat = LocalDate.of(2026, 10, 30)
    private val sabtu = LocalDate.of(2026, 10, 31)
    private val minggu = LocalDate.of(2026, 11, 1)
    private val senin = LocalDate.of(2026, 11, 2)

    private fun rows(vm: AbsensiViewModel) = (vm.state.value.load as Load.Success).data

    private fun admin(repo: TpqRepository = FakeTpqRepository(user = FakeData.userAdmin), kind: AbsensiKind, tanggal: LocalDate) =
        AbsensiViewModel(repo, FakeData.userAdmin, kind, tanggal)

    // ---- Absensi ----

    @Test
    fun `absensi pada hari libur terkunci walau role boleh menulis`() {
        val vm = admin(kind = AbsensiKind.SANTRI, tanggal = sabtu)
        assertTrue(vm.state.value.libur)
        assertTrue(vm.state.value.canWrite) // hak role tidak berubah
        assertTrue(rows(vm).none { vm.state.value.isEditable(it) })
    }

    @Test
    fun `absensi pada hari belajar tidak libur dan bisa diubah`() {
        val vm = admin(kind = AbsensiKind.SANTRI, tanggal = jumat)
        assertFalse(vm.state.value.libur)
        assertTrue(rows(vm).all { vm.state.value.isEditable(it) })
    }

    @Test
    fun `simpan pada hari libur tidak mengirim apa pun ke server`() {
        val vm = admin(kind = AbsensiKind.USTADZ, tanggal = minggu)
        vm.setHadir("u1", false)
        vm.save("u1")
        assertNull(rows(vm).first { it.person.id == "u1" }.entry)
        assertTrue(vm.state.value.status["u1"]?.saving != true)
        assertNull(vm.state.value.status["u1"]?.error) // bukan galat: tombolnya memang terkunci
    }

    @Test
    fun `data lama di hari libur tetap terlihat`() {
        val seeded = FakeData.entry("s1", "2026-10-31", hadir = false, keterangan = "Sakit")
        val repo = FakeTpqRepository(user = FakeData.userAdmin, seedSantri = listOf(seeded))
        val vm = admin(repo, AbsensiKind.SANTRI, sabtu)
        val entry = assertNotNull(rows(vm).first { it.person.id == "s1" }.entry)
        assertFalse(entry.hadir)
        assertEquals("Sakit", entry.keterangan)
        assertTrue(vm.state.value.libur)
    }

    @Test
    fun `ke hari belajar terakhir dari Sabtu dan Minggu menuju Jumat lalu memuat ulang`() {
        val vm = admin(kind = AbsensiKind.SANTRI, tanggal = sabtu)
        vm.setHadir("s1", false) // draft dibuang saat pindah tanggal
        vm.keHariBelajarTerakhir()
        assertEquals(jumat, vm.state.value.tanggal)
        assertFalse(vm.state.value.libur)
        assertTrue(vm.state.value.drafts.isEmpty())
        assertTrue(rows(vm).all { vm.state.value.isEditable(it) })

        val vm2 = admin(kind = AbsensiKind.USTADZ, tanggal = minggu)
        vm2.keHariBelajarTerakhir()
        assertEquals(jumat, vm2.state.value.tanggal)
    }

    @Test
    fun `ke hari belajar terakhir tidak mengubah tanggal hari belajar`() {
        val vm = admin(kind = AbsensiKind.SANTRI, tanggal = senin)
        vm.keHariBelajarTerakhir()
        assertEquals(senin, vm.state.value.tanggal)
    }

    @Test
    fun `pindah dari hari belajar ke Sabtu mengunci dan kembali ke Senin membuka`() {
        val vm = admin(kind = AbsensiKind.SANTRI, tanggal = jumat)
        vm.setTanggal(sabtu)
        assertTrue(vm.state.value.libur)
        vm.setTanggal(senin)
        assertFalse(vm.state.value.libur)
    }

    @Test
    fun `422 libur dari server pada tanggal yang lolos dari jadwal lokal tampil apa adanya di baris`() {
        // Server menganggap Jumat libur (mis. kalender libur khusus nanti), klien belum tahu.
        val fake = FakeTpqRepository(user = FakeData.userAdmin)
        val server = object : TpqRepository by fake {
            override suspend fun simpanAbsensiSantri(santriId: String, tanggal: String, hadir: Boolean, keterangan: String?): AbsensiEntry =
                throw ApiException(ApiError(ErrorCodes.VALIDATION, Jadwal.PESAN_SERVER_LIBUR, 422))
        }
        val vm = admin(server, AbsensiKind.SANTRI, jumat)
        vm.setHadir("s1", false)
        vm.save("s1")
        assertEquals(Jadwal.PESAN_SERVER_LIBUR, vm.state.value.status["s1"]?.error)
        assertFalse(vm.state.value.status["s1"]?.locked == true) // 422 bukan SELF_ATTENDANCE: baris tetap bisa dicoba lagi
    }

    // ---- Rekap ----

    @Test
    fun `rekap hari libur memakai hariBelajar dari server`() {
        val vm = RekapViewModel(FakeTpqRepository(user = FakeData.userAdmin), sabtu)
        val rekap = (vm.state.value.load as Load.Success).data
        assertFalse(rekap.hariBelajar)
        assertEquals(0, rekap.santri.belumDicatat)
        assertTrue(vm.state.value.libur)
    }

    @Test
    fun `rekap hari belajar tidak libur`() {
        val vm = RekapViewModel(FakeTpqRepository(user = FakeData.userAdmin), jumat)
        assertFalse(vm.state.value.libur)
    }

    @Test
    fun `rekap gagal dimuat pada Sabtu tetap menampilkan spanduk dari jadwal lokal`() {
        val vm = RekapViewModel(FakeTpqRepository(user = FakeData.userAdmin, failWith = ApiError.network()), sabtu)
        assertIs<Load.Failure>(vm.state.value.load)
        assertTrue(vm.state.value.libur)
    }

    @Test
    fun `server penentu rekap Sabtu yang dibalas hariBelajar true tidak dianggap libur`() {
        val server = object : TpqRepository by FakeTpqRepository(user = FakeData.userAdmin) {
            override suspend fun rekap(tanggal: String): Rekap = FakeData.rekap("2026-11-02").copy(tanggal = tanggal)
        }
        val vm = RekapViewModel(server, sabtu)
        assertFalse(vm.state.value.libur)
    }

    @Test
    fun `rekap ke hari belajar terakhir menuju Jumat dan memuat rekap hari itu`() {
        val vm = RekapViewModel(FakeTpqRepository(user = FakeData.userAdmin), minggu)
        vm.keHariBelajarTerakhir()
        assertEquals(jumat, vm.state.value.tanggal)
        assertFalse(vm.state.value.libur)
        assertIs<Load.Success<*>>(vm.state.value.load)
    }

    @Test
    fun `rekap libur menurut server pada hari yang menurut jadwal lokal belajar mundur ke hari belajar sebelumnya`() {
        val server = object : TpqRepository by FakeTpqRepository(user = FakeData.userAdmin) {
            // Rabu 4 Nov dianggap libur oleh server.
            override suspend fun rekap(tanggal: String): Rekap = FakeData.rekap(tanggal).copy(hariBelajar = tanggal != "2026-11-04")
        }
        val vm = RekapViewModel(server, LocalDate.of(2026, 11, 4))
        assertTrue(vm.state.value.libur)
        vm.keHariBelajarTerakhir()
        assertEquals(LocalDate.of(2026, 11, 3), vm.state.value.tanggal)
        assertFalse(vm.state.value.libur)
    }

    // ---- Beranda ----

    private fun repoDashboardLibur(user: User): TpqRepository {
        val fake = FakeTpqRepository(user = user, anakIds = setOf("s1"))
        return object : TpqRepository by fake {
            override suspend fun dashboard(tanggal: String?): Dashboard = fake.dashboard("2026-10-31")
        }
    }

    @Test
    fun `beranda memuat dashboard hari libur dengan hariBelajar false`() {
        val vm = BerandaViewModel(repoDashboardLibur(FakeData.userAdmin))
        val d = (vm.state.value.load as Load.Success).data
        assertFalse(d.hariBelajar)
        assertEquals(0, d.ringkasan?.santri?.belumDicatat)
        assertEquals(0, d.ringkasan?.ustadz?.belumDicatat)
        assertTrue(d.ringkasan?.perhatian.orEmpty().none { it.kode.endsWith("BELUM_DICATAT") })

        val wali = BerandaViewModel(repoDashboardLibur(FakeData.userWali))
        assertFalse((wali.state.value.load as Load.Success).data.hariBelajar)
    }
}
