package com.alhikmah.tpq.domain.menu

import com.alhikmah.tpq.domain.model.Role

enum class TabId { BERANDA, ABSENSI, SANTRI, ANAK_SAYA, PROFIL }

/**
 * Tab navigasi bawah, diturunkan dari [MenuResolver] (satu sumber aturan): tab yang menunya tidak boleh diakses
 * role tidak muncul. Urutan: Beranda, Absensi, Santri, Anak saya, Profil.
 *  - ADMIN/TU/PENGAWAS/USTADZ: Beranda, Absensi, Santri, Profil
 *  - PIKET murni: Beranda, Absensi (ustadz), Santri (lihat), Profil
 *  - WALI murni: Beranda, Anak saya, Profil
 *  - WALI + role lain: kelima tab bila menunya ada
 * Tab Absensi membuka layar pilihan (Absensi Santri / Absensi Ustadz / Rekap Harian) sesuai menu yang boleh.
 */
object BottomTabs {
    fun tabsFor(roles: Set<Role>): List<TabId> {
        val menu = MenuResolver.menuFor(roles).map { it.id }.toSet()
        return buildList {
            add(TabId.BERANDA)
            if (MenuId.ABSENSI_SANTRI in menu || MenuId.ABSENSI_USTADZ in menu) add(TabId.ABSENSI)
            if (MenuId.DAFTAR_SANTRI in menu) add(TabId.SANTRI)
            if (MenuId.ANAK_SAYA in menu) add(TabId.ANAK_SAYA)
            add(TabId.PROFIL)
        }
    }

    /** Menu yang ditampilkan di layar tab Absensi (urutan tampil mengikuti [MenuResolver]). */
    fun absensiMenu(roles: Set<Role>): List<MenuEntry> =
        MenuResolver.menuFor(roles).filter { it.id == MenuId.REKAP_HARIAN || it.id == MenuId.ABSENSI_USTADZ || it.id == MenuId.ABSENSI_SANTRI }
}
