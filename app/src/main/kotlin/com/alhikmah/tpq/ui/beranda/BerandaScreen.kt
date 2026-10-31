package com.alhikmah.tpq.ui.beranda

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alhikmah.tpq.domain.fake.FakeData
import com.alhikmah.tpq.domain.menu.MenuAccess
import com.alhikmah.tpq.domain.menu.MenuEntry
import com.alhikmah.tpq.domain.menu.MenuId
import com.alhikmah.tpq.domain.menu.MenuResolver
import com.alhikmah.tpq.domain.model.Role
import com.alhikmah.tpq.domain.model.User
import com.alhikmah.tpq.ui.common.BrandLogo
import com.alhikmah.tpq.ui.common.EmptyBox
import com.alhikmah.tpq.ui.common.Pill
import com.alhikmah.tpq.ui.theme.AlHikmahTheme

/** Judul, penjelasan singkat, dan ikon tiap menu (aturan siapa melihat apa ada di [MenuResolver]). */
private data class MenuMeta(val title: String, val description: String, val icon: ImageVector)

private fun meta(id: MenuId): MenuMeta = when (id) {
    MenuId.REKAP_HARIAN -> MenuMeta("Rekap Harian", "Ringkasan kehadiran santri dan ustadz per hari", Icons.Default.DateRange)
    MenuId.ABSENSI_USTADZ -> MenuMeta("Absensi Ustadz", "Kehadiran ustadz/ustadzah per hari", Icons.Default.Person)
    MenuId.ABSENSI_SANTRI -> MenuMeta("Absensi Santri", "Kehadiran santri per hari", Icons.Default.CheckCircle)
    MenuId.DAFTAR_SANTRI -> MenuMeta("Daftar Santri", "Data santri dan progress belajarnya", Icons.AutoMirrored.Filled.List)
    MenuId.ANAK_SAYA -> MenuMeta("Anak Saya", "Riwayat kehadiran dan progress anak", Icons.Default.Face)
}

fun roleLabel(role: Role): String = when (role) {
    Role.ADMIN -> "Admin"
    Role.TU -> "Tata Usaha"
    Role.PENGAWAS -> "Pengawas"
    Role.PIKET -> "Piket"
    Role.USTADZ -> "Ustadz"
    Role.WALI -> "Wali Santri"
}

@Composable
fun BerandaScreen(user: User, onOpen: (MenuId) -> Unit, onLogout: () -> Unit) {
    BerandaContent(user, MenuResolver.menuFor(user.roles), onOpen, onLogout)
}

@Composable
fun BerandaContent(user: User, menu: List<MenuEntry>, onOpen: (MenuId) -> Unit, onLogout: () -> Unit) {
    var confirmLogout by remember { mutableStateOf(false) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandLogo(44.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("YAYASAN PENDIDIKAN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Al-Hikmah", style = MaterialTheme.typography.titleLarge)
                    }
                    TextButton(onClick = { confirmLogout = true }) { Text("Keluar") }
                }
            }
            item {
                Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Assalamu'alaikum,", style = MaterialTheme.typography.bodyMedium)
                        Text(user.nama, style = MaterialTheme.typography.headlineSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            user.roles.sortedBy { it.ordinal }.forEach { role ->
                                Pill(roleLabel(role), MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f), MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
            if (menu.isEmpty()) {
                item { Box(Modifier.height(240.dp)) { EmptyBox("Belum ada menu", "Akun Anda belum punya hak akses. Hubungi admin yayasan.") } }
            } else {
                items(menu, key = { it.id }) { entry -> MenuCard(entry, onClick = { onOpen(entry.id) }) }
            }
        }
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Keluar dari akun?") },
            text = { Text("Anda perlu memasukkan username dan password lagi untuk masuk.") },
            confirmButton = { TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("Keluar") } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Batal") } },
        )
    }
}

@Composable
private fun MenuCard(entry: MenuEntry, onClick: () -> Unit) {
    val meta = meta(entry.id)
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, shape = CircleShape, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(meta.icon, contentDescription = null) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(meta.title, style = MaterialTheme.typography.titleMedium)
                Text(meta.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (entry.access == MenuAccess.READ && entry.id != MenuId.REKAP_HARIAN && entry.id != MenuId.ANAK_SAYA) {
                Spacer(Modifier.width(8.dp))
                Pill("Lihat saja", MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(showBackground = true, name = "Beranda piket+ustadz")
@Composable
private fun BerandaPreview() {
    val user = FakeData.userPiketUstadz
    AlHikmahTheme(darkTheme = false) { BerandaContent(user, MenuResolver.menuFor(user.roles), {}, {}) }
}

@Preview(showBackground = true, name = "Beranda pengawas gelap")
@Composable
private fun BerandaPengawasDarkPreview() {
    val user = FakeData.userPengawas
    AlHikmahTheme(darkTheme = true) { BerandaContent(user, MenuResolver.menuFor(user.roles), {}, {}) }
}

@Preview(showBackground = true, name = "Beranda wali")
@Composable
private fun BerandaWaliPreview() {
    val user = FakeData.userWali
    AlHikmahTheme(darkTheme = false) { BerandaContent(user, MenuResolver.menuFor(user.roles), {}, {}) }
}
