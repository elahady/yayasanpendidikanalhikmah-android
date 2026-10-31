package com.alhikmah.tpq.domain

import com.alhikmah.tpq.domain.api.ApiException
import com.alhikmah.tpq.domain.api.ErrorCodes
import com.alhikmah.tpq.domain.fake.FakeData
import com.alhikmah.tpq.domain.fake.FakeTpqRepository
import com.alhikmah.tpq.domain.model.Jilid
import com.alhikmah.tpq.domain.model.ProgressUpdate
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Menjaga fake tetap setia pada aturan kontrak, karena preview dan tes lain bergantung padanya. */
class FakeTpqRepositoryTest {

    private val tanggal = "2026-11-02"

    @Test
    fun `upsert absensi santri lalu terbaca di tanggal itu`() = runTest {
        val repo = FakeTpqRepository(user = FakeData.userAdmin)
        val saved = repo.simpanAbsensiSantri("s1", tanggal, hadir = false, keterangan = " Izin ")
        assertEquals("Izin", saved.keterangan)
        repo.simpanAbsensiSantri("s1", tanggal, hadir = true, keterangan = null) // upsert, bukan entri kedua
        val entries = repo.absensiSantriByTanggal(tanggal)
        assertEquals(1, entries.size)
        assertTrue(entries.single().hadir)
    }

    @Test
    fun `piket tidak boleh mengabsen ustadz dirinya sendiri`() = runTest {
        val repo = FakeTpqRepository(user = FakeData.userPiketUstadz)
        val e = assertFailsWith<ApiException> { repo.simpanAbsensiUstadz("u3", tanggal, true, null) }
        assertEquals(ErrorCodes.SELF_ATTENDANCE_FORBIDDEN, e.error.code)
        repo.simpanAbsensiUstadz("u1", tanggal, true, null) // ustadz lain boleh
    }

    @Test
    fun `admin dikecualikan dari larangan absensi diri sendiri`() = runTest {
        val admin = FakeData.userAdmin.copy(ustadzId = "u1")
        FakeTpqRepository(user = admin).simpanAbsensiUstadz("u1", tanggal, true, null)
    }

    @Test
    fun `entri orang lain tidak bisa diubah ustadz biasa tapi bisa oleh admin`() = runTest {
        val seeded = FakeData.entry("s1", tanggal, true) // dicatat "Pak Tata Usaha"
        val ustadz = FakeTpqRepository(user = FakeData.userPiketUstadz, seedSantri = listOf(seeded))
        assertFalse(ustadz.absensiSantriByTanggal(tanggal).single().bisaDiubah)
        assertEquals(ErrorCodes.FORBIDDEN, assertFailsWith<ApiException> { ustadz.simpanAbsensiSantri("s1", tanggal, false, null) }.error.code)

        val admin = FakeTpqRepository(user = FakeData.userAdmin, seedSantri = listOf(seeded))
        assertTrue(admin.absensiSantriByTanggal(tanggal).single().bisaDiubah)
    }

    @Test
    fun `wali dan pengawas tidak boleh menulis`() = runTest {
        assertEquals(
            ErrorCodes.FORBIDDEN,
            assertFailsWith<ApiException> { FakeTpqRepository(user = FakeData.userWali).simpanAbsensiSantri("s1", tanggal, true, null) }.error.code,
        )
        assertEquals(
            ErrorCodes.FORBIDDEN,
            assertFailsWith<ApiException> { FakeTpqRepository(user = FakeData.userPengawas).updateProgress("s1", ProgressUpdate(Jilid.JILID_2)) }.error.code,
        )
    }

    @Test
    fun `update progress mengubah jilid dan boleh menghapus catatan`() = runTest {
        val repo = FakeTpqRepository(user = FakeData.userAdmin)
        val updated = repo.updateProgress("s1", ProgressUpdate(jilid = Jilid.JILID_5, updateCatatan = true, catatan = null))
        assertEquals(Jilid.JILID_5, updated.jilidSaatIni)
        assertNull(updated.catatanProgressMadin)
    }

    @Test
    fun `rekap menghitung belum dicatat`() = runTest {
        val repo = FakeTpqRepository(user = FakeData.userAdmin)
        repo.simpanAbsensiSantri("s1", tanggal, true, null)
        val rekap = repo.rekap(tanggal)
        assertEquals(1, rekap.santri.hadir)
        assertEquals(FakeData.santri.size - 1, rekap.santri.belumDicatat)
    }
}
