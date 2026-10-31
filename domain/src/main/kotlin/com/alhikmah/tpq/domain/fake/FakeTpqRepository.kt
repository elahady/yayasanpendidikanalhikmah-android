package com.alhikmah.tpq.domain.fake

import com.alhikmah.tpq.domain.api.ApiError
import com.alhikmah.tpq.domain.api.ApiException
import com.alhikmah.tpq.domain.api.AuthApi
import com.alhikmah.tpq.domain.api.ErrorCodes
import com.alhikmah.tpq.domain.api.LoginResult
import com.alhikmah.tpq.domain.api.TokenStore
import com.alhikmah.tpq.domain.api.TpqRepository
import com.alhikmah.tpq.domain.menu.MenuResolver
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
import com.alhikmah.tpq.domain.util.Jadwal
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

    /** Password awal akun palsu (dipakai [FakeTpqRepository.ubahPassword]). */
    const val PASSWORD = "rahasia123"

    val userAdmin = User("a1", "Pak Tata Usaha", setOf(Role.TU), null, "tatausaha", null, "2026-09-01T03:00:00.000Z")
    val userPiketUstadz = User("a2", "Ustadz Hasan", setOf(Role.PIKET, Role.USTADZ), "u3", "hasan", "Ustadz Hasan Basri", "2026-10-27T03:12:00.000Z")
    val userPengawas = User("a3", "Bu Pengawas", setOf(Role.PENGAWAS), null, "pengawas", null, "2026-09-01T03:00:00.000Z")
    val userWali = User("a4", "Bapak Wali Aisyah", setOf(Role.WALI), null, "wali.aisyah", null, "2026-09-15T03:00:00.000Z")
    val userUstadz = User("a5", "Ustadzah Siti", setOf(Role.USTADZ), "u2", "siti", "Ustadzah Siti Nurhaliza", "2026-09-01T03:00:00.000Z")
    val userWaliUstadz = User("a6", "Ustadz Fauzi", setOf(Role.USTADZ, Role.WALI), "u1", "fauzi", "Ustadz Ahmad Fauzi", "2026-09-01T03:00:00.000Z")

    // ---- contoh /dashboard untuk preview Compose (angka dari contoh kontrak) ----

    private val perJilid = mapOf(
        Jilid.PRA_NAHDLIYAH to 9, Jilid.JILID_1 to 14, Jilid.JILID_2 to 17, Jilid.JILID_3 to 12,
        Jilid.JILID_4 to 10, Jilid.JILID_5 to 7, Jilid.JILID_6 to 5, Jilid.AL_QURAN to 12,
    )

    /** Sembilan ustadz urut nama untuk preview; "u3" (Hasan Basri) = akun [userPiketUstadz]. 7 hadir, 1 tidak hadir, 1 belum dicatat. */
    val ustadzHariIni: List<UstadzHariIni> = listOf(
        UstadzHariIni("u1", "Ustadz Ahmad Fauzi", "Tahfidz", StatusHadir.HADIR),
        UstadzHariIni("u3", "Ustadz Hasan Basri", "Fiqih", StatusHadir.HADIR),
        UstadzHariIni("u7", "Ustadz Mansur", "Bahasa Arab", StatusHadir.TIDAK_HADIR),
        UstadzHariIni("u2", "Ustadzah Siti Nurhaliza", "Al-Qur'an & Tajwid", StatusHadir.HADIR),
        UstadzHariIni("u4", "Ustadzah Fatimah", "Jilid 1-2", StatusHadir.HADIR),
        UstadzHariIni("u5", "Ustadzah Nur", null, StatusHadir.HADIR),
        UstadzHariIni("u6", "Ustadz Yusuf", "Akhlak", StatusHadir.HADIR),
        UstadzHariIni("u8", "Ustadz Zainal", "Fiqih", StatusHadir.BELUM_DICATAT),
        UstadzHariIni("u9", "Ustadzah Zulfa", "Tajwid", StatusHadir.HADIR),
    )

    /** Hari sampel "hari ini" untuk dashboard hari belajar (Jumat) dan hari libur (Sabtu keesokan harinya). */
    const val HARI_BELAJAR_SAMPEL = "2026-10-30"
    const val HARI_LIBUR_SAMPEL = "2026-10-31"

    /** 14 hari belajar (Senin-Jumat) terakhir sampai [HARI_BELAJAR_SAMPEL] (urut naik), hari terakhir belum lengkap (71 dari 80 tercatat). */
    val trenContoh: List<TrenHarian> = listOf(
        "2026-10-13" to 76, "2026-10-14" to 79, "2026-10-15" to 73, "2026-10-16" to 77, "2026-10-19" to 80,
        "2026-10-20" to 72, "2026-10-21" to 76, "2026-10-22" to 79, "2026-10-23" to 75, "2026-10-26" to 73,
        "2026-10-27" to 77, "2026-10-28" to 78, "2026-10-29" to 74,
    ).map { (tanggal, hadir) -> TrenHarian(tanggal, hadir, 86) } + TrenHarian(HARI_BELAJAR_SAMPEL, 71, 80)

    val ringkasan = Ringkasan(
        santri = SantriRingkas(86, 71, 9, 6, mapOf(Jenjang.ULA to 61, Jenjang.WUSTHO to 25), perJilid),
        ustadz = UstadzRingkas(9, 7, 1, 1, ustadzHariIni),
        tren = trenContoh,
        perhatian = listOf(
            Perhatian("SANTRI_BELUM_DICATAT", 6, "6 santri belum dicatat absensinya", "Aisyah, Fadhil, Rafi dan 3 lainnya", AksiPerhatian.ABSENSI_SANTRI),
            Perhatian("SANTRI_ABSEN_BERUNTUN", 3, "3 santri tidak hadir 3 hari belajar berturut-turut", "Aisyah, Fadhil, Rafi", AksiPerhatian.ABSENSI_SANTRI),
            Perhatian("USTADZ_BELUM_DICATAT", 1, "1 ustadz belum dicatat absensinya", "Zainal", AksiPerhatian.ABSENSI_USTADZ),
            Perhatian("PROGRESS_LAMA", 5, "5 santri progress belum berubah lebih dari 60 hari", "Budi, Citra, Dimas dan 2 lainnya", AksiPerhatian.DAFTAR_SANTRI),
        ),
        aktivitas = listOf(
            Aktivitas("2026-10-30T01:12:00.000Z", "Ust. Hasan", "ABSENSI_SANTRI", "Ust. Hasan mencatat absensi santri (12 santri)"),
            Aktivitas("2026-10-30T00:48:00.000Z", "Ust. Zainal", "ABSENSI_USTADZ", "Ust. Zainal mencatat absensi ustadz (7 ustadz)"),
            Aktivitas("2026-10-29T09:20:00.000Z", "Bu Nur", "ABSENSI_SANTRI", "Bu Nur mencatat absensi santri (14 santri)"),
        ),
    )

    val dashboardAdmin = Dashboard(HARI_BELAJAR_SAMPEL, ringkasan, null)

    /** Piket + ustadz: ustadz 7 dari 9 sudah tercatat tersisa 2 (dipakai kartu tugas piket). */
    val dashboardTugas = Dashboard(
        HARI_BELAJAR_SAMPEL,
        ringkasan.copy(
            ustadz = UstadzRingkas(
                9, 6, 1, 2,
                ustadzHariIni.map { if (it.id == "u9") it.copy(status = StatusHadir.BELUM_DICATAT) else it },
            ),
        ),
        null,
    )

    /** 1-30 Okt 2026 hanya hari belajar (server tidak mengirim Sabtu/Minggu); hari terakhir ([HARI_BELAJAR_SAMPEL]) belum dicatat. */
    private val hariWali: List<HariKehadiran> = (1..30)
        .map { LocalDate.of(2026, 10, it) }
        .filter { Jadwal.hariBelajar(it) }
        .map { d ->
            val status = when (d.dayOfMonth) {
                9, 15 -> StatusHadir.TIDAK_HADIR
                30 -> StatusHadir.BELUM_DICATAT
                else -> StatusHadir.HADIR
            }
            HariKehadiran(Tanggal.toApi(d), status)
        }

    val anakAisyah = AnakDashboard(
        santri = Santri("s1", "Aisyah Nur Hidayah", Jenjang.ULA, "2", Jilid.JILID_3, "Madin: sudah sampai materi fiqih bab wudhu. Bacaan makin lancar, tinggal dilatih panjang-pendek pada mad."),
        diperbaruiPada = "2026-10-29T09:20:00.000Z",
        kehadiran = KehadiranAnak(
            hadir = hariWali.count { it.status == StatusHadir.HADIR },
            tidakHadir = hariWali.count { it.status == StatusHadir.TIDAK_HADIR },
            belumDicatat = hariWali.count { it.status == StatusHadir.BELUM_DICATAT },
            hari = hariWali,
        ),
    )

    val anakHanif = AnakDashboard(
        santri = Santri("s6", "Hanif Maulana", Jenjang.ULA, "1", Jilid.PRA_NAHDLIYAH, null),
        diperbaruiPada = null,
        kehadiran = KehadiranAnak(0, 0, 0, emptyList()),
    )

    val dashboardWali = Dashboard(HARI_BELAJAR_SAMPEL, null, listOf(anakAisyah, anakHanif))

    // ---- contoh hari libur (Sabtu): `hariBelajar=false`, belumDicatat 0, tanpa item belum-dicatat (kontrak "Jadwal") ----

    /** Tren, aktivitas, dan perhatian lain tetap ada; angka hari itu nol karena tidak ada kegiatan. */
    val ringkasanLibur = ringkasan.copy(
        santri = ringkasan.santri.copy(hadir = 0, tidakHadir = 0, belumDicatat = 0),
        ustadz = UstadzRingkas(ringkasan.ustadz.total, 0, 0, 0, emptyList()),
        perhatian = ringkasan.perhatian.filter { it.kode != "SANTRI_BELUM_DICATAT" && it.kode != "USTADZ_BELUM_DICATAT" },
    )

    val dashboardAdminLibur = Dashboard(HARI_LIBUR_SAMPEL, ringkasanLibur, null, hariBelajar = false)

    val dashboardTugasLibur = Dashboard(HARI_LIBUR_SAMPEL, ringkasanLibur, null, hariBelajar = false)

    /** Wali: `kehadiran.hari` tidak memuat tanggal libur; kartu kehadiran tetap menampilkan hari belajar sebelumnya. */
    val dashboardWaliLibur = Dashboard(HARI_LIBUR_SAMPEL, null, listOf(anakAisyah, anakHanif), hariBelajar = false)

    fun entry(subjectId: String, tanggal: String, hadir: Boolean, keterangan: String? = null, bisaDiubah: Boolean = true) =
        AbsensiEntry("e-$subjectId-$tanggal", subjectId, tanggal, hadir, keterangan, "Pak Tata Usaha", bisaDiubah)

    /** Contoh rekap; hari libur: `hariBelajar=false` dan belumDicatat 0 seperti server. */
    fun rekap(tanggal: String): Rekap =
        if (Jadwal.hariBelajar(tanggal)) {
            Rekap(tanggal, RekapCounts(40, 45, 2), RekapCounts(5, 6, 0))
        } else {
            Rekap(tanggal, RekapCounts(0, 45, 0), RekapCounts(0, 6, 0), hariBelajar = false)
        }
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
    /** Id santri yang menjadi anak akun ini (hanya berarti untuk role WALI). */
    private val anakIds: Set<String> = emptySet(),
    private var password: String = FakeData.PASSWORD,
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

    /** PIKET murni tidak boleh GET /absensi-santri (kontrak). */
    private fun requireAbsensiSantriRead() {
        if (!MenuResolver.canViewAbsensiSantri(user.roles)) forbid()
    }

    override suspend fun absensiSantriByTanggal(tanggal: String): List<AbsensiEntry> {
        check()
        requireAbsensiSantriRead()
        return absensiSantri.values.filter { it.tanggal == tanggal }.map(::view)
    }

    override suspend fun absensiSantriRiwayat(santriId: String, dari: String, sampai: String): List<AbsensiEntry> {
        check()
        requireAbsensiSantriRead()
        return absensiSantri.values
            .filter { it.subjectId == santriId && it.tanggal in dari..sampai }
            .sortedByDescending { it.tanggal }
            .map(::view)
    }

    override suspend fun simpanAbsensiSantri(santriId: String, tanggal: String, hadir: Boolean, keterangan: String?): AbsensiEntry {
        check()
        if (!user.hasAny(Role.ADMIN, Role.TU, Role.USTADZ)) forbid()
        tolakBilaLibur(tanggal)
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
        tolakBilaLibur(tanggal)
        return upsert(absensiUstadz, ustadzId, tanggal, hadir, keterangan)
    }

    /** Server menolak pencatatan pada Sabtu/Minggu dengan 422 VALIDATION (kontrak "Jadwal"). */
    private fun tolakBilaLibur(tanggal: String) {
        if (!Jadwal.hariBelajar(tanggal)) throw ApiException(ApiError(ErrorCodes.VALIDATION, Jadwal.PESAN_SERVER_LIBUR, 422))
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
            dicatatOleh = user.nama, // pencatat = akun terakhir yang menyimpan (kontrak)
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
        val hariBelajar = Jadwal.hariBelajar(date)
        // Hari libur tidak dihitung "belum dicatat" (kontrak "Jadwal").
        return Rekap(
            Tanggal.toApi(date),
            RekapCounts(sHadir, santriData.size, if (hariBelajar) santriData.size - sCount else 0),
            RekapCounts(uHadir, ustadzList.size, if (hariBelajar) ustadzList.size - uCount else 0),
            hariBelajar,
        )
    }

    private var wrongPasswordCount = 0

    override suspend fun ubahPassword(passwordLama: String, passwordBaru: String) {
        check()
        if (passwordBaru.length !in 8..72) {
            throw ApiException(ApiError(ErrorCodes.VALIDATION, "Password baru harus 8 sampai 72 karakter.", 422))
        }
        if (passwordBaru == passwordLama) {
            throw ApiException(ApiError(ErrorCodes.VALIDATION, "Password baru harus berbeda dari password sekarang.", 422))
        }
        // Server: 5 salah / 15 menit per akun -> 429 + Retry-After.
        if (wrongPasswordCount >= MAX_WRONG_PASSWORD) {
            throw ApiException(ApiError.fromResponse(429, """{"error":{"code":"RATE_LIMITED","message":"Terlalu banyak percobaan."}}""", "900"))
        }
        if (passwordLama != password) {
            wrongPasswordCount++
            // 422, bukan 401: supaya app tidak mengira sesi habis (kontrak).
            throw ApiException(ApiError(ErrorCodes.VALIDATION, "Password sekarang salah.", 422))
        }
        wrongPasswordCount = 0
        password = passwordBaru
    }

    override suspend fun dashboard(tanggal: String?): Dashboard {
        check()
        val date = if (tanggal == null) Tanggal.today() else Tanggal.fromApi(tanggal) ?: throw ApiException(ApiError.fromResponse(422, null))
        val iso = Tanggal.toApi(date)
        val nonWali = user.roles.any { it != Role.WALI }
        val hariBelajar = Jadwal.hariBelajar(date)
        return Dashboard(
            tanggal = iso,
            ringkasan = if (nonWali) ringkasan(iso, libur = !hariBelajar) else null,
            anak = if (Role.WALI in user.roles) anakIds.mapNotNull { id -> santriData.firstOrNull { it.id == id } }.map { anak(it, date) } else null,
            hariBelajar = hariBelajar,
        )
    }

    private fun ringkasan(tanggal: String, libur: Boolean): Ringkasan {
        val hariIni = absensiSantri.values.filter { it.tanggal == tanggal }
        val sHadir = hariIni.count { it.hadir }
        val sTidak = hariIni.count { !it.hadir }
        val ustHariIni = absensiUstadz.values.filter { it.tanggal == tanggal }
        val uHadir = ustHariIni.count { it.hadir }
        val uTidak = ustHariIni.count { !it.hadir }
        val santri = SantriRingkas(
            total = santriData.size,
            hadir = sHadir,
            tidakHadir = sTidak,
            belumDicatat = if (!libur) santriData.size - sHadir - sTidak else 0,
            perJenjang = Jenjang.entries.associateWith { j -> santriData.count { it.jenjang == j } },
            perJilid = Jilid.entries.associateWith { j -> santriData.count { it.jilidSaatIni == j } },
        )
        val daftarUstadz = ustadzList
            .sortedWith(compareBy<Ustadz> { it.nama.lowercase() }.thenBy { it.id })
            .map { u ->
                val status = when (ustHariIni.lastOrNull { it.subjectId == u.id }?.hadir) {
                    true -> StatusHadir.HADIR
                    false -> StatusHadir.TIDAK_HADIR
                    null -> StatusHadir.BELUM_DICATAT
                }
                UstadzHariIni(u.id, u.nama, u.mapelDiajar, status)
            }
        val ustadz = UstadzRingkas(ustadzList.size, uHadir, uTidak, if (!libur) ustadzList.size - uHadir - uTidak else 0, daftarUstadz)

        // Hari belajar = tanggal yang punya >= 1 entri absensi santri; maksimal 14 terakhir sampai tanggal (urut naik).
        val hariBelajar = absensiSantri.values.map { it.tanggal }.filter { it <= tanggal }.distinct().sorted().takeLast(TREN_HARI)
        val tren = hariBelajar.map { d ->
            val entries = absensiSantri.values.filter { it.tanggal == d }
            TrenHarian(d, entries.count { it.hadir }, entries.size)
        }

        val perhatian = buildList {
            if (MenuResolver.canViewAbsensiSantri(user.roles)) {
                val belum = if (!libur) santriData.filter { s -> hariIni.none { it.subjectId == s.id } } else emptyList()
                if (belum.isNotEmpty()) {
                    add(Perhatian("SANTRI_BELUM_DICATAT", belum.size, "${belum.size} santri belum dicatat absensinya", namaRingkas(belum.map { it.nama }), AksiPerhatian.ABSENSI_SANTRI))
                }
                val tigaTerakhir = hariBelajar.takeLast(3)
                if (tigaTerakhir.size == 3) {
                    val beruntun = santriData.filter { s ->
                        tigaTerakhir.all { d -> absensiSantri[s.id to d]?.hadir == false }
                    }
                    if (beruntun.isNotEmpty()) {
                        add(Perhatian("SANTRI_ABSEN_BERUNTUN", beruntun.size, "${beruntun.size} santri tidak hadir 3 hari belajar berturut-turut", namaRingkas(beruntun.map { it.nama }), AksiPerhatian.ABSENSI_SANTRI))
                    }
                }
            }
            val ustBelum = if (!libur) ustadzList.filter { u -> ustHariIni.none { it.subjectId == u.id } } else emptyList()
            if (ustBelum.isNotEmpty()) {
                add(Perhatian("USTADZ_BELUM_DICATAT", ustBelum.size, "${ustBelum.size} ustadz belum dicatat absensinya", namaRingkas(ustBelum.map { it.nama }), AksiPerhatian.ABSENSI_USTADZ))
            }
            // PROGRESS_LAMA tidak ditiru: fake tidak menyimpan updatedAt.
        }

        val batas = Tanggal.fromApi(tanggal)?.minusDays(6)?.let(Tanggal::toApi) ?: tanggal
        fun kelompok(store: Collection<AbsensiEntry>, jenis: String, label: String): List<Aktivitas> =
            store.filter { it.dicatatOleh != null && it.tanggal in batas..tanggal }
                .groupBy { it.dicatatOleh!! to it.tanggal }
                .map { (key, list) ->
                    Aktivitas("${key.second}T00:00:00.000Z", key.first, jenis, "${key.first} mencatat absensi $label (${list.size} $label)")
                }
        val aktivitas = (
            (if (MenuResolver.canViewAbsensiSantri(user.roles)) kelompok(absensiSantri.values, "ABSENSI_SANTRI", "santri") else emptyList()) +
                kelompok(absensiUstadz.values, "ABSENSI_USTADZ", "ustadz")
            ).sortedByDescending { it.waktu }.take(AKTIVITAS_MAKS)

        return Ringkasan(santri, ustadz, tren, perhatian, aktivitas)
    }

    private fun anak(santri: Santri, tanggal: LocalDate): AnakDashboard {
        val iso = Tanggal.toApi(tanggal)
        val hariBelajar = absensiSantri.values.map { it.tanggal }.toSet() + iso
        // Tanggal libur tidak ikut sebagai BELUM_DICATAT; hanya bila anak ini punya entri di hari libur itu (data lama).
        val hari = (29 downTo 0).map { Tanggal.toApi(tanggal.minusDays(it.toLong())) }
            .filter { it in hariBelajar }
            .filter { Jadwal.hariBelajar(it) || absensiSantri.containsKey(santri.id to it) }
            .map { d ->
                val status = when (absensiSantri[santri.id to d]?.hadir) {
                    true -> StatusHadir.HADIR
                    false -> StatusHadir.TIDAK_HADIR
                    null -> StatusHadir.BELUM_DICATAT
                }
                HariKehadiran(d, status)
            }
        return AnakDashboard(
            santri = santri,
            diperbaruiPada = null,
            kehadiran = KehadiranAnak(
                hadir = hari.count { it.status == StatusHadir.HADIR },
                tidakHadir = hari.count { it.status == StatusHadir.TIDAK_HADIR },
                belumDicatat = hari.count { it.status == StatusHadir.BELUM_DICATAT },
                hari = hari,
            ),
        )
    }

    /** "A, B, C dan N lainnya" (sampai 3 nama), seperti server. */
    private fun namaRingkas(names: List<String>): String {
        val depan = names.sorted().take(3).map { it.substringBefore(' ') }
        val sisa = names.size - depan.size
        return depan.joinToString(", ") + if (sisa > 0) " dan $sisa lainnya" else ""
    }

    private companion object {
        const val MAX_WRONG_PASSWORD = 5
        const val TREN_HARI = 14
        const val AKTIVITAS_MAKS = 6
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
