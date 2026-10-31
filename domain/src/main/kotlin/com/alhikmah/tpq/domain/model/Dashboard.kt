package com.alhikmah.tpq.domain.model

import com.alhikmah.tpq.domain.menu.MenuId
import kotlin.math.roundToInt

/**
 * Bahan layar Beranda dari `GET /dashboard` (docs/api-contract.md di repo web).
 * [ringkasan] null untuk akun WALI murni; [anak] null bila akun tidak punya role WALI.
 */
data class Dashboard(
    val tanggal: String,
    val ringkasan: Ringkasan?,
    val anak: List<AnakDashboard>?,
)

data class Ringkasan(
    val santri: SantriRingkas,
    val ustadz: UstadzRingkas,
    val tren: List<TrenHarian>,
    val perhatian: List<Perhatian>,
    val aktivitas: List<Aktivitas>,
)

data class SantriRingkas(
    val total: Int,
    val hadir: Int,
    val tidakHadir: Int,
    val belumDicatat: Int,
    /** Semua kunci selalu ada (0 bila kosong). */
    val perJenjang: Map<Jenjang, Int>,
    val perJilid: Map<Jilid, Int>,
) {
    /** Persen hadir dibulatkan (0 bila belum ada santri). */
    val persenHadir: Int
        get() = if (total > 0) (hadir * 100f / total).roundToInt() else 0

    /** Bagian 0..1 untuk cincin kehadiran. */
    val bagianHadir: Float
        get() = if (total > 0) (hadir.toFloat() / total).coerceIn(0f, 1f) else 0f
}

data class UstadzRingkas(
    val total: Int,
    val hadir: Int,
    val tidakHadir: Int,
    val belumDicatat: Int,
) {
    /** Ustadz yang sudah punya entri hari itu (hadir + tidak hadir). */
    val tercatat: Int get() = (total - belumDicatat).coerceAtLeast(0)
}

/** Satu hari belajar pada tren: [tercatat] = jumlah entri (hadir + tidak hadir). */
data class TrenHarian(val tanggal: String, val hadir: Int, val tercatat: Int)

/** Tujuan ketukan pada item "Perlu perhatian" (menurut kontrak). */
enum class AksiPerhatian(val menu: MenuId) {
    ABSENSI_SANTRI(MenuId.ABSENSI_SANTRI),
    ABSENSI_USTADZ(MenuId.ABSENSI_USTADZ),
    DAFTAR_SANTRI(MenuId.DAFTAR_SANTRI),
    ;

    companion object {
        fun fromApi(value: String?): AksiPerhatian? = entries.firstOrNull { it.name == value }
    }
}

/**
 * Satu item "Perlu perhatian". [judul] dan [keterangan] dirakit server (Indonesia), ditampilkan apa adanya.
 * [aksi] null bila server mengirim aksi yang belum dikenal app ini.
 */
data class Perhatian(
    val kode: String,
    val jumlah: Int,
    val judul: String,
    val keterangan: String?,
    val aksi: AksiPerhatian?,
) {
    /** Layar tujuan bila menunya tersedia untuk role ini ([allowed]); null = item tidak bisa diketuk. */
    fun tujuan(allowed: Set<MenuId>): MenuId? = aksi?.menu?.takeIf { it in allowed }
}

/** Satu baris aktivitas terbaru; [waktu] berupa ISO-8601. [jenis] = ABSENSI_SANTRI | ABSENSI_USTADZ. */
data class Aktivitas(val waktu: String, val oleh: String, val jenis: String, val ringkas: String)

enum class StatusHadir {
    HADIR,
    TIDAK_HADIR,
    BELUM_DICATAT,
    ;

    companion object {
        /** Nilai tak dikenal diperlakukan sebagai belum dicatat (paling netral). */
        fun fromApi(value: String?): StatusHadir = entries.firstOrNull { it.name == value } ?: BELUM_DICATAT
    }
}

data class HariKehadiran(val tanggal: String, val status: StatusHadir)

/** Kehadiran satu anak; tiga angka = hitungan dari [hari] (hanya hari belajar, maksimal 30 tanggal terakhir). */
data class KehadiranAnak(
    val hadir: Int,
    val tidakHadir: Int,
    val belumDicatat: Int,
    val hari: List<HariKehadiran>,
)

/** [diperbaruiPada] ISO-8601 (kapan data progress terakhir berubah); null bila server tidak mengirimnya. */
data class AnakDashboard(
    val santri: Santri,
    val diperbaruiPada: String?,
    val kehadiran: KehadiranAnak,
)

/** Anak yang dipilih untuk ditampilkan: sesuai [id] bila ada, selain itu anak pertama. */
fun List<AnakDashboard>.pilih(id: String?): AnakDashboard? = firstOrNull { it.santri.id == id } ?: firstOrNull()
