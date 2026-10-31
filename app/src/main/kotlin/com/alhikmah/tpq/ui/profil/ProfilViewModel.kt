package com.alhikmah.tpq.ui.profil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhikmah.tpq.domain.api.TpqRepository
import com.alhikmah.tpq.domain.profil.PasswordForm
import com.alhikmah.tpq.domain.util.Outcome
import com.alhikmah.tpq.domain.util.attempt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Isian dialog Ubah password. [galat] = pesan validasi lokal atau pesan server (apa adanya). */
data class UbahPasswordState(
    val tampil: Boolean = false,
    val lama: String = "",
    val baru: String = "",
    val ulangi: String = "",
    val galat: String? = null,
    val menyimpan: Boolean = false,
)

data class ProfilUiState(
    val ubahPassword: UbahPasswordState = UbahPasswordState(),
    /** Pesan sekali tampil (mis. "Password berhasil diubah."); dibaca layar lalu [ProfilViewModel.pesanDibaca]. */
    val pesan: String? = null,
)

class ProfilViewModel(private val repo: TpqRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProfilUiState())
    val state: StateFlow<ProfilUiState> = _state.asStateFlow()

    fun bukaUbahPassword() = _state.update { it.copy(ubahPassword = UbahPasswordState(tampil = true)) }

    fun tutupUbahPassword() = _state.update { it.copy(ubahPassword = UbahPasswordState()) }

    fun onLama(text: String) = ubah { it.copy(lama = text, galat = null) }

    fun onBaru(text: String) = ubah { it.copy(baru = text, galat = null) }

    fun onUlangi(text: String) = ubah { it.copy(ulangi = text, galat = null) }

    fun pesanDibaca() = _state.update { it.copy(pesan = null) }

    /**
     * Validasi lokal ringan ([PasswordForm]), lalu kirim. Galat server (mis. 422 "Password sekarang salah.", 429)
     * ditampilkan di dialog apa adanya; 422 tidak memicu logout (hanya 401 yang memicu, di ApiClient).
     */
    fun kirim() {
        val form = _state.value.ubahPassword
        if (form.menyimpan) return
        PasswordForm.validate(form.lama, form.baru, form.ulangi)?.let { pesan ->
            ubah { it.copy(galat = pesan) }
            return
        }
        ubah { it.copy(menyimpan = true, galat = null) }
        viewModelScope.launch {
            when (val hasil = attempt { repo.ubahPassword(form.lama, form.baru) }) {
                is Outcome.Ok -> _state.update { ProfilUiState(pesan = "Password berhasil diubah.") }
                is Outcome.Err -> ubah { it.copy(menyimpan = false, galat = hasil.error.message) }
            }
        }
    }

    private fun ubah(change: (UbahPasswordState) -> UbahPasswordState) =
        _state.update { it.copy(ubahPassword = change(it.ubahPassword)) }
}
