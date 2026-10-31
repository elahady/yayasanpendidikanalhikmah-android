package com.alhikmah.tpq.domain.session

import com.alhikmah.tpq.domain.api.ApiError
import com.alhikmah.tpq.domain.api.ApiException
import com.alhikmah.tpq.domain.api.AuthApi
import com.alhikmah.tpq.domain.api.TokenStore
import com.alhikmah.tpq.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SessionState {
    /** Sedang memeriksa token tersimpan lewat GET /me saat app dibuka. */
    data object Checking : SessionState

    /** Belum masuk. [notice] terisi bila keluar karena sesi berakhir (401). */
    data class LoggedOut(val notice: String? = null) : SessionState

    data class LoggedIn(val user: User) : SessionState

    /** Token ada tetapi server tidak terjangkau; sesi tidak dibuang, pengguna bisa mencoba lagi. */
    data class Unreachable(val error: ApiError) : SessionState
}

/**
 * Pemilik status sesi: login, cek sesi saat app dibuka, dan kembali ke login saat server menjawab 401.
 * Tidak ada refresh token (kontrak): token berlaku 30 hari lalu harus masuk ulang.
 */
class SessionManager(
    private val auth: AuthApi,
    private val tokens: TokenStore,
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Checking)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    /** Dipakai klien HTTP untuk header Authorization. */
    fun token(): String? = tokens.get()

    /** Cek sesi saat app dibuka (atau saat "Coba lagi"): tanpa token langsung ke login, selain itu GET /me. */
    suspend fun restore() {
        if (tokens.get() == null) {
            _state.value = SessionState.LoggedOut()
            return
        }
        _state.value = SessionState.Checking
        try {
            _state.value = SessionState.LoggedIn(auth.me())
        } catch (e: ApiException) {
            if (e.error.isUnauthenticated) {
                tokens.clear()
                _state.value = SessionState.LoggedOut(EXPIRED_NOTICE)
            } else {
                _state.value = SessionState.Unreachable(e.error)
            }
        }
    }

    /** Melempar [ApiException] bila gagal (mis. username/password salah); layar login menampilkan pesannya. */
    suspend fun login(username: String, password: String) {
        val result = auth.login(username.trim(), password)
        tokens.save(result.accessToken)
        _state.value = SessionState.LoggedIn(result.user)
    }

    fun logout() {
        tokens.clear()
        _state.value = SessionState.LoggedOut()
    }

    /** Dipanggil klien HTTP saat ada jawaban 401 pada permintaan bertoken: token dibuang, layar kembali ke login. */
    fun onUnauthorized() {
        if (_state.value is SessionState.LoggedOut) return
        tokens.clear()
        _state.value = SessionState.LoggedOut(EXPIRED_NOTICE)
    }

    companion object {
        const val EXPIRED_NOTICE = "Sesi Anda berakhir. Silakan masuk lagi."
    }
}
