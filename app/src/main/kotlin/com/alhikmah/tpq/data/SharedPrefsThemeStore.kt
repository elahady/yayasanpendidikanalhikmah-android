package com.alhikmah.tpq.data

import android.content.Context
import com.alhikmah.tpq.domain.tema.ThemeStore

/** Pilihan tema di SharedPreferences biasa (bukan rahasia, jadi tanpa enkripsi seperti token). */
class SharedPrefsThemeStore(context: Context) : ThemeStore {
    private val prefs = context.applicationContext.getSharedPreferences("tampilan", Context.MODE_PRIVATE)

    override fun load(): String? = prefs.getString(KEY, null)

    override fun save(value: String) {
        prefs.edit().putString(KEY, value).apply()
    }

    private companion object {
        const val KEY = "tema"
    }
}
