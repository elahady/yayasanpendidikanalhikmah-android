package com.alhikmah.tpq.domain.beranda

import com.alhikmah.tpq.domain.model.Aktivitas
import com.alhikmah.tpq.domain.model.StatusHadir
import com.alhikmah.tpq.domain.model.TrenHarian
import com.alhikmah.tpq.domain.model.User
import com.alhikmah.tpq.domain.model.UstadzHariIni
import com.alhikmah.tpq.domain.model.UstadzRingkas
import com.alhikmah.tpq.domain.roster.Roster
import com.alhikmah.tpq.domain.util.Tanggal
import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToInt

/* Logika tampilan Beranda yang bisa diuji (bukan di composable): daftar ustadz hari ini, tren 14 hari, aktivitas terbaru. */

/**
 * Hasil penyusunan kartu "Ustadz hari ini".
 * [baris] = yang ditampilkan; [lainnya] = jumlah baris tersisa yang tidak muat (untuk "Lihat semua");
 * [catatanSendiri] = true bila absensi akun ini sendiri tidak ditampilkan karena dicatat oleh piket lain.
 * [adaDaftar] false bila server lama tidak mengirim `daftar` (UI jatuh ke ringkasan angka).
 */
data class UstadzHariIniTampil(
    val baris: List<UstadzHariIni>,
    val lainnya: Int,
    val catatanSendiri: Boolean,
    val adaDaftar: Boolean,
)

object UstadzHariIniBeranda {
    const val MAKS_BARIS = 5

    private fun prioritas(status: StatusHadir): Int = when (status) {
        StatusHadir.BELUM_DICATAT -> 0 // yang masih perlu dicatat piket di atas
        StatusHadir.TIDAK_HADIR -> 1
        StatusHadir.HADIR -> 2
    }

    /**
     * Menyembunyikan ustadz yang tertaut ke akun sendiri kecuali akun ADMIN/TU (aturan yang sama dengan layar Absensi
     * Ustadz: [Roster.hiddenUstadzId]), mengurutkan "belum dicatat" lalu "tidak hadir" lalu "hadir" (urut nama di
     * dalam kelompok, sesuai urutan server), dan memotong sampai [maks] baris.
     *
     * Catatan "Absensi Anda sendiri dicatat oleh piket lain." muncul bila ada baris yang disembunyikan, atau bila
     * daftar tidak dikirim server tetapi akun memang tertaut ke ustadz tanpa hak ADMIN/TU.
     */
    fun susun(ustadz: UstadzRingkas, user: User, maks: Int = MAKS_BARIS): UstadzHariIniTampil {
        val hiddenId = Roster.hiddenUstadzId(user)
        val adaDaftar = ustadz.daftar.isNotEmpty()
        val terlihat = ustadz.daftar
            .filterNot { it.id == hiddenId }
            .sortedBy { prioritas(it.status) } // sortedBy stabil: urutan nama dari server terjaga
        val tersembunyi = hiddenId != null && (!adaDaftar || ustadz.daftar.any { it.id == hiddenId })
        return UstadzHariIniTampil(
            baris = terlihat.take(maks.coerceAtLeast(0)),
            lainnya = (terlihat.size - maks).coerceAtLeast(0),
            catatanSendiri = tersembunyi,
            adaDaftar = adaDaftar,
        )
    }
}

/** Satu batang pada grafik tren. [tinggi] = hadir / tercatat (0..1). */
data class BatangTren(val tanggal: String, val hadir: Int, val tercatat: Int, val tinggi: Float, val persen: Int)

/**
 * Tren kehadiran santri per hari belajar. [rataRataPersen] = rata-rata persen harian (hari tanpa catatan dilewati),
 * null bila tidak ada hari bercatatan. [labelAwal] = tanggal batang pertama; [labelAkhir] = "Hari ini · 89%" bila
 * batang terakhir = tanggal dashboard, selain itu "30 Okt · 89%". [deskripsi] = ringkasan teks untuk pembaca layar.
 */
data class TrenKehadiranTampil(
    val batang: List<BatangTren>,
    val rataRataPersen: Int?,
    val labelAwal: String,
    val labelAkhir: String,
    val deskripsi: String,
)

object TrenKehadiran {

    /** Null bila [tren] kosong (kartu menampilkan keadaan kosong). Urutan masukan = urutan naik dari server. */
    fun susun(tren: List<TrenHarian>, tanggalDashboard: String?): TrenKehadiranTampil? {
        if (tren.isEmpty()) return null
        val batang = tren.map { h ->
            val tinggi = if (h.tercatat > 0) (h.hadir.toFloat() / h.tercatat).coerceIn(0f, 1f) else 0f
            BatangTren(h.tanggal, h.hadir, h.tercatat, tinggi, (tinggi * 100f).roundToInt())
        }
        val bercatatan = batang.filter { it.tercatat > 0 }
        val rata = if (bercatatan.isEmpty()) null else (bercatatan.map { it.tinggi }.average() * 100).roundToInt()

        fun label(b: BatangTren): String = Tanggal.fromApi(b.tanggal)?.let(Tanggal::short) ?: b.tanggal
        val awal = batang.first()
        val akhir = batang.last()
        val akhirHariIni = akhir.tanggal == tanggalDashboard
        val labelAkhir = (if (akhirHariIni) "Hari ini" else label(akhir)) + " · ${akhir.persen}%"

        val deskripsi = buildString {
            append("Kehadiran santri ${batang.size} hari belajar terakhir")
            if (rata != null) append(", rata-rata $rata persen")
            append(". ")
            append(
                batang.joinToString(". ") { b ->
                    val nama = if (b.tanggal == tanggalDashboard) "Hari ini" else label(b)
                    "$nama: ${b.persen} persen, ${b.hadir} dari ${b.tercatat} tercatat"
                },
            )
            append('.')
        }
        return TrenKehadiranTampil(batang, rata, label(awal), labelAkhir, deskripsi)
    }
}

/** Waktu aktivitas dalam bahasa manusia, selalu zona Asia/Jakarta. */
object WaktuAktivitas {

    /** Jam `HH.mm` Asia/Jakarta dari waktu ISO server; null bila bukan waktu lengkap (mis. tanggal polos). */
    fun jam(iso: String?): String? {
        if (iso.isNullOrBlank()) return null
        return try {
            val t = Instant.parse(iso).atZone(Tanggal.ZONA)
            String.format(java.util.Locale.ROOT, "%02d.%02d", t.hour, t.minute)
        } catch (_: java.time.format.DateTimeParseException) {
            null
        }
    }

    /**
     * "Hari ini, 08.12" / "Kemarin, 16.20" / "29 Okt, 09.20". [hariIni] = tanggal dashboard (hari ini menurut server).
     * Bila waktu tak bisa dibaca: string aslinya.
     */
    fun label(iso: String, hariIni: LocalDate): String {
        val tanggal = Tanggal.fromServerTime(iso) ?: return iso
        val hari = when (tanggal) {
            hariIni -> "Hari ini"
            hariIni.minusDays(1) -> "Kemarin"
            else -> Tanggal.short(tanggal)
        }
        return jam(iso)?.let { "$hari, $it" } ?: hari
    }
}

/** Baris aktivitas siap tampil. */
data class AktivitasTampil(val ringkas: String, val waktu: String, val oleh: String)

object AktivitasBeranda {
    const val MAKS = 6

    /** Server sudah mengurutkan terbaru dahulu dan membatasi 6; di sini hanya dipotong lagi sebagai pengaman. */
    fun susun(aktivitas: List<Aktivitas>, hariIni: LocalDate, maks: Int = MAKS): List<AktivitasTampil> =
        aktivitas.take(maks).map { AktivitasTampil(it.ringkas, WaktuAktivitas.label(it.waktu, hariIni), it.oleh) }
}
