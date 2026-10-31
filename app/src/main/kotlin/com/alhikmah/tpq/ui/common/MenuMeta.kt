package com.alhikmah.tpq.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.alhikmah.tpq.domain.menu.MenuAccess
import com.alhikmah.tpq.domain.menu.MenuEntry
import com.alhikmah.tpq.domain.menu.MenuId
import com.alhikmah.tpq.domain.model.Role

/** Judul, penjelasan singkat, dan ikon tiap menu (aturan siapa melihat apa ada di `MenuResolver`). */
data class MenuMeta(val title: String, val description: String, val icon: ImageVector)

fun menuMeta(id: MenuId): MenuMeta = when (id) {
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

/** Kartu menu berbentuk baris (dipakai layar tab Absensi). */
@Composable
fun MenuListCard(entry: MenuEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val meta = menuMeta(entry.id)
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
