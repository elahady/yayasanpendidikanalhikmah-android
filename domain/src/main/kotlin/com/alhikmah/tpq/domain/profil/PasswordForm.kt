package com.alhikmah.tpq.domain.profil

/**
 * Validasi lokal ringan untuk form Ubah password; aturan lain (beda dari password lama, password lama benar,
 * batas percobaan) ditentukan server dan pesannya ditampilkan apa adanya.
 */
object PasswordForm {
    const val MIN = 8
    const val MAX = 72

    /** Pesan galat Indonesia untuk pengguna, atau null bila isian lolos pemeriksaan lokal. */
    fun validate(lama: String, baru: String, ulangi: String): String? = when {
        lama.isEmpty() -> "Isi password Anda saat ini."
        baru.length < MIN || baru.length > MAX -> "Password baru harus $MIN sampai $MAX karakter."
        baru != ulangi -> "Ulangi password baru dengan isian yang sama."
        else -> null
    }
}
