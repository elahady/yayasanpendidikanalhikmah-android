package com.alhikmah.tpq.domain

import com.alhikmah.tpq.domain.api.ApiError
import com.alhikmah.tpq.domain.api.ApiException
import com.alhikmah.tpq.domain.api.AuthApi
import com.alhikmah.tpq.domain.api.LoginResult
import com.alhikmah.tpq.domain.fake.FakeData
import com.alhikmah.tpq.domain.fake.InMemoryTokenStore
import com.alhikmah.tpq.domain.model.User
import com.alhikmah.tpq.domain.session.SessionManager
import com.alhikmah.tpq.domain.session.SessionState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class SessionManagerTest {

    private class StubAuth(var meResult: () -> User = { FakeData.userAdmin }) : AuthApi {
        override suspend fun login(username: String, password: String): LoginResult {
            if (password == "salah") throw ApiException(ApiError.fromResponse(401, """{"error":{"code":"UNAUTHENTICATED","message":"Username atau password salah."}}"""))
            return LoginResult("jwt", FakeData.userAdmin)
        }

        override suspend fun me(): User = meResult()
    }

    @Test
    fun `tanpa token langsung ke login tanpa memanggil server`() = runTest {
        val auth = StubAuth(meResult = { error("tidak boleh dipanggil") })
        val session = SessionManager(auth, InMemoryTokenStore())
        session.restore()
        assertEquals(SessionState.LoggedOut(), session.state.value)
    }

    @Test
    fun `token valid memulihkan sesi lewat me`() = runTest {
        val session = SessionManager(StubAuth(), InMemoryTokenStore("jwt"))
        session.restore()
        assertEquals(SessionState.LoggedIn(FakeData.userAdmin), session.state.value)
    }

    @Test
    fun `401 saat cek sesi membuang token dan kembali ke login`() = runTest {
        val tokens = InMemoryTokenStore("jwt")
        val session = SessionManager(StubAuth(meResult = { throw ApiException(ApiError.fromResponse(401, null)) }), tokens)
        session.restore()
        assertEquals(SessionState.LoggedOut(SessionManager.EXPIRED_NOTICE), session.state.value)
        assertNull(tokens.get())
    }

    @Test
    fun `jaringan putus saat cek sesi tidak membuang token`() = runTest {
        val tokens = InMemoryTokenStore("jwt")
        val session = SessionManager(StubAuth(meResult = { throw ApiException(ApiError.network()) }), tokens)
        session.restore()
        assertIs<SessionState.Unreachable>(session.state.value)
        assertEquals("jwt", tokens.get())
    }

    @Test
    fun `login menyimpan token, gagal tidak mengubah apa pun`() = runTest {
        val tokens = InMemoryTokenStore()
        val session = SessionManager(StubAuth(), tokens)
        val e = assertFailsWith<ApiException> { session.login("admin", "salah") }
        assertEquals("Username atau password salah.", e.error.message)
        assertNull(tokens.get())

        session.login("  admin ", "benar")
        assertEquals("jwt", tokens.get())
        assertIs<SessionState.LoggedIn>(session.state.value)
    }

    @Test
    fun `401 pada permintaan biasa dan logout kembali ke login`() = runTest {
        val tokens = InMemoryTokenStore()
        val session = SessionManager(StubAuth(), tokens)
        session.login("admin", "benar")
        session.onUnauthorized()
        assertEquals(SessionState.LoggedOut(SessionManager.EXPIRED_NOTICE), session.state.value)
        assertNull(tokens.get())

        session.login("admin", "benar")
        session.logout()
        assertEquals(SessionState.LoggedOut(), session.state.value)
        assertNull(tokens.get())
    }
}
