package com.alhikmah.tpq.domain.tema

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Pilihan tampilan di layar Profil. [label] sama dengan teks di mockup. */
enum class ThemeMode(val label: String) {
    SISTEM("Ikuti sistem"),
    TERANG("Terang"),
    GELAP("Gelap"),
    ;

    /** Apakah tema gelap dipakai, diberi tahu [sistemGelap] (pengaturan HP). */
    fun gelap(sistemGelap: Boolean): Boolean = when (this) {
        SISTEM -> sistemGelap
        TERANG -> false
        GELAP -> true
    }

    companion object {
        /** Nilai tak dikenal/kosong (mis. data rusak) jatuh ke [SISTEM]. */
        fun fromStored(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: SISTEM
    }
}

/** Penyimpanan kecil pilihan tema (di Android: SharedPreferences biasa; bukan rahasia). */
interface ThemeStore {
    fun load(): String?

    fun save(value: String)
}

/**
 * Pemilik pilihan tema: dibaca dari [store] saat dibuat, dipublikasikan lewat [mode], dan disimpan setiap kali berubah.
 * Tidak bergantung Android supaya bisa diuji di JVM.
 */
class ThemeSettings(private val store: ThemeStore) {
    private val _mode = MutableStateFlow(ThemeMode.fromStored(store.load()))
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun set(mode: ThemeMode) {
        if (_mode.value == mode) return
        _mode.value = mode
        store.save(mode.name)
    }
}

class InMemoryThemeStore(private var value: String? = null) : ThemeStore {
    override fun load(): String? = value

    override fun save(value: String) {
        this.value = value
    }
}
