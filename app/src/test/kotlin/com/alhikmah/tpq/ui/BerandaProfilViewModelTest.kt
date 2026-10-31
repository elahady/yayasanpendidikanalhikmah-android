package com.alhikmah.tpq.ui

import com.alhikmah.tpq.domain.api.ApiError
import com.alhikmah.tpq.domain.fake.FakeData
import com.alhikmah.tpq.domain.fake.FakeTpqRepository
import com.alhikmah.tpq.domain.util.Load
import com.alhikmah.tpq.ui.beranda.BerandaViewModel
import com.alhikmah.tpq.ui.profil.ProfilViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
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
class BerandaViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `memuat dashboard saat dibuat`() {
        val vm = BerandaViewModel(FakeTpqRepository(user = FakeData.userAdmin))
        val load = assertIs<Load.Success<*>>(vm.state.value.load)
        assertNotNull(load.data)
        assertFalse(vm.state.value.refreshing)
    }

    @Test
    fun `galat menampilkan pesan lalu Coba lagi berhasil`() {
        val repo = FakeTpqRepository(user = FakeData.userAdmin, failWith = ApiError.network())
        val vm = BerandaViewModel(repo)
        val gagal = assertIs<Load.Failure>(vm.state.value.load)
        assertEquals(ApiError.network().message, gagal.error.message)

        repo.failWith = null
        vm.muat()
        assertIs<Load.Success<*>>(vm.state.value.load)
    }

    @Test
    fun `segarkan menjaga data lama bila gagal dan menyimpan pesan`() {
        val repo = FakeTpqRepository(user = FakeData.userAdmin)
        val vm = BerandaViewModel(repo)
        val awal = vm.state.value.load
        repo.failWith = ApiError.timeout()
        vm.segarkan()
        assertEquals(awal, vm.state.value.load) // data lama tetap
        assertFalse(vm.state.value.refreshing)
        assertEquals(ApiError.timeout().message, vm.state.value.refreshError)

        vm.tutupGalatSegar()
        assertNull(vm.state.value.refreshError)

        repo.failWith = null
        vm.segarkan()
        assertNull(vm.state.value.refreshError)
        assertIs<Load.Success<*>>(vm.state.value.load)
    }

    @Test
    fun `segarkan setelah gagal memuat sama dengan muat ulang penuh`() {
        val repo = FakeTpqRepository(user = FakeData.userAdmin, failWith = ApiError.network())
        val vm = BerandaViewModel(repo)
        repo.failWith = null
        vm.segarkan()
        assertIs<Load.Success<*>>(vm.state.value.load)
    }

    @Test
    fun `pilih anak disimpan di state`() {
        val vm = BerandaViewModel(FakeTpqRepository(user = FakeData.userWali, anakIds = setOf("s1", "s2")))
        assertNull(vm.state.value.anakTerpilih)
        vm.pilihAnak("s2")
        assertEquals("s2", vm.state.value.anakTerpilih)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProfilViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(repo: FakeTpqRepository = FakeTpqRepository()) = ProfilViewModel(repo).also { it.bukaUbahPassword() }

    private fun ProfilViewModel.isi(lama: String, baru: String, ulangi: String) {
        onLama(lama)
        onBaru(baru)
        onUlangi(ulangi)
    }

    @Test
    fun `dialog dibuka bersih dan ditutup menghapus isian`() {
        val vm = vm()
        assertTrue(vm.state.value.ubahPassword.tampil)
        vm.isi("a", "b", "c")
        vm.tutupUbahPassword()
        assertFalse(vm.state.value.ubahPassword.tampil)
        assertEquals("", vm.state.value.ubahPassword.lama)
        assertEquals("", vm.state.value.ubahPassword.baru)
    }

    @Test
    fun `validasi lokal menolak password pendek dan tidak cocok`() {
        val vm = vm()
        vm.isi(FakeData.PASSWORD, "pendek", "pendek")
        vm.kirim()
        assertEquals("Password baru harus 8 sampai 72 karakter.", vm.state.value.ubahPassword.galat)
        assertFalse(vm.state.value.ubahPassword.menyimpan)

        vm.isi(FakeData.PASSWORD, "passwordbaru1", "passwordbaru2")
        vm.kirim()
        assertEquals("Ulangi password baru dengan isian yang sama.", vm.state.value.ubahPassword.galat)
    }

    @Test
    fun `mengetik menghapus galat sebelumnya`() {
        val vm = vm()
        vm.isi("", "", "")
        vm.kirim()
        assertNotNull(vm.state.value.ubahPassword.galat)
        vm.onLama("x")
        assertNull(vm.state.value.ubahPassword.galat)
    }

    @Test
    fun `password lama salah menampilkan pesan server dan dialog tetap terbuka`() {
        val vm = vm()
        vm.isi("salahsekali", "passwordbaru1", "passwordbaru1")
        vm.kirim()
        val s = vm.state.value
        assertTrue(s.ubahPassword.tampil)
        assertEquals("Password sekarang salah.", s.ubahPassword.galat)
        assertFalse(s.ubahPassword.menyimpan)
        assertNull(s.pesan)
        assertEquals("passwordbaru1", s.ubahPassword.baru) // isian tidak hilang, pengguna cukup memperbaiki
    }

    @Test
    fun `berhasil menutup dialog, mengosongkan isian, dan memberi pesan sekali tampil`() {
        val vm = vm()
        vm.isi(FakeData.PASSWORD, "passwordbaru1", "passwordbaru1")
        vm.kirim()
        val s = vm.state.value
        assertFalse(s.ubahPassword.tampil)
        assertEquals("", s.ubahPassword.baru)
        assertEquals("Password berhasil diubah.", s.pesan)
        vm.pesanDibaca()
        assertNull(vm.state.value.pesan)
    }

    @Test
    fun `batas percobaan 429 ditampilkan dari server`() {
        val repo = FakeTpqRepository()
        val vm = vm(repo)
        repeat(5) {
            vm.isi("salahsekali", "passwordbaru1", "passwordbaru1")
            vm.kirim()
        }
        vm.isi(FakeData.PASSWORD, "passwordbaru1", "passwordbaru1")
        vm.kirim()
        val galat = assertNotNull(vm.state.value.ubahPassword.galat)
        assertTrue(galat.endsWith("Coba lagi dalam 15 menit."), galat)
    }

    @Test
    fun `galat jaringan ditampilkan apa adanya`() {
        val vm = vm(FakeTpqRepository(failWith = ApiError.network()))
        vm.isi(FakeData.PASSWORD, "passwordbaru1", "passwordbaru1")
        vm.kirim()
        assertEquals(ApiError.network().message, vm.state.value.ubahPassword.galat)
    }
}
