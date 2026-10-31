package com.alhikmah.tpq.ui

import com.alhikmah.tpq.domain.api.ApiError
import com.alhikmah.tpq.domain.fake.FakeData
import com.alhikmah.tpq.domain.fake.FakeTpqRepository
import com.alhikmah.tpq.domain.model.Jilid
import com.alhikmah.tpq.domain.model.User
import com.alhikmah.tpq.domain.util.Load
import com.alhikmah.tpq.domain.util.Tanggal
import com.alhikmah.tpq.ui.absensi.AbsensiKind
import com.alhikmah.tpq.ui.absensi.AbsensiViewModel
import com.alhikmah.tpq.ui.absensi.isEditable
import com.alhikmah.tpq.ui.absensi.needsSave
import com.alhikmah.tpq.ui.santri.SantriDetailViewModel
import com.alhikmah.tpq.ui.santri.hasChanges
import com.alhikmah.tpq.ui.santri.jilidSelected
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

@OptIn(ExperimentalCoroutinesApi::class)
class AbsensiViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    // Senin tetap: tes tidak boleh bergantung pada hari tes dijalankan (Sabtu dan Minggu libur, fake menolak 422).
    private val senin = LocalDate.of(2026, 11, 2)

    private fun rows(vm: AbsensiViewModel) =(vm.state.value.load as Load.Success).data

    @Test
    fun `memuat roster santri dan menandai belum dicatat`() {
        val vm = AbsensiViewModel(FakeTpqRepository(user = FakeData.userAdmin), FakeData.userAdmin, AbsensiKind.SANTRI, senin)
        assertTrue(vm.state.value.canWrite)
        assertEquals(FakeData.santri.size, rows(vm).size)
        assertTrue(rows(vm).all { it.belumDicatat })
    }

    @Test
    fun `simpan satu baris membuat entri dan membersihkan draft`() {
        val vm = AbsensiViewModel(FakeTpqRepository(user = FakeData.userAdmin), FakeData.userAdmin, AbsensiKind.SANTRI, senin)
        vm.setHadir("s1", false)
        vm.setKeterangan("s1", "Sakit")
        assertEquals(false, vm.state.value.drafts["s1"]?.hadir)
        vm.save("s1")

        val row = rows(vm).first { it.person.id == "s1" }
        val entry = assertNotNull(row.entry)
        assertFalse(entry.hadir)
        assertEquals("Sakit", entry.keterangan)
        assertTrue(vm.state.value.drafts.isEmpty())
        assertFalse(vm.state.value.needsSave(row)) // sudah tersimpan, tak perlu simpan lagi
        // baris lain tidak tersentuh
        assertTrue(rows(vm).filter { it.person.id != "s1" }.all { it.belumDicatat })
    }

    @Test
    fun `entri yang ada hanya perlu simpan setelah diubah`() {
        val seeded = FakeData.entry("s1", Tanggal.toApi(senin), hadir = true)
        val vm = AbsensiViewModel(FakeTpqRepository(user = FakeData.userAdmin, seedSantri = listOf(seeded)), FakeData.userAdmin, AbsensiKind.SANTRI, senin)
        val row = rows(vm).first { it.person.id == "s1" }
        assertFalse(vm.state.value.needsSave(row))
        vm.setHadir("s1", false)
        assertTrue(vm.state.value.needsSave(row))
        vm.setHadir("s1", true) // kembali ke nilai semula
        assertFalse(vm.state.value.needsSave(row))
    }

    @Test
    fun `entri yang dikunci server dan akses hanya-lihat tidak bisa diubah`() {
        val seeded = FakeData.entry("s1", Tanggal.toApi(senin), hadir = true, bisaDiubah = false)
        // USTADZ biasa: entri dicatat orang lain -> dikunci server.
        val ustadz = User("x", "Ustadz Lain", setOf(com.alhikmah.tpq.domain.model.Role.USTADZ), "u2")
        val vm = AbsensiViewModel(FakeTpqRepository(user = ustadz, seedSantri = listOf(seeded)), ustadz, AbsensiKind.SANTRI, senin)
        val locked = rows(vm).first { it.person.id == "s1" }
        assertFalse(vm.state.value.isEditable(locked))
        assertTrue(vm.state.value.isEditable(rows(vm).first { it.person.id == "s2" }))
        vm.setHadir("s1", false)
        vm.save("s1")
        assertEquals(true, rows(vm).first { it.person.id == "s1" }.entry?.hadir)

        // PENGAWAS: hanya lihat.
        val pengawas = FakeData.userPengawas
        val ro = AbsensiViewModel(FakeTpqRepository(user = pengawas), pengawas, AbsensiKind.SANTRI, senin)
        assertFalse(ro.state.value.canWrite)
        assertFalse(ro.state.value.isEditable(rows(ro).first()))
    }

    @Test
    fun `ustadz yang tertaut ke akun disembunyikan dari absensi ustadz`() {
        val me = FakeData.userPiketUstadz // ustadzId = u3
        val vm = AbsensiViewModel(FakeTpqRepository(user = me), me, AbsensiKind.USTADZ, senin)
        assertTrue(vm.state.value.selfHidden)
        assertEquals(listOf("u1", "u2"), rows(vm).map { it.person.id }.sorted())
        assertTrue(vm.state.value.canWrite) // PIKET menulis absensi ustadz
    }

    @Test
    fun `SELF_ATTENDANCE_FORBIDDEN dari server mengunci baris dan menampilkan pesannya`() {
        // Server menganggap akun terhubung ke u3, tetapi klien belum tahu (ustadzId null) sehingga barisnya tampil.
        val serverUser = FakeData.userPiketUstadz
        val clientUser = serverUser.copy(ustadzId = null)
        val vm = AbsensiViewModel(FakeTpqRepository(user = serverUser), clientUser, AbsensiKind.USTADZ, senin)
        assertTrue(rows(vm).any { it.person.id == "u3" })
        vm.save("u3")
        val status = vm.state.value.status["u3"]
        assertNotNull(status)
        assertTrue(status.locked)
        assertEquals("Anda tidak boleh mengisi absensi atas nama diri sendiri.", status.error)
        assertFalse(vm.state.value.isEditable(rows(vm).first { it.person.id == "u3" }))
        assertNull(rows(vm).first { it.person.id == "u3" }.entry)
        // ustadz lain tetap bisa disimpan
        vm.save("u1")
        assertNotNull(rows(vm).first { it.person.id == "u1" }.entry)
    }

    @Test
    fun `gagal memuat menampilkan error lalu pulih saat dicoba lagi`() {
        val repo = FakeTpqRepository(user = FakeData.userAdmin, failWith = ApiError.network())
        val vm = AbsensiViewModel(repo, FakeData.userAdmin, AbsensiKind.SANTRI, senin)
        val failure = assertIs<Load.Failure>(vm.state.value.load)
        assertTrue(failure.error.isConnectivity)
        repo.failWith = null
        vm.load()
        assertIs<Load.Success<*>>(vm.state.value.load)
    }

    @Test
    fun `ganti tanggal memuat ulang dan membuang draft`() {
        val vm = AbsensiViewModel(FakeTpqRepository(user = FakeData.userAdmin), FakeData.userAdmin, AbsensiKind.SANTRI, senin)
        vm.setHadir("s1", false)
        vm.setTanggal(senin.plusDays(1))
        assertTrue(vm.state.value.drafts.isEmpty())
        assertEquals(senin.plusDays(1), vm.state.value.tanggal)
    }

    @Test
    fun `progress santri diubah lalu disimpan`() {
        val vm = SantriDetailViewModel(FakeTpqRepository(user = FakeData.userAdmin), "s1", canEdit = true)
        assertEquals(Jilid.JILID_4, vm.state.value.jilidSelected)
        assertFalse(vm.state.value.hasChanges)
        vm.onJilid(Jilid.JILID_5)
        vm.onCatatan("  Fiqih bab shalat ")
        assertTrue(vm.state.value.hasChanges)
        vm.save()
        val santri = (vm.state.value.santri as Load.Success).data
        assertEquals(Jilid.JILID_5, santri.jilidSaatIni)
        assertEquals("Fiqih bab shalat", santri.catatanProgressMadin)
        assertFalse(vm.state.value.hasChanges)
        assertTrue(vm.state.value.saved)
    }

    @Test
    fun `detail santri untuk piket murni tidak memuat riwayat absensi`() {
        val piket = FakeData.userPiketUstadz.copy(roles = setOf(com.alhikmah.tpq.domain.model.Role.PIKET))
        val vm = SantriDetailViewModel(FakeTpqRepository(user = piket), "s1", canEdit = false, canViewRiwayat = false)
        assertFalse(vm.state.value.canViewRiwayat)
        assertIs<Load.Success<*>>(vm.state.value.santri) // progress tetap tampil
        assertEquals(emptyList(), (vm.state.value.riwayat as Load.Success).data)
    }

    @Test
    fun `wali tidak bisa menyimpan progress dan kegagalan simpan menampilkan pesan`() {
        val wali = SantriDetailViewModel(FakeTpqRepository(user = FakeData.userWali), "s1", canEdit = false)
        wali.onJilid(Jilid.JILID_6)
        wali.save()
        assertEquals(Jilid.JILID_4, (wali.state.value.santri as Load.Success).data.jilidSaatIni)

        // Server menolak (mis. role dicabut): pesan tampil, edit dipertahankan.
        val repo = FakeTpqRepository(user = FakeData.userPengawas)
        val vm = SantriDetailViewModel(repo, "s1", canEdit = true)
        vm.onJilid(Jilid.JILID_2)
        vm.save()
        assertEquals("Anda tidak punya izin untuk tindakan ini.", vm.state.value.saveError)
        assertEquals(Jilid.JILID_2, vm.state.value.jilidSelected)
    }
}
