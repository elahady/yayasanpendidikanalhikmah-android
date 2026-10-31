package com.alhikmah.tpq.domain.roster

import com.alhikmah.tpq.domain.model.AbsensiEntry
import com.alhikmah.tpq.domain.model.Santri
import com.alhikmah.tpq.domain.model.User
import com.alhikmah.tpq.domain.model.Ustadz
import com.alhikmah.tpq.domain.menu.MenuResolver

/** Seseorang di daftar absen (santri atau ustadz), disamakan bentuknya. */
data class RosterPerson(
    val id: String,
    val nama: String,
    val keterangan: String?,
)

/** Satu baris layar absensi: orangnya + entri absensi tanggal itu (null = belum dicatat). */
data class RosterRow(
    val person: RosterPerson,
    val entry: AbsensiEntry?,
) {
    val belumDicatat: Boolean get() = entry == null

    /** Baris tanpa entri boleh dibuat (server yang menolak bila tidak berhak); entri yang ada ikut `bisaDiubah` server. */
    val bisaDiubah: Boolean get() = entry?.bisaDiubah ?: true
}

object Roster {

    fun person(santri: Santri) = RosterPerson(santri.id, santri.nama, santri.keteranganKelas.takeIf { it.isNotEmpty() })

    fun person(ustadz: Ustadz) = RosterPerson(ustadz.id, ustadz.nama, ustadz.mapelDiajar)

    /**
     * Menggabungkan daftar orang (dari /santri atau /ustadz) dengan entri absensi satu tanggal.
     * Urut nama (tanpa memedulikan huruf besar/kecil). Entri yang orangnya tidak ada di daftar diabaikan;
     * bila ada beberapa entri untuk orang yang sama, entri terakhir yang dipakai.
     */
    fun merge(people: List<RosterPerson>, entries: List<AbsensiEntry>): List<RosterRow> {
        val bySubject = entries.associateBy { it.subjectId }
        return people
            .sortedWith(compareBy<RosterPerson> { it.nama.lowercase() }.thenBy { it.id })
            .map { RosterRow(it, bySubject[it.id]) }
    }

    /**
     * Ustadz yang tidak boleh diabsen oleh akun ini: ustadz yang tertaut ke akun sendiri (server menolak dengan
     * SELF_ATTENDANCE_FORBIDDEN). ADMIN/TU dikecualikan oleh kontrak, jadi baris mereka tetap tampil.
     */
    fun hiddenUstadzId(user: User): String? =
        if (user.ustadzId != null && !MenuResolver.mayRecordOwnUstadzAttendance(user.roles)) user.ustadzId else null

    fun excluding(people: List<RosterPerson>, hiddenId: String?): List<RosterPerson> =
        if (hiddenId == null) people else people.filterNot { it.id == hiddenId }

    /** Ringkasan untuk header layar: berapa hadir / tidak hadir / belum dicatat. */
    fun summarize(rows: List<RosterRow>): RosterSummary = RosterSummary(
        hadir = rows.count { it.entry?.hadir == true },
        tidakHadir = rows.count { it.entry?.hadir == false },
        belumDicatat = rows.count { it.entry == null },
    )
}

data class RosterSummary(val hadir: Int, val tidakHadir: Int, val belumDicatat: Int)
