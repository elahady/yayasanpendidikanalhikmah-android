package com.alhikmah.tpq.domain.model

/**
 * Satu entri absensi (santri atau ustadz). [subjectId] = `santriId` atau `ustadzId` menurut endpoint-nya.
 * [bisaDiubah] dihitung server untuk pemanggil; klien tinggal menonaktifkan tombol.
 */
data class AbsensiEntry(
    val id: String,
    val subjectId: String,
    val tanggal: String,
    val hadir: Boolean,
    val keterangan: String?,
    val dicatatOleh: String?,
    val bisaDiubah: Boolean,
)

data class RekapCounts(
    val hadir: Int,
    val total: Int,
    val belumDicatat: Int,
)

/** [hariBelajar] false bila [tanggal] libur (Sabtu/Minggu); server lama tidak mengirimnya -> true. */
data class Rekap(
    val tanggal: String,
    val santri: RekapCounts,
    val ustadz: RekapCounts,
    val hariBelajar: Boolean = true,
)
