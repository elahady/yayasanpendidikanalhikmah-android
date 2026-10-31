package com.alhikmah.tpq.domain.api

import com.alhikmah.tpq.domain.model.AbsensiEntry
import com.alhikmah.tpq.domain.model.Aktivitas
import com.alhikmah.tpq.domain.model.AksiPerhatian
import com.alhikmah.tpq.domain.model.AnakDashboard
import com.alhikmah.tpq.domain.model.Dashboard
import com.alhikmah.tpq.domain.model.HariKehadiran
import com.alhikmah.tpq.domain.model.KehadiranAnak
import com.alhikmah.tpq.domain.model.Perhatian
import com.alhikmah.tpq.domain.model.Ringkasan
import com.alhikmah.tpq.domain.model.SantriRingkas
import com.alhikmah.tpq.domain.model.StatusHadir
import com.alhikmah.tpq.domain.model.TrenHarian
import com.alhikmah.tpq.domain.model.UstadzHariIni
import com.alhikmah.tpq.domain.model.UstadzRingkas
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
        // `hariBelajar` baru (31 Okt 2026): server lama tidak mengirimnya, jadi dianggap hari belajar.
        Rekap(obj.getString("tanggal"), counts(obj.getJSONObject("santri")), counts(obj.getJSONObject("ustadz")), obj.optBoolean("hariBelajar", true))
    }

    /**
     * `GET /dashboard`. Field inti (angka santri/ustadz, id/nama anak) wajib; tren/perhatian/aktivitas yang hilang
     * dianggap kosong, dan kunci jilid/jenjang yang tak ada dianggap 0 supaya server yang sedikit berbeda tidak membuat layar gagal.
     */
    fun parseDashboard(body: String): Dashboard = guard {
        val obj = JSONObject(body)
        Dashboard(
            tanggal = obj.getString("tanggal"),
            ringkasan = if (obj.isNull("ringkasan")) null else ringkasan(obj.getJSONObject("ringkasan")),
            anak = if (obj.isNull("anak")) null else objects(obj.getJSONArray("anak")).map(::anak),
            // `hariBelajar` baru (31 Okt 2026): server lama tidak mengirimnya, jadi dianggap hari belajar.
            hariBelajar = obj.optBoolean("hariBelajar", true),
        )
    }

    /** Body `POST /me/password`. */
    fun passwordBody(passwordLama: String, passwordBaru: String): JSONObject =
        JSONObject().put("passwordLama", passwordLama).put("passwordBaru", passwordBaru)

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
        return User(
            id = obj.getString("id"),
            nama = obj.getString("nama"),
            roles = roles,
            ustadzId = obj.optStringOrNull("ustadzId"),
            // Tiga field di bawah baru (31 Okt 2026): server lama tidak mengirimnya, jadi opsional.
            username = obj.optStringOrNull("username"),
            ustadzNama = obj.optStringOrNull("ustadzNama"),
            dibuatPada = obj.optStringOrNull("dibuatPada"),
        )
    }

    private fun ringkasan(obj: JSONObject): Ringkasan {
        val santri = obj.getJSONObject("santri")
        val ustadz = obj.getJSONObject("ustadz")
        val perJenjang = santri.optJSONObject("perJenjang")
        val perJilid = santri.optJSONObject("perJilid")
        return Ringkasan(
            santri = SantriRingkas(
                total = santri.getInt("total"),
                hadir = santri.getInt("hadir"),
                tidakHadir = santri.getInt("tidakHadir"),
                belumDicatat = santri.getInt("belumDicatat"),
                perJenjang = Jenjang.entries.associateWith { perJenjang?.optInt(it.name, 0) ?: 0 },
                perJilid = Jilid.entries.associateWith { perJilid?.optInt(it.name, 0) ?: 0 },
            ),
            ustadz = UstadzRingkas(
                total = ustadz.getInt("total"),
                hadir = ustadz.getInt("hadir"),
                tidakHadir = ustadz.getInt("tidakHadir"),
                belumDicatat = ustadz.getInt("belumDicatat"),
                // `daftar` baru (31 Okt 2026): server lama tidak mengirimnya, jadi dianggap kosong.
                daftar = optObjects(ustadz, "daftar").map {
                    UstadzHariIni(
                        id = it.getString("id"),
                        nama = it.getString("nama"),
                        mapelDiajar = it.optStringOrNull("mapelDiajar"),
                        status = StatusHadir.fromApi(it.optStringOrNull("status")),
                    )
                },
            ),
            tren = optObjects(obj, "tren").map { TrenHarian(it.getString("tanggal"), it.getInt("hadir"), it.getInt("tercatat")) },
            perhatian = optObjects(obj, "perhatian").map {
                Perhatian(
                    kode = it.getString("kode"),
                    jumlah = it.optInt("jumlah", 0),
                    judul = it.getString("judul"),
                    keterangan = it.optStringOrNull("keterangan"),
                    aksi = AksiPerhatian.fromApi(it.optStringOrNull("aksi")),
                )
            },
            aktivitas = optObjects(obj, "aktivitas").map {
                Aktivitas(it.getString("waktu"), it.getString("oleh"), it.getString("jenis"), it.getString("ringkas"))
            },
        )
    }

    private fun anak(obj: JSONObject): AnakDashboard {
        val kehadiran = obj.getJSONObject("kehadiran")
        return AnakDashboard(
            santri = santri(obj.getJSONObject("santri")),
            diperbaruiPada = obj.optStringOrNull("diperbaruiPada"),
            kehadiran = KehadiranAnak(
                hadir = kehadiran.getInt("hadir"),
                tidakHadir = kehadiran.getInt("tidakHadir"),
                belumDicatat = kehadiran.getInt("belumDicatat"),
                hari = optObjects(kehadiran, "hari").map { HariKehadiran(it.getString("tanggal"), StatusHadir.fromApi(it.optStringOrNull("status"))) },
            ),
        )
    }

    private fun objects(array: JSONArray): List<JSONObject> = (0 until array.length()).map { array.getJSONObject(it) }

    private fun optObjects(obj: JSONObject, key: String): List<JSONObject> = obj.optJSONArray(key)?.let(::objects) ?: emptyList()

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
