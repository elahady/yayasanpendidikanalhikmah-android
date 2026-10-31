package com.alhikmah.tpq.domain.fake

import com.alhikmah.tpq.domain.api.ApiError
import com.alhikmah.tpq.domain.api.ApiException
import com.alhikmah.tpq.domain.api.AuthApi
import com.alhikmah.tpq.domain.api.ErrorCodes
import com.alhikmah.tpq.domain.api.LoginResult
import com.alhikmah.tpq.domain.api.TokenStore
import com.alhikmah.tpq.domain.api.TpqRepository
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
import com.alhikmah.tpq.domain.util.Tanggal
import java.time.LocalDate

/** Contoh data untuk Compose preview dan unit test. */
object FakeData {
    val santri = listOf(
        Santri("s1", "Aisyah Putri", Jenjang.ULA, "3", Jilid.JILID_4, "Hafal Juz 30 sampai An-Naba"),
        Santri("s2", "Budi Santoso", Jenjang.ULA, "1", Jilid.JILID_1, null),
        Santri("s3", "Citra Lestari", Jenjang.WUSTHO, "2", Jilid.AL_QURAN, "Fiqih: bab thaharah"),
        Santri("s4", "Dimas Pratama", Jenjang.ULA, "2", Jilid.PRA_NAHDLIYAH, null),
        Santri("s5", "Elok Maharani", Jenjang.WUSTHO, "1", Jilid.JILID_6, "Aqidah: rukun iman"),
    )

    val ustadz = listOf(
        Ustadz("u1", "Ustadz Ahmad Fauzi", "Tahfidz"),
        Ustadz("u2", "Ustadzah Siti Nurhaliza", "Al-Qur'an & Tajwid"),
        Ustadz("u3", "Ustadz Hasan Basri", "Fiqih"),
    )

    val userAdmin = User("a1", "Pak Tata Usaha", setOf(Role.TU), null)
    val userPiketUstadz = User("a2", "Ustadz Hasan", setOf(Role.PIKET, Role.USTADZ), "u3")
    val userPengawas = User("a3", "Bu Pengawas", setOf(Role.PENGAWAS), null)
    val userWali = User("a4", "Bapak Wali Aisyah", setOf(Role.WALI), null)

    fun entry(subjectId: String, tanggal: String, hadir: Boolean, keterangan: String? = null, bisaDiubah: Boolean = true) =
        AbsensiEntry("e-$subjectId-$tanggal", subjectId, tanggal, hadir, keterangan, "Pak Tata Usaha", bisaDiubah)

    fun rekap(tanggal: String) = Rekap(tanggal, RekapCounts(40, 45, 2), RekapCounts(5, 6, 0))
}

/**
 * Repository palsu di memori yang meniru aturan server seperlunya: hak tulis per role, `bisaDiubah`, dan larangan
 * absensi atas nama diri sendiri. [failWith] memaksa semua panggilan gagal (untuk menguji/preview state error).
 */
class FakeTpqRepository(
    private val user: User = FakeData.userAdmin,
    santriList: List<Santri> = FakeData.santri,
    private val ustadzList: List<Ustadz> = FakeData.ustadz,
    seedSantri: List<AbsensiEntry> = emptyList(),
    seedUstadz: List<AbsensiEntry> = emptyList(),
    var failWith: ApiError? = null,
) : TpqRepository {

    private val santriData = santriList.toMutableList()
    private val absensiSantri = seedSantri.associateBy { it.subjectId to it.tanggal }.toMutableMap()
    private val absensiUstadz = seedUstadz.associateBy { it.subjectId to it.tanggal }.toMutableMap()

    private fun check() {
        failWith?.let { throw ApiException(it) }
    }

    private fun forbid(): Nothing = throw ApiException(ApiError(ErrorCodes.FORBIDDEN, "Anda tidak punya izin untuk tindakan ini.", 403))

    private val isAdmin get() = user.hasAny(Role.ADMIN, Role.TU)

    /** Entri milik orang lain hanya bisa diubah ADMIN/TU; entri sendiri (dicatat akun ini) selalu bisa. */
    private fun view(entry: AbsensiEntry): AbsensiEntry =
        entry.copy(bisaDiubah = isAdmin || entry.dicatatOleh == user.nama)

    override suspend fun santri(query: String?): List<Santri> {
        check()
        return santriData.filter { query.isNullOrBlank() || it.nama.contains(query, ignoreCase = true) }
    }

    override suspend fun santriDetail(id: String): Santri {
        check()
        return santriData.firstOrNull { it.id == id } ?: throw ApiException(ApiError.fromResponse(404, null))
    }

    override suspend fun updateProgress(id: String, update: ProgressUpdate): Santri {
        check()
        if (!user.hasAny(Role.ADMIN, Role.TU, Role.USTADZ)) forbid()
        val index = santriData.indexOfFirst { it.id == id }
        if (index < 0) throw ApiException(ApiError.fromResponse(404, null))
        var next = santriData[index]
        update.jilid?.let { next = next.copy(jilidSaatIni = it) }
        if (update.updateCatatan) next = next.copy(catatanProgressMadin = update.catatan)
        santriData[index] = next
        return next
    }

    override suspend fun ustadz(): List<Ustadz> {
        check()
        if (user.roles == setOf(Role.WALI)) forbid()
        return ustadzList
    }

    override suspend fun absensiSantriByTanggal(tanggal: String): List<AbsensiEntry> {
        check()
        return absensiSantri.values.filter { it.tanggal == tanggal }.map(::view)
    }

    override suspend fun absensiSantriRiwayat(santriId: String, dari: String, sampai: String): List<AbsensiEntry> {
        check()
        return absensiSantri.values
            .filter { it.subjectId == santriId && it.tanggal in dari..sampai }
            .sortedByDescending { it.tanggal }
            .map(::view)
    }

    override suspend fun simpanAbsensiSantri(santriId: String, tanggal: String, hadir: Boolean, keterangan: String?): AbsensiEntry {
        check()
        if (!user.hasAny(Role.ADMIN, Role.TU, Role.USTADZ)) forbid()
        return upsert(absensiSantri, santriId, tanggal, hadir, keterangan)
    }

    override suspend fun absensiUstadzByTanggal(tanggal: String): List<AbsensiEntry> {
        check()
        return absensiUstadz.values.filter { it.tanggal == tanggal }.map(::view)
    }

    override suspend fun simpanAbsensiUstadz(ustadzId: String, tanggal: String, hadir: Boolean, keterangan: String?): AbsensiEntry {
        check()
        if (!user.hasAny(Role.ADMIN, Role.TU, Role.PIKET)) forbid()
        if (!isAdmin && ustadzId == user.ustadzId) {
            throw ApiException(ApiError(ErrorCodes.SELF_ATTENDANCE_FORBIDDEN, "Anda tidak boleh mengisi absensi atas nama diri sendiri.", 403))
        }
        return upsert(absensiUstadz, ustadzId, tanggal, hadir, keterangan)
    }

    private fun upsert(
        store: MutableMap<Pair<String, String>, AbsensiEntry>,
        subjectId: String,
        tanggal: String,
        hadir: Boolean,
        keterangan: String?,
    ): AbsensiEntry {
        val existing = store[subjectId to tanggal]
        if (existing != null && !view(existing).bisaDiubah) forbid()
        val saved = AbsensiEntry(
            id = existing?.id ?: "e-$subjectId-$tanggal",
            subjectId = subjectId,
            tanggal = tanggal,
            hadir = hadir,
            keterangan = keterangan?.trim()?.takeIf { it.isNotEmpty() },
            dicatatOleh = existing?.dicatatOleh ?: user.nama,
            bisaDiubah = true,
        )
        store[subjectId to tanggal] = saved
        return saved
    }

    override suspend fun rekap(tanggal: String): Rekap {
        check()
        val date: LocalDate = Tanggal.fromApi(tanggal) ?: throw ApiException(ApiError.fromResponse(422, null))
        val sCount = absensiSantri.values.count { it.tanggal == tanggal }
        val sHadir = absensiSantri.values.count { it.tanggal == tanggal && it.hadir }
        val uCount = absensiUstadz.values.count { it.tanggal == tanggal }
        val uHadir = absensiUstadz.values.count { it.tanggal == tanggal && it.hadir }
        return Rekap(
            Tanggal.toApi(date),
            RekapCounts(sHadir, santriData.size, santriData.size - sCount),
            RekapCounts(uHadir, ustadzList.size, ustadzList.size - uCount),
        )
    }
}

/** Login palsu: password apa pun diterima kecuali "salah"; username dicocokkan ke [users]. */
class FakeAuthApi(private val users: Map<String, User> = mapOf("admin" to FakeData.userAdmin)) : AuthApi {
    override suspend fun login(username: String, password: String): LoginResult {
        val user = users[username]
        if (user == null || password == "salah") {
            throw ApiException(ApiError(ErrorCodes.UNAUTHENTICATED, "Username atau password salah.", 401))
        }
        return LoginResult("token-$username", user)
    }

    override suspend fun me(): User = users.values.first()
}

class InMemoryTokenStore(private var token: String? = null) : TokenStore {
    override fun get(): String? = token

    override fun save(token: String) {
        this.token = token
    }

    override fun clear() {
        token = null
    }
}
