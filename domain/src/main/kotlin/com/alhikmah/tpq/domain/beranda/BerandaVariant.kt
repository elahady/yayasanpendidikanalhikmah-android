package com.alhikmah.tpq.domain.beranda

import com.alhikmah.tpq.domain.model.Role
import com.alhikmah.tpq.domain.model.Role.ADMIN
import com.alhikmah.tpq.domain.model.Role.PENGAWAS
import com.alhikmah.tpq.domain.model.Role.PIKET
import com.alhikmah.tpq.domain.model.Role.TU
import com.alhikmah.tpq.domain.model.Role.USTADZ
import com.alhikmah.tpq.domain.model.Role.WALI

/** Tiga tampilan Beranda (mockup MobAdmin / MobUstadz / MobWali) plus keadaan tanpa role yang dikenal. */
enum class BerandaVariant {
    /** ADMIN, TU, PENGAWAS: cincin kehadiran, perlu perhatian, tangga jilid, pintasan menu. */
    ADMIN,

    /** PIKET dan/atau USTADZ (tanpa ADMIN/TU/PENGAWAS): kartu tugas, ringkasan ustadz, aksi santri. */
    TUGAS,

    /** WALI murni: pilih anak, perjalanan jilid, kehadiran 30 hari, catatan. */
    WALI,

    /** Akun tanpa role yang dikenal app ini. */
    TANPA_AKSES,
}

object BerandaResolver {

    /**
     * Aturan pemilih varian (peran tertinggi menang):
     * 1. ada ADMIN/TU/PENGAWAS -> [BerandaVariant.ADMIN]
     * 2. ada PIKET/USTADZ -> [BerandaVariant.TUGAS]
     * 3. hanya WALI -> [BerandaVariant.WALI]
     * 4. selain itu -> [BerandaVariant.TANPA_AKSES]
     * Akun WALI + role lain mendapat varian role lain ditambah bagian anak (lihat [tampilkanBagianAnak]).
     */
    fun variantFor(roles: Set<Role>): BerandaVariant = when {
        roles.any { it == ADMIN || it == TU || it == PENGAWAS } -> BerandaVariant.ADMIN
        roles.any { it == PIKET || it == USTADZ } -> BerandaVariant.TUGAS
        WALI in roles -> BerandaVariant.WALI
        else -> BerandaVariant.TANPA_AKSES
    }

    /** Bagian anak ditambahkan di bawah varian lain bila server mengirim `anak` (tidak null) dan daftarnya tidak kosong. */
    fun tampilkanBagianAnak(variant: BerandaVariant, anakCount: Int?): Boolean =
        variant != BerandaVariant.WALI && variant != BerandaVariant.TANPA_AKSES && (anakCount ?: 0) > 0
}
