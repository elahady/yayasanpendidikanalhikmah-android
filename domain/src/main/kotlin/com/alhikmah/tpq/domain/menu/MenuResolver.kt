package com.alhikmah.tpq.domain.menu

import com.alhikmah.tpq.domain.model.Role
import com.alhikmah.tpq.domain.model.Role.ADMIN
import com.alhikmah.tpq.domain.model.Role.PENGAWAS
import com.alhikmah.tpq.domain.model.Role.PIKET
import com.alhikmah.tpq.domain.model.Role.TU
import com.alhikmah.tpq.domain.model.Role.USTADZ
import com.alhikmah.tpq.domain.model.Role.WALI

enum class MenuId {
    REKAP_HARIAN,
    ABSENSI_USTADZ,
    ABSENSI_SANTRI,
    DAFTAR_SANTRI,
    ANAK_SAYA,
}

/** WRITE = boleh menulis/mengubah di layar itu; READ = hanya melihat. */
enum class MenuAccess { WRITE, READ }

data class MenuEntry(val id: MenuId, val access: MenuAccess)

/**
 * Aturan role -> menu (tabel "Role" di docs/api-contract.md). Akun multi-role mendapat gabungan menu semua rolenya;
 * bila satu menu terbuka lewat beberapa role, akses WRITE menang atas READ. Server tetap penentu akhir (UI hanya
 * menyembunyikan/menonaktifkan).
 */
object MenuResolver {

    private data class Rule(val id: MenuId, val write: Set<Role>, val read: Set<Role>)

    // Urutan di sini = urutan tampil di beranda.
    private val rules = listOf(
        Rule(MenuId.REKAP_HARIAN, write = emptySet(), read = setOf(PENGAWAS, ADMIN, TU, PIKET, USTADZ)),
        Rule(MenuId.ABSENSI_USTADZ, write = setOf(ADMIN, TU, PIKET), read = setOf(PENGAWAS, USTADZ)),
        Rule(MenuId.ABSENSI_SANTRI, write = setOf(ADMIN, TU, USTADZ), read = setOf(PENGAWAS, PIKET)),
        Rule(MenuId.DAFTAR_SANTRI, write = setOf(ADMIN, TU, USTADZ), read = setOf(PENGAWAS, PIKET)),
        Rule(MenuId.ANAK_SAYA, write = emptySet(), read = setOf(WALI)),
    )

    fun menuFor(roles: Set<Role>): List<MenuEntry> = rules.mapNotNull { rule ->
        when {
            roles.any { it in rule.write } -> MenuEntry(rule.id, MenuAccess.WRITE)
            roles.any { it in rule.read } -> MenuEntry(rule.id, MenuAccess.READ)
            else -> null
        }
    }

    /** Hak mengubah progress santri (PATCH /santri/{id}/progress): ADMIN, TU, USTADZ. */
    fun canEditProgress(roles: Set<Role>): Boolean = roles.any { it == ADMIN || it == TU || it == USTADZ }

    /** ADMIN/TU dikecualikan dari larangan absensi atas nama diri sendiri (server yang menentukan; ini hanya untuk UI). */
    fun mayRecordOwnUstadzAttendance(roles: Set<Role>): Boolean = roles.any { it == ADMIN || it == TU }
}
