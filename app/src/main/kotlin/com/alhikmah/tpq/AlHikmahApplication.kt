package com.alhikmah.tpq

import android.app.Application
import androidx.compose.runtime.staticCompositionLocalOf
import com.alhikmah.tpq.data.ApiClient
import com.alhikmah.tpq.data.HttpAuthApi
import com.alhikmah.tpq.data.HttpTpqRepository
import com.alhikmah.tpq.data.KeystoreTokenStore
import com.alhikmah.tpq.domain.api.TpqRepository
import com.alhikmah.tpq.domain.session.SessionManager

/** Penampung dependensi (DI manual, sama dengan pola Rizqflow). Dibuat sekali di [AlHikmahApplication]. */
class AppContainer(application: Application) {
    private val tokenStore = KeystoreTokenStore(application)

    private val apiClient = ApiClient(
        baseUrl = BuildConfig.API_BASE_URL,
        token = { tokenStore.get() },
        onUnauthorized = { session.onUnauthorized() },
    )

    val session: SessionManager = SessionManager(HttpAuthApi(apiClient), tokenStore)

    val repository: TpqRepository = HttpTpqRepository(apiClient)
}

class AlHikmahApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Disediakan di MainActivity; layar mengambil dependensi dari sini (preview tidak memakainya). */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer belum disediakan") }
