package com.alhikmah.tpq.ui.beranda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alhikmah.tpq.domain.menu.MenuAccess
import com.alhikmah.tpq.domain.menu.MenuEntry
import com.alhikmah.tpq.domain.menu.MenuId
import com.alhikmah.tpq.domain.menu.MenuResolver
import com.alhikmah.tpq.domain.model.Ringkasan
import com.alhikmah.tpq.domain.model.SantriRingkas
import com.alhikmah.tpq.domain.model.StatusHadir
import com.alhikmah.tpq.domain.model.User
import com.alhikmah.tpq.domain.model.UstadzRingkas
import com.alhikmah.tpq.domain.model.Role

/**
 * Varian PIKET / USTADZ (mockup MobUstadz). Kontrak tidak punya data "kelas saya", jadi bagian itu diganti ringkasan
 * santri hari ini dengan tombol ke layar yang dibolehkan role (Absensi Santri, Perbarui progress).
 * Urutan: PIKET = kartu tugas piket, ustadz hari ini, santri hari ini; USTADZ saja = santri hari ini (kartu utama), ustadz hari ini.
 */
@Composable
internal fun TugasSections(user: User, ringkasan: Ringkasan, menu: List<MenuEntry>, allowed: Set<MenuId>, onOpen: (MenuId) -> Unit) {
    val piket = Role.PIKET in user.roles
    if (piket) {
        PiketCard(ringkasan.ustadz, MenuId.ABSENSI_USTADZ in allowed, onOpen)
        UstadzHariIniCard(user, ringkasan.ustadz, MenuId.ABSENSI_USTADZ in allowed, onOpen)
        SantriHariIniCard(ringkasan.santri, menu, user, utama = false, onOpen)
    } else {
        SantriHariIniCard(ringkasan.santri, menu, user, utama = true, onOpen)
        UstadzHariIniCard(user, ringkasan.ustadz, MenuId.ABSENSI_USTADZ in allowed, onOpen)
    }
    if (ringkasan.perhatian.isNotEmpty()) PerhatianCard(ringkasan.perhatian, allowed, onOpen)
}

/** Kartu tugas piket: "Catat absensi ustadz n/m" dengan batang kemajuan dan tombol lanjut. */
@Composable
private fun PiketCard(ustadz: UstadzRingkas, bisaBuka: Boolean, onOpen: (MenuId) -> Unit) {
    val redup = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
    val fraction = if (ustadz.total > 0) ustadz.tercatat.toFloat() / ustadz.total else 0f
    HeroCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Eyebrow("Tugas piket hari ini", color = redup)
                Text("Catat absensi ustadz", style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp))
            }
            Text(
                buildAnnotatedString {
                    append("${ustadz.tercatat}")
                    withStyle(SpanStyle(fontSize = 16.sp, color = redup)) { append("/${ustadz.total}") }
                },
                modifier = Modifier.clearAndSetSemantics { contentDescription = "${ustadz.tercatat} dari ${ustadz.total} ustadz sudah tercatat" },
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
            )
        }
        BatangKemajuan(fraction)
        Text(
            when {
                ustadz.total == 0 -> "Belum ada data ustadz."
                ustadz.belumDicatat > 0 -> "Tersisa ${ustadz.belumDicatat} ustadz belum tercatat"
                else -> "Semua ustadz sudah tercatat. Alhamdulillah."
            },
            style = MaterialTheme.typography.bodySmall,
        )
        if (bisaBuka) {
            HeroButton(if (ustadz.belumDicatat > 0) "Lanjutkan mencatat" else "Lihat absensi ustadz", { onOpen(MenuId.ABSENSI_USTADZ) })
        }
    }
}

/** Ringkasan ustadz hari ini (angka saja: daftar nama per ustadz tidak ada di /dashboard, ada di layar Absensi Ustadz). */
@Composable
private fun UstadzHariIniCard(user: User, ustadz: UstadzRingkas, bisaBuka: Boolean, onOpen: (MenuId) -> Unit) {
    SectionCard(spacing = 8) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Ustadz hari ini")
            if (bisaBuka) LinkAction("Lihat semua", { onOpen(MenuId.ABSENSI_USTADZ) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            AngkaLabel(ustadz.hadir, "hadir")
            AngkaLabel(ustadz.tidakHadir, "tidak hadir")
            AngkaLabel(ustadz.belumDicatat, "belum dicatat")
        }
        // Server melarang pencatatan atas nama sendiri kecuali ADMIN/TU, jadi catatan ini benar untuk akun varian ini.
        if (user.ustadzId != null && !MenuResolver.mayRecordOwnUstadzAttendance(user.roles)) {
            Text("Absensi Anda sendiri dicatat oleh piket lain.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Santri hari ini + aksi sesuai hak role. [utama] = tampil sebagai kartu hero (untuk USTADZ tanpa piket).
 * Aksi pertama bergaya utama, kedua bergaris.
 */
@Composable
private fun SantriHariIniCard(santri: SantriRingkas, menu: List<MenuEntry>, user: User, utama: Boolean, onOpen: (MenuId) -> Unit) {
    val body: @Composable ColumnScope.() -> Unit = {
        val redup = if (utama) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Eyebrow("Santri hari ini", color = redup)
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("${santri.hadir}") }
                        append(" dari ${santri.total} hadir")
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
                )
            }
            val lengkap = santri.belumDicatat == 0
            val label = if (lengkap) "Sudah lengkap" else "${santri.belumDicatat} belum dicatat"
            if (utama) {
                com.alhikmah.tpq.ui.common.Pill(label, MaterialTheme.colorScheme.onPrimary, MaterialTheme.colorScheme.primary)
            } else {
                StatusPill(label, if (lengkap) StatusHadir.HADIR else StatusHadir.BELUM_DICATAT)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            AngkaLabel(santri.hadir, "hadir", muted = redup)
            AngkaLabel(santri.tidakHadir, "tidak hadir", muted = redup)
            AngkaLabel(santri.belumDicatat, "belum dicatat", muted = redup)
        }
        val aksi = aksiSantri(menu, user)
        aksi.forEachIndexed { index, (label, menuId) ->
            val klik = { onOpen(menuId) }
            when {
                utama && index == 0 -> HeroButton(label, klik)
                utama -> HeroGhostAction(label, klik)
                index == 0 -> PrimaryAction(label, klik)
                else -> GhostAction(label, klik)
            }
        }
    }
    if (utama) HeroCard(content = body) else SectionCard(content = body)
}

/** Aksi yang tersedia di kartu santri: absensi (tulis/lihat) dan progress (ubah/lihat daftar) menurut [MenuResolver]. */
internal fun aksiSantri(menu: List<MenuEntry>, user: User): List<Pair<String, MenuId>> = buildList {
    menu.firstOrNull { it.id == MenuId.ABSENSI_SANTRI }?.let {
        add((if (it.access == MenuAccess.WRITE) "Catat absensi santri" else "Lihat absensi santri") to MenuId.ABSENSI_SANTRI)
    }
    if (menu.any { it.id == MenuId.DAFTAR_SANTRI }) {
        add((if (MenuResolver.canEditProgress(user.roles)) "Perbarui progress santri" else "Lihat daftar santri") to MenuId.DAFTAR_SANTRI)
    }
}
