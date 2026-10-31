package com.alhikmah.tpq.ui.beranda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhikmah.tpq.domain.api.TpqRepository
import com.alhikmah.tpq.domain.model.Dashboard
import com.alhikmah.tpq.domain.util.Load
import com.alhikmah.tpq.domain.util.Outcome
import com.alhikmah.tpq.domain.util.attempt
import com.alhikmah.tpq.domain.util.toLoad
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BerandaUiState(
    val load: Load<Dashboard> = Load.Loading,
    /** true saat tarik-untuk-muat-ulang berjalan; data lama tetap tampil. */
    val refreshing: Boolean = false,
    /** Pesan server bila muat ulang gagal padahal data lama masih ada. */
    val refreshError: String? = null,
    /** Anak yang dipilih (id santri); null = anak pertama. */
    val anakTerpilih: String? = null,
)

/**
 * Memuat `GET /dashboard` (tanggal = hari ini menurut server). Muat pertama menampilkan loading penuh; muat ulang
 * ([segarkan]) menjaga data lama di layar dan hanya menampilkan pesan bila gagal.
 */
class BerandaViewModel(private val repo: TpqRepository) : ViewModel() {
    private val _state = MutableStateFlow(BerandaUiState())
    val state: StateFlow<BerandaUiState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        muat()
    }

    /** Muat penuh (loading di layar), dipakai awal dan tombol "Coba lagi". */
    fun muat() {
        job?.cancel()
        _state.update { it.copy(load = Load.Loading, refreshing = false, refreshError = null) }
        job = viewModelScope.launch {
            val load = attempt { repo.dashboard() }.toLoad()
            _state.update { it.copy(load = load) }
        }
    }

    /**
     * Muat ulang dengan data lama tetap tampil. Sedang memuat pertama kali = abaikan (jangan memulai ulang);
     * sebelumnya gagal (belum ada data) = sama dengan [muat].
     */
    fun segarkan() {
        when (_state.value.load) {
            Load.Loading -> return
            is Load.Failure -> {
                muat()
                return
            }
            is Load.Success -> Unit
        }
        job?.cancel()
        _state.update { it.copy(refreshing = true, refreshError = null) }
        job = viewModelScope.launch {
            when (val result = attempt { repo.dashboard() }) {
                is Outcome.Ok -> _state.update { it.copy(load = Load.Success(result.value), refreshing = false) }
                is Outcome.Err -> _state.update { it.copy(refreshing = false, refreshError = result.error.message) }
            }
        }
    }

    fun pilihAnak(id: String) = _state.update { it.copy(anakTerpilih = id) }

    fun tutupGalatSegar() = _state.update { it.copy(refreshError = null) }
}
