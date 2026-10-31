package com.alhikmah.tpq.data

import com.alhikmah.tpq.domain.api.ApiJson
import com.alhikmah.tpq.domain.api.AuthApi
import com.alhikmah.tpq.domain.api.LoginResult
import com.alhikmah.tpq.domain.api.TpqRepository
import com.alhikmah.tpq.domain.model.AbsensiEntry
import com.alhikmah.tpq.domain.model.ProgressUpdate
import com.alhikmah.tpq.domain.model.Rekap
import com.alhikmah.tpq.domain.model.Santri
import com.alhikmah.tpq.domain.model.User
import com.alhikmah.tpq.domain.model.Ustadz

/** Implementasi [AuthApi] lewat HTTP (POST /auth/login, GET /me). */
class HttpAuthApi(private val client: ApiClient) : AuthApi {
    override suspend fun login(username: String, password: String): LoginResult =
        ApiJson.parseLogin(client.post("auth", "login", body = ApiJson.loginBody(username, password)))

    override suspend fun me(): User = ApiJson.parseUser(client.get("me"))
}

/** Implementasi [TpqRepository] lewat HTTP, mengikuti docs/api-contract.md. */
class HttpTpqRepository(private val client: ApiClient) : TpqRepository {

    override suspend fun santri(query: String?): List<Santri> =
        ApiJson.parseSantriList(client.get("santri", query = mapOf("q" to query?.trim()?.takeIf { it.isNotEmpty() })))

    override suspend fun santriDetail(id: String): Santri = ApiJson.parseSantri(client.get("santri", id))

    override suspend fun updateProgress(id: String, update: ProgressUpdate): Santri =
        ApiJson.parseSantri(client.patch("santri", id, "progress", body = ApiJson.progressBody(update)))

    override suspend fun ustadz(): List<Ustadz> = ApiJson.parseUstadzList(client.get("ustadz"))

    override suspend fun absensiSantriByTanggal(tanggal: String): List<AbsensiEntry> =
        ApiJson.parseAbsensi(client.get("absensi-santri", query = mapOf("tanggal" to tanggal)), ApiJson.SUBJECT_SANTRI)

    override suspend fun absensiSantriRiwayat(santriId: String, dari: String, sampai: String): List<AbsensiEntry> =
        ApiJson.parseAbsensi(
            client.get("absensi-santri", query = mapOf("santriId" to santriId, "dari" to dari, "sampai" to sampai)),
            ApiJson.SUBJECT_SANTRI,
        )

    override suspend fun simpanAbsensiSantri(santriId: String, tanggal: String, hadir: Boolean, keterangan: String?): AbsensiEntry =
        ApiJson.parseAbsensiEntry(
            client.put("absensi-santri", body = ApiJson.absensiBody(ApiJson.SUBJECT_SANTRI, santriId, tanggal, hadir, keterangan)),
            ApiJson.SUBJECT_SANTRI,
        )

    override suspend fun absensiUstadzByTanggal(tanggal: String): List<AbsensiEntry> =
        ApiJson.parseAbsensi(client.get("absensi-ustadz", query = mapOf("tanggal" to tanggal)), ApiJson.SUBJECT_USTADZ)

    override suspend fun simpanAbsensiUstadz(ustadzId: String, tanggal: String, hadir: Boolean, keterangan: String?): AbsensiEntry =
        ApiJson.parseAbsensiEntry(
            client.put("absensi-ustadz", body = ApiJson.absensiBody(ApiJson.SUBJECT_USTADZ, ustadzId, tanggal, hadir, keterangan)),
            ApiJson.SUBJECT_USTADZ,
        )

    override suspend fun rekap(tanggal: String): Rekap = ApiJson.parseRekap(client.get("rekap", query = mapOf("tanggal" to tanggal)))
}
