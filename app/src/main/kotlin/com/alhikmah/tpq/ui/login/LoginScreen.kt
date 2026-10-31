package com.alhikmah.tpq.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alhikmah.tpq.LocalAppContainer
import com.alhikmah.tpq.domain.session.SessionManager
import com.alhikmah.tpq.domain.util.Outcome
import com.alhikmah.tpq.domain.util.attempt
import com.alhikmah.tpq.ui.common.BrandLogo
import com.alhikmah.tpq.ui.theme.AlHikmahTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val busy: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean get() = username.isNotBlank() && password.isNotEmpty() && !busy
}

class LoginViewModel(private val session: SessionManager) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onUsername(value: String) = _state.update { it.copy(username = value, error = null) }

    fun onPassword(value: String) = _state.update { it.copy(password = value, error = null) }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            // Sukses: SessionManager mengganti status ke LoggedIn dan AppRoot berpindah layar sendiri.
            when (val result = attempt { session.login(current.username, current.password) }) {
                is Outcome.Ok -> _state.update { it.copy(busy = false, password = "") }
                is Outcome.Err -> _state.update { it.copy(busy = false, error = result.error.message) }
            }
        }
    }
}

@Composable
fun LoginScreen(notice: String?) {
    val container = LocalAppContainer.current
    val vm: LoginViewModel = viewModel(factory = viewModelFactory { initializer { LoginViewModel(container.session) } })
    val state by vm.state.collectAsStateWithLifecycle()
    LoginContent(state, notice, vm::onUsername, vm::onPassword, vm::submit)
}

@Composable
fun LoginContent(
    state: LoginUiState,
    notice: String?,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val focus = LocalFocusManager.current
    var showPassword by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(24.dp))
            BrandLogo(120.dp)
            Spacer(Modifier.height(16.dp))
            Text("Yayasan Pendidikan Al-Hikmah", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                "TPQ & Madrasah Diniyah",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            if (notice != null) {
                Text(notice, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
            }

            OutlinedTextField(
                value = state.username,
                onValueChange = onUsername,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Username") },
                singleLine = true,
                enabled = !state.busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = onPassword,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                singleLine = true,
                enabled = !state.busy,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focus.clearFocus()
                    onSubmit()
                }),
                trailingIcon = {
                    TextButton(onClick = { showPassword = !showPassword }) { Text(if (showPassword) "Sembunyikan" else "Tampilkan") }
                },
            )
            if (state.error != null) {
                Text(
                    state.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = onSubmit, enabled = state.canSubmit, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                if (state.busy) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Text("Masuk...")
                    }
                } else {
                    Text("Masuk")
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Akun dibuat oleh admin yayasan.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, name = "Login")
@Composable
private fun LoginPreview() {
    AlHikmahTheme(darkTheme = false) { LoginContent(LoginUiState(username = "ustadz.hasan", password = "rahasia"), null, {}, {}, {}) }
}

@Preview(showBackground = true, name = "Login gelap + error")
@Composable
private fun LoginDarkErrorPreview() {
    AlHikmahTheme(darkTheme = true) {
        LoginContent(LoginUiState(username = "x", password = "y", error = "Username atau password salah."), "Sesi Anda berakhir. Silakan masuk lagi.", {}, {}, {})
    }
}
