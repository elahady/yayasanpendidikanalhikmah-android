package com.alhikmah.tpq.domain.util

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/** Tanggal kontrak API: `YYYY-MM-DD` menurut zona Asia/Jakarta. */
object Tanggal {
    val ZONA: ZoneId = ZoneId.of("Asia/Jakarta")

    private val hari = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
    private val bulan = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember",
    )

    fun today(clock: Clock = Clock.system(ZONA)): LocalDate = LocalDate.now(clock.withZone(ZONA))

    /** `2026-11-02`. */
    fun toApi(date: LocalDate): String = date.toString()

    /** Null bila bukan tanggal valid `YYYY-MM-DD`. */
    fun fromApi(value: String?): LocalDate? = try {
        value?.let(LocalDate::parse)
    } catch (_: java.time.format.DateTimeParseException) {
        null
    }

    /** `Senin`, `Selasa`, ... `Minggu`. */
    fun namaHari(date: LocalDate): String = hari[date.dayOfWeek.value - 1]

    /** `Senin, 2 November 2026`. */
    fun label(date: LocalDate): String =
        "${hari[date.dayOfWeek.value - 1]}, ${date.dayOfMonth} ${bulan[date.monthValue - 1]} ${date.year}"

    /** `2 Nov` tanpa tahun, untuk daftar riwayat yang rapat. */
    fun short(date: LocalDate): String = "${date.dayOfMonth} ${bulan[date.monthValue - 1].take(3)}"

    /** `29 Oktober 2026` (tanpa nama hari). */
    fun panjang(date: LocalDate): String = "${date.dayOfMonth} ${bulan[date.monthValue - 1]} ${date.year}"

    /**
     * Waktu ISO-8601 dari server (UTC, mis. `2026-10-29T09:20:00.000Z`) menjadi tanggal kalender Asia/Jakarta.
     * Tanggal polos `YYYY-MM-DD` juga diterima; null bila tidak valid.
     */
    fun fromServerTime(iso: String?): LocalDate? {
        if (iso.isNullOrBlank()) return null
        return try {
            java.time.Instant.parse(iso).atZone(ZONA).toLocalDate()
        } catch (_: java.time.format.DateTimeParseException) {
            fromApi(iso)
        }
    }

    /** Label `29 Oktober 2026` dari waktu server; null bila tidak valid. */
    fun panjangDariServer(iso: String?): String? = fromServerTime(iso)?.let(::panjang)
}
