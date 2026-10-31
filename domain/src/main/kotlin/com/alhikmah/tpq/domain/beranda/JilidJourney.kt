package com.alhikmah.tpq.domain.beranda

import com.alhikmah.tpq.domain.model.Jilid

enum class StepState { SELESAI, SEKARANG, BELUM }

/** Satu tahap pada "perjalanan jilid": [singkat] dipakai di bawah titik (Pra, J1..J6, Qur'an). */
data class JilidStep(val jilid: Jilid, val singkat: String, val state: StepState)

/** Delapan tahap An-Nahdhiyah (urutan enum [Jilid] = urutan belajar). */
object JilidJourney {

    fun singkat(jilid: Jilid): String = when (jilid) {
        Jilid.PRA_NAHDLIYAH -> "Pra"
        Jilid.AL_QURAN -> "Qur'an"
        else -> "J" + jilid.name.removePrefix("JILID_")
    }

    /** Tahap sebelum [current] = selesai, [current] = sekarang, sesudahnya = belum. [current] null (belum dicatat) = semua belum. */
    fun steps(current: Jilid?): List<JilidStep> = Jilid.entries.map { j ->
        val state = when {
            current == null -> StepState.BELUM
            j.ordinal < current.ordinal -> StepState.SELESAI
            j == current -> StepState.SEKARANG
            else -> StepState.BELUM
        }
        JilidStep(j, singkat(j), state)
    }

    /** Tahap berikutnya; null bila sudah Al-Qur'an atau jilid belum dicatat. */
    fun next(current: Jilid?): Jilid? = current?.let { Jilid.entries.getOrNull(it.ordinal + 1) }

    /** Kalimat ringkas di bawah perjalanan jilid. */
    fun ringkasan(current: Jilid?): String {
        if (current == null) return "Jilid belum dicatat oleh ustadz."
        val berikutnya = next(current) ?: return "Sekarang di ${current.label}, tahap akhir An-Nahdhiyah."
        return "Sekarang di ${current.label}. Tahap berikutnya ${berikutnya.label}."
    }
}
