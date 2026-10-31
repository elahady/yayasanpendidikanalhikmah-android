package com.alhikmah.tpq.domain.model

/** Role akun menurut kontrak API v1. Satu akun bisa punya lebih dari satu role. */
enum class Role {
    ADMIN,
    TU,
    PENGAWAS,
    PIKET,
    USTADZ,
    WALI,
    ;

    companion object {
        /** Nilai di luar daftar (role baru di server yang belum dikenal app ini) diabaikan, bukan membuat app gagal. */
        fun fromApi(value: String?): Role? = entries.firstOrNull { it.name == value }
    }
}

/** Akun yang sedang masuk (`user` pada /auth/login dan /me). */
data class User(
    val id: String,
    val nama: String,
    val roles: Set<Role>,
    /** Terisi bila akun tertaut ke seorang ustadz; dipakai menyembunyikan diri sendiri dari absensi ustadz. */
    val ustadzId: String?,
    /** Field baru di kontrak (31 Okt 2026): opsional, null bila server lama tidak mengirimnya. */
    val username: String? = null,
    /** Nama ustadz yang tertaut ke akun (null bila tidak tertaut). */
    val ustadzNama: String? = null,
    /** ISO-8601, kapan akun dibuat. */
    val dibuatPada: String? = null,
) {
    fun hasAny(vararg candidates: Role): Boolean = candidates.any { it in roles }
}
