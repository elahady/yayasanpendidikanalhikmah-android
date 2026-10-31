package com.alhikmah.tpq.domain.util

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Jadwal belajar TPQ: **Senin-Jumat belajar, Sabtu dan Minggu libur** (keputusan pemilik, 31 Okt 2026).
 * Salinan aturan server (`lib/jadwal.ts` di repo web; lihat "Jadwal (hari belajar)" di docs/api-contract.md).
 * Server tetap penentu: app memakai ini untuk mencegah input yang pasti ditolak dan untuk menampilkan keadaan libur
 * sebelum jawaban server tiba. Belum ada kalender libur khusus (libur nasional / cuti bersama).
 */
object Jadwal {
    /** Pesan 422 server untuk `PUT /absensi-*` pada hari libur (dipakai fake agar sama dengan server). */
    const val PESAN_SERVER_LIBUR = "TPQ libur pada hari Sabtu dan Minggu. Absensi tidak dapat dicatat."

    /** Spanduk di layar absensi/rekap saat tanggal terpilih libur. */
    const val BANNER_LIBUR = "Libur — TPQ belajar Senin–Jumat"

    /** Teks tombol yang memindahkan tanggal ke hari belajar terakhir. */
    const val AKSI_HARI_BELAJAR_TERAKHIR = "Ke hari belajar terakhir"

    /** Kalimat aturan, dipakai di kartu libur Beranda dan di spanduk. */
    const val ATURAN = "TPQ belajar Senin–Jumat."

    private val hariLibur = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    fun libur(date: LocalDate): Boolean = date.dayOfWeek in hariLibur

    fun hariBelajar(date: LocalDate): Boolean = !libur(date)

    /**
     * Untuk tanggal `YYYY-MM-DD` dari API. Tanggal yang tidak bisa dibaca dianggap hari belajar: app tidak menebak,
     * server yang memutuskan.
     */
    fun hariBelajar(tanggal: String?): Boolean = Tanggal.fromApi(tanggal)?.let { hariBelajar(it) } ?: true

    /** `Senin`, `Sabtu`, dst. */
    fun namaHari(date: LocalDate): String = Tanggal.namaHari(date)

    /** Hari belajar pada atau sebelum [date]: tanggal itu sendiri bila hari belajar, selain itu mundur (Sabtu/Minggu -> Jumat). */
    fun hariBelajarTerakhir(date: LocalDate): LocalDate {
        var d = date
        while (libur(d)) d = d.minusDays(1)
        return d
    }

    /** Hari belajar terakhir *sebelum* [date] (Senin -> Jumat pekan lalu, Sabtu -> Jumat). */
    fun hariBelajarSebelum(date: LocalDate): LocalDate = hariBelajarTerakhir(date.minusDays(1))

    /** Hari belajar pertama *setelah* [date] (Jumat/Sabtu/Minggu -> Senin). */
    fun hariBelajarBerikutnya(date: LocalDate): LocalDate {
        var d = date.plusDays(1)
        while (libur(d)) d = d.plusDays(1)
        return d
    }

    /** `Jadwal.hariBelajarTerakhir` untuk tanggal API; null bila bukan tanggal valid. */
    fun hariBelajarTerakhir(tanggal: String?): String? = Tanggal.fromApi(tanggal)?.let { Tanggal.toApi(hariBelajarTerakhir(it)) }

    /** "TPQ belajar Senin–Jumat. Kegiatan dilanjutkan hari Senin." untuk kartu libur Beranda. */
    fun keteranganLibur(date: LocalDate): String = "$ATURAN Kegiatan dilanjutkan hari ${namaHari(hariBelajarBerikutnya(date))}."
}
