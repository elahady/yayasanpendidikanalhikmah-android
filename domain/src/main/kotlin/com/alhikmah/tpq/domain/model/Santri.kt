package com.alhikmah.tpq.domain.model

enum class Jenjang(val label: String) {
    ULA("Ula"),
    WUSTHO("Wustho"),
    ;

    companion object {
        fun fromApi(value: String?): Jenjang? = entries.firstOrNull { it.name == value }
    }
}

/** Progress Al-Qur'an metode An-Nahdhiyah: pra-jilid, jilid 1-6, lalu Al-Qur'an. Urutan enum = urutan belajar. */
enum class Jilid(val label: String) {
    PRA_NAHDLIYAH("Pra Nahdhiyah"),
    JILID_1("Jilid 1"),
    JILID_2("Jilid 2"),
    JILID_3("Jilid 3"),
    JILID_4("Jilid 4"),
    JILID_5("Jilid 5"),
    JILID_6("Jilid 6"),
    AL_QURAN("Al-Qur'an"),
    ;

    companion object {
        fun fromApi(value: String?): Jilid? = entries.firstOrNull { it.name == value }
    }
}

data class Santri(
    val id: String,
    val nama: String,
    val jenjang: Jenjang?,
    val kelas: String?,
    val jilidSaatIni: Jilid?,
    val catatanProgressMadin: String?,
) {
    /** Contoh: "Ula - Kelas 3". */
    val keteranganKelas: String
        get() = listOfNotNull(jenjang?.label, kelas?.takeIf { it.isNotBlank() }?.let { "Kelas $it" }).joinToString(" - ")

    /** Contoh: "Jenjang Ula · Kelas 3" (kartu anak di Beranda wali); kosong bila keduanya tidak ada. */
    val keteranganJenjangKelas: String
        get() = listOfNotNull(jenjang?.label?.let { "Jenjang $it" }, kelas?.takeIf { it.isNotBlank() }?.let { "Kelas $it" }).joinToString(" · ")
}

data class Ustadz(
    val id: String,
    val nama: String,
    val mapelDiajar: String?,
)

/** Perubahan progress untuk PATCH /santri/{id}/progress. Hanya field yang diminta yang dikirim. */
data class ProgressUpdate(
    val jilid: Jilid? = null,
    /** true bila [catatan] ikut dikirim (termasuk null yang berarti menghapus catatan). */
    val updateCatatan: Boolean = false,
    val catatan: String? = null,
) {
    val isEmpty: Boolean get() = jilid == null && !updateCatatan
}
