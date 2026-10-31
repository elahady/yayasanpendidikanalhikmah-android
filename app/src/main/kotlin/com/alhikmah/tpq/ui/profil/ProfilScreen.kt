package com.alhikmah.tpq.ui.profil

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alhikmah.tpq.BuildConfig
import com.alhikmah.tpq.LocalAppContainer
import com.alhikmah.tpq.domain.fake.FakeData
import com.alhikmah.tpq.domain.model.User
import com.alhikmah.tpq.domain.tema.ThemeMode
import com.alhikmah.tpq.domain.util.Inisial
import com.alhikmah.tpq.domain.util.Tanggal
import com.alhikmah.tpq.ui.beranda.SectionCard
import com.alhikmah.tpq.ui.beranda.SectionTitle
import com.alhikmah.tpq.ui.common.Pill
import com.alhikmah.tpq.ui.common.roleLabel
import com.alhikmah.tpq.ui.theme.AlHikmahTheme
import com.alhikmah.tpq.ui.theme.statusColors

/** Layar Profil: identitas, pilihan tampilan (disimpan), akun (ubah password, info), dan Keluar. */
@Composable
fun ProfilScreen(user: User, onLogout: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: ProfilViewModel = viewModel(
        key = "profil-${user.id}",
        factory = viewModelFactory { initializer { ProfilViewModel(container.repository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val mode by container.themeSettings.mode.collectAsStateWithLifecycle()
    ProfilContent(
        user = user,
        versi = BuildConfig.VERSION_NAME,
        mode = mode,
        state = state,
        onMode = container.themeSettings::set,
        onBukaUbahPassword = vm::bukaUbahPassword,
        onTutupUbahPassword = vm::tutupUbahPassword,
        onLama = vm::onLama,
        onBaru = vm::onBaru,
        onUlangi = vm::onUlangi,
        onKirim = vm::kirim,
        onPesanDibaca = vm::pesanDibaca,
        onLogout = onLogout,
    )
}

@Composable
fun ProfilContent(
    user: User,
    versi: String,
    mode: ThemeMode,
    state: ProfilUiState,
    onMode: (ThemeMode) -> Unit,
    onBukaUbahPassword: () -> Unit,
    onTutupUbahPassword: () -> Unit,
    onLama: (String) -> Unit,
    onBaru: (String) -> Unit,
    onUlangi: (String) -> Unit,
    onKirim: () -> Unit,
    onPesanDibaca: () -> Unit,
    onLogout: () -> Unit,
) {
    var konfirmasiKeluar by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.pesan) {
        state.pesan?.let {
            snackbar.showSnackbar(it)
            onPesanDibaca()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Profil", style = MaterialTheme.typography.headlineMedium)
            Identitas(user)
            Tampilan(mode, onMode)
            Akun(user, versi, onBukaUbahPassword)
            OutlinedButton(
                onClick = { konfirmasiKeluar = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text("Keluar dari akun", color = MaterialTheme.colorScheme.secondary) }
        }
    }

    if (state.ubahPassword.tampil) {
        UbahPasswordDialog(state.ubahPassword, onLama, onBaru, onUlangi, onKirim, onTutupUbahPassword)
    }
    if (konfirmasiKeluar) {
        AlertDialog(
            onDismissRequest = { konfirmasiKeluar = false },
            title = { Text("Keluar dari akun?") },
            text = { Text("Anda perlu memasukkan username dan password lagi untuk masuk.") },
            confirmButton = { TextButton(onClick = { konfirmasiKeluar = false; onLogout() }) { Text("Keluar") } },
            dismissButton = { TextButton(onClick = { konfirmasiKeluar = false }) { Text("Batal") } },
        )
    }
}

@Composable
private fun Identitas(user: User) {
    SectionCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(64.dp).background(MaterialTheme.statusColors.aksen, CircleShape), contentAlignment = Alignment.Center) {
                Text(
                    Inisial.dari(user.nama),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp),
                    color = MaterialTheme.statusColors.onAksen,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Column {
                    Text(user.nama, style = MaterialTheme.typography.titleLarge)
                    if (user.username != null) {
                        Text("@${user.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    user.roles.sortedBy { it.ordinal }.forEach { role ->
                        Pill(roleLabel(role), MaterialTheme.statusColors.hadirContainer, MaterialTheme.statusColors.onHadirContainer)
                    }
                }
            }
        }
    }
}

@Composable
private fun Tampilan(mode: ThemeMode, onMode: (ThemeMode) -> Unit) {
    SectionCard {
        Column {
            SectionTitle("Tampilan")
            Text("\"Ikuti sistem\" mengikuti pengaturan HP Anda.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(50))
                .padding(4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ThemeMode.entries.forEach { pilihan ->
                val aktif = pilihan == mode
                Box(
                    Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(50))
                        .background(if (aktif) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .selectable(selected = aktif, onClick = { onMode(pilihan) }, role = Role.RadioButton),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        pilihan.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (aktif) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Akun(user: User, versi: String, onUbahPassword: () -> Unit) {
    SectionCard(spacing = 0) {
        SectionTitle("Akun", Modifier.padding(bottom = 8.dp))
        BarisAkun(Icons.Default.Lock, "Ubah password", onClick = onUbahPassword) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (user.ustadzNama != null) {
            BarisAkun(Icons.Default.Person, "Ustadz tertaut") { NilaiAkun(user.ustadzNama.orEmpty()) }
        }
        Tanggal.panjangDariServer(user.dibuatPada)?.let { dibuat ->
            BarisAkun(Icons.Default.DateRange, "Akun dibuat") { NilaiAkun(dibuat) }
        }
        BarisAkun(Icons.Default.Info, "Versi aplikasi") { NilaiAkun(versi) }
    }
}

@Composable
private fun NilaiAkun(teks: String) {
    Text(teks, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun BarisAkun(icon: ImageVector, label: String, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        val klik = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        Row(
            klik.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp))
            trailing()
        }
    }
}

@Composable
private fun UbahPasswordDialog(
    form: UbahPasswordState,
    onLama: (String) -> Unit,
    onBaru: (String) -> Unit,
    onUlangi: (String) -> Unit,
    onKirim: () -> Unit,
    onTutup: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!form.menyimpan) onTutup() },
        title = { Text("Ubah password") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KolomPassword("Password sekarang", form.lama, onLama, form.menyimpan)
                KolomPassword("Password baru (8 sampai 72 karakter)", form.baru, onBaru, form.menyimpan)
                KolomPassword("Ulangi password baru", form.ulangi, onUlangi, form.menyimpan)
                if (form.galat != null) {
                    Text(form.galat, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onKirim, enabled = !form.menyimpan, modifier = Modifier.heightIn(min = 48.dp)) {
                if (form.menyimpan) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Simpan")
                }
            }
        },
        dismissButton = { TextButton(onClick = onTutup, enabled = !form.menyimpan, modifier = Modifier.heightIn(min = 48.dp)) { Text("Batal") } },
    )
}

@Composable
private fun KolomPassword(label: String, nilai: String, onChange: (String) -> Unit, terkunci: Boolean) {
    OutlinedTextField(
        value = nilai,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        enabled = !terkunci,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
}

// ---------------------------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------------------------

@Composable
private fun PreviewProfil(user: User, mode: ThemeMode, gelap: Boolean, state: ProfilUiState = ProfilUiState()) {
    AlHikmahTheme(darkTheme = gelap) {
        ProfilContent(user, "0.1.0", mode, state, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 900, name = "Profil terang")
@Composable
private fun ProfilTerangPreview() = PreviewProfil(FakeData.userPiketUstadz, ThemeMode.TERANG, gelap = false)

@Preview(showBackground = true, heightDp = 900, name = "Profil gelap (ikuti sistem)")
@Composable
private fun ProfilGelapPreview() = PreviewProfil(FakeData.userPiketUstadz, ThemeMode.SISTEM, gelap = true)

@Preview(showBackground = true, heightDp = 900, name = "Dialog ubah password galat")
@Composable
private fun UbahPasswordPreview() = PreviewProfil(
    FakeData.userWali,
    ThemeMode.GELAP,
    gelap = true,
    state = ProfilUiState(UbahPasswordState(tampil = true, lama = "salah", baru = "baru12345", ulangi = "baru12345", galat = "Password sekarang salah.")),
)
