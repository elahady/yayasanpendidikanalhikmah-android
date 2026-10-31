package com.alhikmah.tpq.domain.api

import com.alhikmah.tpq.domain.model.AbsensiEntry
import com.alhikmah.tpq.domain.model.Dashboard
import com.alhikmah.tpq.domain.model.ProgressUpdate
import com.alhikmah.tpq.domain.model.Rekap
import com.alhikmah.tpq.domain.model.Santri
import com.alhikmah.tpq.domain.model.User
import com.alhikmah.tpq.domain.model.Ustadz

/**
 * Kontrak akses data ke server (sesuai docs/api-contract.md). Semua fungsi suspend dan melempar [ApiException]
 * bila gagal. Implementasi nyata ada di :app (HTTP); [com.alhikmah.tpq.domain.fake.FakeTpqRepository] untuk preview dan tes.
 */
interface AuthApi {
    suspend fun login(username: String, password: String): LoginResult

    suspend fun me(): User
}

interface TpqRepository {
    suspend fun santri(query: String? = null): List<Santri>

    suspend fun santriDetail(id: String): Santri

    suspend fun updateProgress(id: String, update: ProgressUpdate): Santri

    suspend fun ustadz(): List<Ustadz>

    suspend fun absensiSantriByTanggal(tanggal: String): List<AbsensiEntry>

    suspend fun absensiSantriRiwayat(santriId: String, dari: String, sampai: String): List<AbsensiEntry>

    suspend fun simpanAbsensiSantri(santriId: String, tanggal: String, hadir: Boolean, keterangan: String?): AbsensiEntry

    suspend fun absensiUstadzByTanggal(tanggal: String): List<AbsensiEntry>

    suspend fun simpanAbsensiUstadz(ustadzId: String, tanggal: String, hadir: Boolean, keterangan: String?): AbsensiEntry

    suspend fun rekap(tanggal: String): Rekap

    /**
     * `GET /dashboard`. [tanggal] null = hari ini menurut server (Asia/Jakarta), jadi tidak bergantung jam/zona HP.
     * Hasilnya sudah menyesuaikan role akun (ringkasan untuk non-WALI, anak untuk WALI).
     */
    suspend fun dashboard(tanggal: String? = null): Dashboard

    /**
     * `POST /me/password`. Gagal dengan [ApiException] ber-kode VALIDATION (422) bila password lama salah atau password
     * baru tak memenuhi aturan (pesan server siap tampil; sengaja bukan 401 sehingga tidak memicu logout), atau RATE_LIMITED (429).
     */
    suspend fun ubahPassword(passwordLama: String, passwordBaru: String)
}

/** Penyimpanan token akses (di Android: dienkripsi dengan Android Keystore). Dipanggil dari thread mana pun. */
interface TokenStore {
    fun get(): String?

    fun save(token: String)

    fun clear()
}
