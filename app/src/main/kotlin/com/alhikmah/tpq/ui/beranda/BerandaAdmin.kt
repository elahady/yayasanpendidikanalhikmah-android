package com.alhikmah.tpq.ui.beranda

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.alhikmah.tpq.domain.beranda.AktivitasBeranda
import com.alhikmah.tpq.domain.beranda.AktivitasTampil
import com.alhikmah.tpq.domain.beranda.TrenKehadiran
import com.alhikmah.tpq.domain.beranda.TrenKehadiranTampil
import com.alhikmah.tpq.domain.menu.MenuEntry
import com.alhikmah.tpq.domain.menu.MenuId
import com.alhikmah.tpq.domain.model.Aktivitas
import com.alhikmah.tpq.domain.model.Ringkasan
import com.alhikmah.tpq.domain.model.TrenHarian
import com.alhikmah.tpq.domain.util.Tanggal
import com.alhikmah.tpq.ui.theme.statusColors

/**
 * Varian ADMIN / TU / PENGAWAS (mockup MobAdmin): cincin kehadiran santri + ustadz hadir, "Perlu perhatian"
 * (bisa diketuk bila menunya tersedia), kehadiran 14 hari (tren), tangga jilid, aktivitas terbaru, dan pintasan menu.
 */
@Composable
internal fun AdminSections(ringkasan: Ringkasan, tanggal: String?, menu: List<MenuEntry>, allowed: Set<MenuId>, onOpen: (MenuId) -> Unit) {
    AdminHero(ringkasan)
    PerhatianCard(ringkasan.perhatian, allowed, onOpen)
    TrenKehadiranCard(ringkasan.tren, tanggal)
    TanggaJilidCard(ringkasan.santri)
    AktivitasTerbaruCard(ringkasan.aktivitas, tanggal)
    MenuShortcuts(menu, onOpen)
}

@Composable
private fun AdminHero(ringkasan: Ringkasan) {
    val s = ringkasan.santri
    val u = ringkasan.ustadz
    val redup = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
    val kecil = MaterialTheme.typography.bodyMedium.fontSize
    HeroCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            KehadiranRing(s.bagianHadir, s.persenHadir)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Eyebrow("Santri hadir hari ini", color = redup)
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("${s.hadir}") }
                        append(" dari ${s.total} santri")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text("${s.tidakHadir} tidak hadir · ${s.belumDicatat} belum dicatat", style = MaterialTheme.typography.bodySmall)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Ustadz & ustadzah hadir", style = MaterialTheme.typography.bodyMedium)
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("${u.hadir}") }
                    append(" ")
                    withStyle(SpanStyle(fontSize = kecil, color = redup)) {
                        append("dari ${u.total}")
                    }
                },
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Kehadiran 14 hari (tren)
// ---------------------------------------------------------------------------------------------

private val tinggiGrafik = 120.dp

/**
 * Batang kehadiran santri per hari belajar (tinggi = hadir / tercatat); hari terakhir berwarna aksen, lainnya primer
 * agak pudar. Grafik dibaca pembaca layar sebagai satu elemen dengan ringkasan teks ([TrenKehadiranTampil.deskripsi]).
 */
@Composable
internal fun TrenKehadiranCard(tren: List<TrenHarian>, tanggalDashboard: String?, modifier: Modifier = Modifier) {
    val tampil = remember(tren, tanggalDashboard) { TrenKehadiran.susun(tren, tanggalDashboard) }
    SectionCard(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            SectionTitle("Kehadiran 14 hari")
            if (tampil?.rataRataPersen != null) {
                Text("rata-rata ${tampil.rataRataPersen}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (tampil == null) {
            Text("Belum ada data kehadiran santri.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            BatangTrenKehadiran(tampil)
        }
    }
}

@Composable
private fun BatangTrenKehadiran(tampil: TrenKehadiranTampil) {
    val primer = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val aksen = MaterialTheme.statusColors.aksen
    Column(
        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = tampil.deskripsi },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(tinggiGrafik), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
            tampil.batang.forEachIndexed { index, b ->
                val terakhir = index == tampil.batang.lastIndex
                val tinggi = if (b.tercatat > 0) (tinggiGrafik * b.tinggi).coerceAtLeast(3.dp) else 2.dp
                Box(
                    Modifier.weight(1f).height(tinggi)
                        .background(if (terakhir) aksen else primer, RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tampil.labelAwal, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(tampil.labelAkhir, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Aktivitas terbaru
// ---------------------------------------------------------------------------------------------

@Composable
internal fun AktivitasTerbaruCard(aktivitas: List<Aktivitas>, tanggalDashboard: String?, modifier: Modifier = Modifier) {
    // Hari ini menurut server (tanggal dashboard), bukan jam HP.
    val hariIni = remember(tanggalDashboard) { Tanggal.fromApi(tanggalDashboard) ?: Tanggal.today() }
    val baris = remember(aktivitas, hariIni) { AktivitasBeranda.susun(aktivitas, hariIni) }
    SectionCard(modifier, spacing = 14) {
        SectionTitle("Aktivitas terbaru")
        if (baris.isEmpty()) {
            Text("Belum ada aktivitas dalam 7 hari terakhir.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            baris.forEach { AktivitasRow(it) }
        }
    }
}

@Composable
private fun AktivitasRow(item: AktivitasTampil) {
    val oleh = item.oleh
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.padding(top = 7.dp).size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        Column(Modifier.weight(1f)) {
            Text(
                buildAnnotatedString {
                    // Teks dirakit server ("<nama> mencatat ..."); nama pencatat ditebalkan bila memang di awal kalimat.
                    if (oleh.isNotEmpty() && item.ringkas.startsWith(oleh)) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(oleh) }
                        append(item.ringkas.removePrefix(oleh))
                    } else {
                        append(item.ringkas)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(item.waktu, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
