package com.alhikmah.tpq.domain

import com.alhikmah.tpq.domain.menu.MenuAccess
import com.alhikmah.tpq.domain.menu.MenuAccess.READ
import com.alhikmah.tpq.domain.menu.MenuAccess.WRITE
import com.alhikmah.tpq.domain.menu.MenuId
import com.alhikmah.tpq.domain.menu.MenuId.ABSENSI_SANTRI
import com.alhikmah.tpq.domain.menu.MenuId.ABSENSI_USTADZ
import com.alhikmah.tpq.domain.menu.MenuId.ANAK_SAYA
import com.alhikmah.tpq.domain.menu.MenuId.DAFTAR_SANTRI
import com.alhikmah.tpq.domain.menu.MenuId.REKAP_HARIAN
import com.alhikmah.tpq.domain.menu.MenuResolver
import com.alhikmah.tpq.domain.model.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MenuResolverTest {

    private fun menu(vararg roles: Role): Map<MenuId, MenuAccess> =
        MenuResolver.menuFor(roles.toSet()).associate { it.id to it.access }

    @Test
    fun `admin dan tu bisa menulis semua kecuali anak saya`() {
        val expected = mapOf(REKAP_HARIAN to READ, ABSENSI_USTADZ to WRITE, ABSENSI_SANTRI to WRITE, DAFTAR_SANTRI to WRITE)
        assertEquals(expected, menu(Role.ADMIN))
        assertEquals(expected, menu(Role.TU))
    }

    @Test
    fun `pengawas hanya melihat`() {
        assertEquals(
            mapOf(REKAP_HARIAN to READ, ABSENSI_USTADZ to READ, ABSENSI_SANTRI to READ, DAFTAR_SANTRI to READ),
            menu(Role.PENGAWAS),
        )
    }

    @Test
    fun `piket menulis absensi ustadz dan hanya melihat santri`() {
        assertEquals(
            mapOf(REKAP_HARIAN to READ, ABSENSI_USTADZ to WRITE, ABSENSI_SANTRI to READ, DAFTAR_SANTRI to READ),
            menu(Role.PIKET),
        )
    }

    @Test
    fun `ustadz menulis absensi santri dan progress, hanya melihat absensi ustadz`() {
        assertEquals(
            mapOf(REKAP_HARIAN to READ, ABSENSI_USTADZ to READ, ABSENSI_SANTRI to WRITE, DAFTAR_SANTRI to WRITE),
            menu(Role.USTADZ),
        )
    }

    @Test
    fun `wali hanya punya anak saya`() {
        assertEquals(mapOf(ANAK_SAYA to READ), menu(Role.WALI))
    }

    @Test
    fun `multi role menggabungkan menu dan akses tulis menang`() {
        // PIKET + USTADZ: tulis absensi ustadz (dari PIKET) dan tulis absensi santri (dari USTADZ).
        assertEquals(
            mapOf(REKAP_HARIAN to READ, ABSENSI_USTADZ to WRITE, ABSENSI_SANTRI to WRITE, DAFTAR_SANTRI to WRITE),
            menu(Role.PIKET, Role.USTADZ),
        )
        // USTADZ + WALI: menu ustadz ditambah Anak Saya.
        assertEquals(setOf(REKAP_HARIAN, ABSENSI_USTADZ, ABSENSI_SANTRI, DAFTAR_SANTRI, ANAK_SAYA), menu(Role.USTADZ, Role.WALI).keys)
    }

    @Test
    fun `urutan menu tetap dan tanpa role tidak ada menu`() {
        assertEquals(
            listOf(REKAP_HARIAN, ABSENSI_USTADZ, ABSENSI_SANTRI, DAFTAR_SANTRI, ANAK_SAYA),
            MenuResolver.menuFor(Role.entries.toSet()).map { it.id },
        )
        assertTrue(MenuResolver.menuFor(emptySet()).isEmpty())
    }

    @Test
    fun `hak ubah progress hanya admin tu ustadz`() {
        assertTrue(MenuResolver.canEditProgress(setOf(Role.ADMIN)))
        assertTrue(MenuResolver.canEditProgress(setOf(Role.TU)))
        assertTrue(MenuResolver.canEditProgress(setOf(Role.PIKET, Role.USTADZ)))
        assertFalse(MenuResolver.canEditProgress(setOf(Role.PENGAWAS, Role.PIKET, Role.WALI)))
    }
}
