package com.alhikmah.tpq.ui.beranda

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alhikmah.tpq.domain.beranda.UstadzHariIniBeranda
import com.alhikmah.tpq.domain.beranda.UstadzHariIniTampil
import com.alhikmah.tpq.domain.menu.MenuAccess
import com.alhikmah.tpq.domain.menu.MenuEntry
import com.alhikmah.tpq.domain.menu.MenuId
import com.alhikmah.tpq.domain.menu.MenuResolver
import com.alhikmah.tpq.domain.model.Ringkasan
import com.alhikmah.tpq.domain.model.SantriRingkas
import com.alhikmah.tpq.domain.model.StatusHadir
import com.alhikmah.tpq.domain.model.User
import com.alhikmah.tpq.domain.model.UstadzHariIni
import com.alhikmah.tpq.domain.util.Inisial
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

/**
 * Daftar ustadz hari ini per baris (inisial, nama, mapel, pill status). Ustadz yang tertaut ke akun sendiri
 * disembunyikan kecuali akun ADMIN/TU (aturan di [UstadzHariIniBeranda], sama dengan layar Absensi Ustadz).
 * Server lama tanpa `daftar`: jatuh ke ringkasan angka.
 */
@Composable
private fun UstadzHariIniCard(user: User, ustadz: UstadzRingkas, bisaBuka: Boolean, onOpen: (MenuId) -> Unit) {
    val tampil = remember(ustadz, user) { UstadzHariIniBeranda.susun(ustadz, user) }
    UstadzHariIniContent(tampil, ustadz, bisaBuka, onOpen)
}

@Composable
private fun UstadzHariIniContent(tampil: UstadzHariIniTampil, ustadz: UstadzRingkas, bisaBuka: Boolean, onOpen: (MenuId) -> Unit) {
    SectionCard(spacing = 4) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Ustadz hari ini")
            if (bisaBuka) LinkAction("Lihat semua", { onOpen(MenuId.ABSENSI_USTADZ) })
        }
        when {
            !tampil.adaDaftar -> Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                AngkaLabel(ustadz.hadir, "hadir")
                AngkaLabel(ustadz.tidakHadir, "tidak hadir")
                AngkaLabel(ustadz.belumDicatat, "belum dicatat")
            }
            tampil.baris.isEmpty() -> Text("Belum ada ustadz lain yang perlu ditampilkan.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> tampil.baris.forEach { UstadzRow(it) }
        }
        if (tampil.lainnya > 0) {
            Text("dan ${tampil.lainnya} ustadz lainnya", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        // Server melarang pencatatan atas nama sendiri kecuali ADMIN/TU, jadi catatan ini benar untuk akun yang barisnya disembunyikan.
        if (tampil.catatanSendiri) {
            Text("Absensi Anda sendiri dicatat oleh piket lain.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Satu baris ustadz; dibaca TalkBack sebagai satu kalimat ("Ustadz Hasan, Fiqih, Hadir"). */
@Composable
private fun UstadzRow(u: UstadzHariIni) {
    val label = when (u.status) {
        StatusHadir.HADIR -> "Hadir"
        StatusHadir.TIDAK_HADIR -> "Tidak hadir"
        StatusHadir.BELUM_DICATAT -> "Belum dicatat"
    }
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 8.dp).semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape), contentAlignment = Alignment.Center) {
                Text(Inisial.dari(u.nama), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Column(Modifier.weight(1f)) {
                Text(u.nama, style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!u.mapelDiajar.isNullOrBlank()) {
                    Text(u.mapelDiajar.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            StatusPill(label, u.status)
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
