package com.alhikmah.tpq.domain

import com.alhikmah.tpq.domain.beranda.BerandaResolver
import com.alhikmah.tpq.domain.beranda.BerandaVariant
import com.alhikmah.tpq.domain.beranda.JilidJourney
import com.alhikmah.tpq.domain.beranda.StepState
import com.alhikmah.tpq.domain.menu.BottomTabs
import com.alhikmah.tpq.domain.menu.MenuId
import com.alhikmah.tpq.domain.menu.TabId
import com.alhikmah.tpq.domain.model.Jilid
import com.alhikmah.tpq.domain.model.Role
import com.alhikmah.tpq.domain.profil.PasswordForm
import com.alhikmah.tpq.domain.tema.InMemoryThemeStore
import com.alhikmah.tpq.domain.tema.ThemeMode
import com.alhikmah.tpq.domain.tema.ThemeSettings
import com.alhikmah.tpq.domain.util.Inisial
import com.alhikmah.tpq.domain.util.Tanggal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BerandaVariantTest {
    private fun v(vararg roles: Role) = BerandaResolver.variantFor(roles.toSet())

    @Test
    fun `admin tu pengawas memakai varian admin walau punya role lain`() {
        assertEquals(BerandaVariant.ADMIN, v(Role.ADMIN))
        assertEquals(BerandaVariant.ADMIN, v(Role.TU))
        assertEquals(BerandaVariant.ADMIN, v(Role.PENGAWAS))
        assertEquals(BerandaVariant.ADMIN, v(Role.TU, Role.USTADZ, Role.WALI))
        assertEquals(BerandaVariant.ADMIN, v(Role.PIKET, Role.PENGAWAS))
    }

    @Test
    fun `piket dan ustadz memakai varian tugas`() {
        assertEquals(BerandaVariant.TUGAS, v(Role.PIKET))
        assertEquals(BerandaVariant.TUGAS, v(Role.USTADZ))
        assertEquals(BerandaVariant.TUGAS, v(Role.PIKET, Role.USTADZ))
        assertEquals(BerandaVariant.TUGAS, v(Role.USTADZ, Role.WALI))
    }

    @Test
    fun `wali murni memakai varian wali, tanpa role dikenal tanpa akses`() {
        assertEquals(BerandaVariant.WALI, v(Role.WALI))
        assertEquals(BerandaVariant.TANPA_AKSES, v())
    }

    @Test
    fun `bagian anak ditambahkan hanya di varian lain dan bila ada anak`() {
        assertTrue(BerandaResolver.tampilkanBagianAnak(BerandaVariant.TUGAS, 2))
        assertTrue(BerandaResolver.tampilkanBagianAnak(BerandaVariant.ADMIN, 1))
        assertFalse(BerandaResolver.tampilkanBagianAnak(BerandaVariant.TUGAS, 0))
        assertFalse(BerandaResolver.tampilkanBagianAnak(BerandaVariant.TUGAS, null))
        assertFalse(BerandaResolver.tampilkanBagianAnak(BerandaVariant.WALI, 2)) // varian wali sudah memuatnya
    }
}

class BottomTabsTest {
    private fun tabs(vararg roles: Role) = BottomTabs.tabsFor(roles.toSet())

    @Test
    fun `tab sesuai role`() {
        val umum = listOf(TabId.BERANDA, TabId.ABSENSI, TabId.SANTRI, TabId.PROFIL)
        assertEquals(umum, tabs(Role.ADMIN))
        assertEquals(umum, tabs(Role.TU))
        assertEquals(umum, tabs(Role.PENGAWAS))
        assertEquals(umum, tabs(Role.USTADZ))
        assertEquals(umum, tabs(Role.PIKET)) // absensi ustadz + daftar santri (lihat saja)
        assertEquals(listOf(TabId.BERANDA, TabId.ANAK_SAYA, TabId.PROFIL), tabs(Role.WALI))
        assertEquals(listOf(TabId.BERANDA, TabId.ABSENSI, TabId.SANTRI, TabId.ANAK_SAYA, TabId.PROFIL), tabs(Role.USTADZ, Role.WALI))
        assertEquals(listOf(TabId.BERANDA, TabId.PROFIL), tabs())
    }

    @Test
    fun `menu absensi piket murni tanpa absensi santri`() {
        val ids = BottomTabs.absensiMenu(setOf(Role.PIKET)).map { it.id }
        assertEquals(listOf(MenuId.REKAP_HARIAN, MenuId.ABSENSI_USTADZ), ids)
        assertTrue(BottomTabs.absensiMenu(setOf(Role.WALI)).isEmpty())
    }
}

class JilidJourneyTest {
    @Test
    fun `delapan tahap dengan status sebelum sekarang dan sesudah`() {
        val steps = JilidJourney.steps(Jilid.JILID_3)
        assertEquals(8, steps.size)
        assertEquals(listOf("Pra", "J1", "J2", "J3", "J4", "J5", "J6", "Qur'an"), steps.map { it.singkat })
        assertEquals(
            listOf(StepState.SELESAI, StepState.SELESAI, StepState.SELESAI, StepState.SEKARANG, StepState.BELUM, StepState.BELUM, StepState.BELUM, StepState.BELUM),
            steps.map { it.state },
        )
    }

    @Test
    fun `jilid belum dicatat semua belum, al quran semua selesai kecuali terakhir`() {
        assertTrue(JilidJourney.steps(null).all { it.state == StepState.BELUM })
        val akhir = JilidJourney.steps(Jilid.AL_QURAN)
        assertEquals(StepState.SEKARANG, akhir.last().state)
        assertTrue(akhir.dropLast(1).all { it.state == StepState.SELESAI })
    }

    @Test
    fun `tahap berikutnya dan kalimat ringkasan`() {
        assertEquals(Jilid.JILID_4, JilidJourney.next(Jilid.JILID_3))
        assertEquals(Jilid.AL_QURAN, JilidJourney.next(Jilid.JILID_6))
        assertNull(JilidJourney.next(Jilid.AL_QURAN))
        assertNull(JilidJourney.next(null))
        assertEquals("Sekarang di Jilid 3. Tahap berikutnya Jilid 4.", JilidJourney.ringkasan(Jilid.JILID_3))
        assertEquals("Sekarang di Al-Qur'an, tahap akhir An-Nahdhiyah.", JilidJourney.ringkasan(Jilid.AL_QURAN))
        assertEquals("Jilid belum dicatat oleh ustadz.", JilidJourney.ringkasan(null))
    }
}

class PasswordFormTest {
    @Test
    fun `validasi lokal`() {
        assertNull(PasswordForm.validate("lama", "baru12345", "baru12345"))
        assertEquals("Isi password Anda saat ini.", PasswordForm.validate("", "baru12345", "baru12345"))
        assertEquals("Password baru harus 8 sampai 72 karakter.", PasswordForm.validate("lama", "pendek", "pendek"))
        assertEquals("Password baru harus 8 sampai 72 karakter.", PasswordForm.validate("lama", "x".repeat(73), "x".repeat(73)))
        assertEquals("Ulangi password baru dengan isian yang sama.", PasswordForm.validate("lama", "baru12345", "baru12346"))
    }

    @Test
    fun `batas panjang 8 dan 72 diterima`() {
        assertNull(PasswordForm.validate("lama", "x".repeat(8), "x".repeat(8)))
        assertNull(PasswordForm.validate("lama", "x".repeat(72), "x".repeat(72)))
    }
}

class ThemeSettingsTest {
    @Test
    fun `bawaan ikuti sistem bila belum ada pilihan`() {
        assertEquals(ThemeMode.SISTEM, ThemeSettings(InMemoryThemeStore()).mode.value)
    }

    @Test
    fun `pilihan tersimpan dan terbaca lagi di instans baru`() {
        val store = InMemoryThemeStore()
        ThemeSettings(store).set(ThemeMode.GELAP)
        assertEquals("GELAP", store.load())
        assertEquals(ThemeMode.GELAP, ThemeSettings(store).mode.value)
    }

    @Test
    fun `mengubah pilihan memperbarui flow`() {
        val settings = ThemeSettings(InMemoryThemeStore())
        settings.set(ThemeMode.TERANG)
        assertEquals(ThemeMode.TERANG, settings.mode.value)
    }

    @Test
    fun `nilai tersimpan rusak jatuh ke ikuti sistem`() {
        assertEquals(ThemeMode.SISTEM, ThemeSettings(InMemoryThemeStore("UNGU")).mode.value)
        assertEquals(ThemeMode.SISTEM, ThemeMode.fromStored(null))
    }

    @Test
    fun `tema gelap menurut pilihan dan pengaturan sistem`() {
        assertTrue(ThemeMode.SISTEM.gelap(sistemGelap = true))
        assertFalse(ThemeMode.SISTEM.gelap(sistemGelap = false))
        assertFalse(ThemeMode.TERANG.gelap(sistemGelap = true))
        assertTrue(ThemeMode.GELAP.gelap(sistemGelap = false))
    }

    @Test
    fun `label sama dengan mockup`() {
        assertEquals(listOf("Ikuti sistem", "Terang", "Gelap"), ThemeMode.entries.map { it.label })
    }
}

class InisialDanTanggalServerTest {
    @Test
    fun `inisial avatar`() {
        assertEquals("UH", Inisial.dari("Ust. Hasan"))
        assertEquals("AI", Inisial.dari("Aisyah"))
        assertEquals("AN", Inisial.dari("Aisyah Nur Hidayah"))
        assertEquals("?", Inisial.dari("  "))
        assertEquals("X", Inisial.dari("x"))
    }

    @Test
    fun `waktu server UTC menjadi tanggal Jakarta`() {
        // 2026-10-29 20:00 UTC = 2026-10-30 03:00 WIB.
        assertEquals(LocalDate.of(2026, 10, 30), Tanggal.fromServerTime("2026-10-29T20:00:00.000Z"))
        assertEquals("30 Oktober 2026", Tanggal.panjangDariServer("2026-10-29T20:00:00.000Z"))
        assertEquals("27 Oktober 2026", Tanggal.panjangDariServer("2026-10-27"))
        assertNull(Tanggal.panjangDariServer("bukan tanggal"))
        assertNull(Tanggal.panjangDariServer(null))
    }
}
