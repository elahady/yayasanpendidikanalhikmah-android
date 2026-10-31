package com.alhikmah.tpq.domain.api

import com.alhikmah.tpq.domain.model.AbsensiEntry
import com.alhikmah.tpq.domain.model.Jenjang
import com.alhikmah.tpq.domain.model.Jilid
import com.alhikmah.tpq.domain.model.ProgressUpdate
import com.alhikmah.tpq.domain.model.Rekap
import com.alhikmah.tpq.domain.model.RekapCounts
import com.alhikmah.tpq.domain.model.Role
import com.alhikmah.tpq.domain.model.Santri
import com.alhikmah.tpq.domain.model.User
import com.alhikmah.tpq.domain.model.Ustadz
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Hasil POST /auth/login. */
data class LoginResult(val accessToken: String, val user: User)

/**
 * Konversi JSON <-> model sesuai docs/api-contract.md di repo web. Fungsi parse melempar [ApiException] ber-kode
 * BAD_RESPONSE bila bentuk jawaban tidak sesuai kontrak (field wajib hilang), bukan crash.
 */
object ApiJson {

    const val SUBJECT_SANTRI = "santriId"
    const val SUBJECT_USTADZ = "ustadzId"

    fun parseLogin(body: String): LoginResult = guard {
        val obj = JSONObject(body)
        val token = obj.getString("accessToken")
        require(token.isNotBlank())
        LoginResult(token, user(obj.getJSONObject("user")))
    }

    fun parseUser(body: String): User = guard { user(JSONObject(body)) }

    fun parseSantri(body: String): Santri = guard { santri(JSONObject(body)) }

    fun parseSantriList(body: String): List<Santri> = guard { items(body).map(::santri) }

    fun parseUstadzList(body: String): List<Ustadz> = guard {
        items(body).map { Ustadz(it.getString("id"), it.getString("nama"), it.optStringOrNull("mapelDiajar")) }
    }

    /** [subjectKey] = [SUBJECT_SANTRI] (absensi santri) atau [SUBJECT_USTADZ] (absensi ustadz). */
    fun parseAbsensi(body: String, subjectKey: String): List<AbsensiEntry> = guard { items(body).map { absensi(it, subjectKey) } }

    fun parseAbsensiEntry(body: String, subjectKey: String): AbsensiEntry = guard { absensi(JSONObject(body), subjectKey) }

    fun parseRekap(body: String): Rekap = guard {
        val obj = JSONObject(body)
        Rekap(obj.getString("tanggal"), counts(obj.getJSONObject("santri")), counts(obj.getJSONObject("ustadz")))
    }

    fun progressBody(update: ProgressUpdate): JSONObject = JSONObject().apply {
        update.jilid?.let { put("jilidSaatIni", it.name) }
        if (update.updateCatatan) put("catatanProgressMadin", update.catatan ?: JSONObject.NULL)
    }

    /** Body PUT /absensi-santri atau /absensi-ustadz; [subjectKey] menentukan nama field id. */
    fun absensiBody(subjectKey: String, subjectId: String, tanggal: String, hadir: Boolean, keterangan: String?): JSONObject =
        JSONObject().apply {
            put(subjectKey, subjectId)
            put("tanggal", tanggal)
            put("hadir", hadir)
            put("keterangan", keterangan?.trim()?.takeIf { it.isNotEmpty() } ?: JSONObject.NULL)
        }

    fun loginBody(username: String, password: String): JSONObject =
        JSONObject().put("username", username).put("password", password)

    // ---- internal ----

    private fun user(obj: JSONObject): User {
        val roles = obj.getJSONArray("roles").strings().mapNotNull(Role::fromApi).toSet()
        return User(obj.getString("id"), obj.getString("nama"), roles, obj.optStringOrNull("ustadzId"))
    }

    private fun santri(obj: JSONObject) = Santri(
        id = obj.getString("id"),
        nama = obj.getString("nama"),
        jenjang = Jenjang.fromApi(obj.optStringOrNull("jenjang")),
        kelas = obj.optStringOrNull("kelas"),
        jilidSaatIni = Jilid.fromApi(obj.optStringOrNull("jilidSaatIni")),
        catatanProgressMadin = obj.optStringOrNull("catatanProgressMadin"),
    )

    private fun absensi(obj: JSONObject, subjectKey: String) = AbsensiEntry(
        id = obj.getString("id"),
        subjectId = obj.getString(subjectKey),
        tanggal = obj.getString("tanggal"),
        hadir = obj.getBoolean("hadir"),
        keterangan = obj.optStringOrNull("keterangan"),
        dicatatOleh = obj.optStringOrNull("dicatatOleh"),
        bisaDiubah = obj.optBoolean("bisaDiubah", false),
    )

    private fun counts(obj: JSONObject) = RekapCounts(obj.getInt("hadir"), obj.getInt("total"), obj.getInt("belumDicatat"))

    private fun items(body: String): List<JSONObject> {
        val array = JSONObject(body).getJSONArray("items")
        return (0 until array.length()).map { array.getJSONObject(it) }
    }

    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

    private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

    private inline fun <T> guard(block: () -> T): T = try {
        block()
    } catch (e: JSONException) {
        throw ApiException(ApiError.badResponse(), e)
    } catch (e: IllegalArgumentException) {
        throw ApiException(ApiError.badResponse(), e)
    }
}
