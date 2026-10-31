package com.alhikmah.tpq.ui.beranda

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alhikmah.tpq.domain.menu.MenuEntry
import com.alhikmah.tpq.domain.menu.MenuId
import com.alhikmah.tpq.domain.model.Jilid
import com.alhikmah.tpq.domain.model.Perhatian
import com.alhikmah.tpq.domain.model.SantriRingkas
import com.alhikmah.tpq.domain.model.StatusHadir
import com.alhikmah.tpq.ui.common.Pill
import com.alhikmah.tpq.ui.common.menuMeta
import com.alhikmah.tpq.ui.theme.statusColors

/* Bahan penyusun bersama ketiga varian Beranda (admin, tugas, wali). Semua warna dari MaterialTheme/statusColors. */

/** Kartu putih dengan garis tipis (kartu biasa di mockup). */
@Composable
internal fun SectionCard(modifier: Modifier = Modifier, spacing: Int = 12, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(spacing.dp), content = content)
    }
}

/** Kartu utama berwarna primer (hero) dengan teks onPrimary. */
@Composable
internal fun HeroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

/** Label kecil huruf kapital di atas judul. */
@Composable
internal fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp, fontWeight = FontWeight.Bold),
        color = color,
    )
}

@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, lineHeight = 24.sp))
}

/** Pill status kehadiran dengan teks (warna hanya pendukung). */
@Composable
internal fun StatusPill(text: String, status: StatusHadir, modifier: Modifier = Modifier) {
    val c = MaterialTheme.statusColors
    when (status) {
        StatusHadir.HADIR -> Pill(text, c.hadirContainer, c.onHadirContainer, modifier)
        StatusHadir.TIDAK_HADIR -> Pill(text, c.tidakHadirContainer, c.onTidakHadirContainer, modifier)
        StatusHadir.BELUM_DICATAT -> Pill(text, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant, modifier)
    }
}

/** Tombol di atas hero: latar onPrimary, teks primary. Tinggi sentuh >= 48dp. */
@Composable
internal fun HeroButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick,
        modifier.fillMaxWidth().heightIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimary, contentColor = MaterialTheme.colorScheme.primary),
    ) { Text(text) }
}

@Composable
internal fun PrimaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick, modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(text) }
}

@Composable
internal fun GhostAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick, modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(text) }
}

/** Tautan teks kecil (mis. "Lihat semua"), area sentuh >= 48dp. */
@Composable
internal fun LinkAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.heightIn(min = 48.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// Cincin kehadiran
// ---------------------------------------------------------------------------------------------

/** Cincin kemajuan: lintasan = warna konten 30%, isi = warna konten penuh (di atas kartu primer: onPrimary). */
@Composable
internal fun KehadiranRing(fraction: Float, persen: Int, modifier: Modifier = Modifier) {
    val fill = LocalContentColor.current
    val track = fill.copy(alpha = 0.3f)
    Box(
        modifier.size(96.dp).clearAndSetSemantics { contentDescription = "$persen persen santri hadir hari ini" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, topLeft, arc, style = Stroke(stroke))
            if (fraction > 0f) drawArc(fill, -90f, 360f * fraction, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text("$persen%", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    }
}

// ---------------------------------------------------------------------------------------------
// Perlu perhatian
// ---------------------------------------------------------------------------------------------

/**
 * Daftar "Perlu perhatian". Item bisa diketuk (dengan tanda panah) hanya bila menu tujuannya tersedia untuk role ini
 * ([allowed]); selain itu tampil sebagai teks biasa. Teks judul/keterangan dari server apa adanya.
 */
@Composable
internal fun PerhatianCard(items: List<Perhatian>, allowed: Set<MenuId>, onOpen: (MenuId) -> Unit, modifier: Modifier = Modifier) {
    SectionCard(modifier, spacing = 4) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Perlu perhatian")
            if (items.isNotEmpty()) {
                Pill("${items.size}", MaterialTheme.statusColors.tidakHadirContainer, MaterialTheme.statusColors.onTidakHadirContainer)
            }
        }
        if (items.isEmpty()) {
            Text("Tidak ada yang perlu diperhatikan hari ini.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            items.forEach { item -> PerhatianRow(item, item.tujuan(allowed), onOpen) }
        }
    }
}

@Composable
private fun PerhatianRow(item: Perhatian, tujuan: MenuId?, onOpen: (MenuId) -> Unit) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        val rowModifier = if (tujuan != null) {
            Modifier.clickable(onClickLabel = "Buka ${menuMeta(tujuan).title}") { onOpen(tujuan) }
        } else {
            Modifier
        }
        Row(
            rowModifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.judul, style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp))
                if (!item.keterangan.isNullOrBlank()) {
                    Text(item.keterangan.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (tujuan != null) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Tangga jilid
// ---------------------------------------------------------------------------------------------

private val labelJilidSingkat = mapOf(
    Jilid.PRA_NAHDLIYAH to "Pra", Jilid.JILID_1 to "J1", Jilid.JILID_2 to "J2", Jilid.JILID_3 to "J3",
    Jilid.JILID_4 to "J4", Jilid.JILID_5 to "J5", Jilid.JILID_6 to "J6", Jilid.AL_QURAN to "Qur'an",
)

/** Teks pembaca layar untuk grafik: "Pra Nahdhiyah 9, Jilid 1 14, ...". */
internal fun deskripsiTanggaJilid(santri: SantriRingkas): String =
    Jilid.entries.joinToString(", ") { "${it.label} ${santri.perJilid[it] ?: 0} santri" }

@Composable
internal fun TanggaJilidCard(santri: SantriRingkas, modifier: Modifier = Modifier) {
    val maks = (santri.perJilid.values.maxOrNull() ?: 0).coerceAtLeast(1)
    val barMax = 84.dp
    SectionCard(modifier) {
        Column {
            SectionTitle("Tangga jilid")
            Text("Jumlah santri di tiap tahap An-Nahdhiyah", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(
            Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "Tangga jilid. " + deskripsiTanggaJilid(santri) },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                Jilid.entries.forEach { jilid ->
                    val n = santri.perJilid[jilid] ?: 0
                    val barColor = if (jilid == Jilid.AL_QURAN) MaterialTheme.statusColors.aksen else MaterialTheme.colorScheme.primary
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Text("$n", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier.fillMaxWidth()
                                .height(if (n > 0) (barMax * (n.toFloat() / maks)).coerceAtLeast(3.dp) else 0.dp)
                                .background(barColor, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Jilid.entries.forEach { jilid ->
                    Text(
                        labelJilidSingkat.getValue(jilid),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Pintasan menu
// ---------------------------------------------------------------------------------------------

/** Kisi pintasan 2 kolom ke layar yang boleh diakses role (tanpa "Anak Saya", yang ada di tab bawah). */
@Composable
internal fun MenuShortcuts(menu: List<MenuEntry>, onOpen: (MenuId) -> Unit, modifier: Modifier = Modifier) {
    val tiles = menu.filter { it.id != MenuId.ANAK_SAYA }
    if (tiles.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { entry -> MenuTile(entry.id, { onOpen(entry.id) }, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MenuTile(id: MenuId, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val meta = menuMeta(id)
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 104.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(meta.icon, contentDescription = null) }
            }
            Text(meta.title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp))
        }
    }
}

/** Teks "N santri" dsb. dengan angka tebal di depan. */
@Composable
internal fun AngkaLabel(angka: Int, label: String, modifier: Modifier = Modifier, muted: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Column(modifier) {
        Text("$angka", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, lineHeight = 30.sp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = muted)
    }
}


/** Tombol bergaris di atas hero (pasangan [HeroButton]). */
@Composable
internal fun HeroGhostAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick,
        modifier.fillMaxWidth().heightIn(min = 48.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)),
    ) { Text(text) }
}

/** Batang kemajuan sederhana (lintasan 30% + isi) memakai warna konten saat ini; teks pendamping wajib ada di dekatnya. */
@Composable
internal fun BatangKemajuan(fraction: Float, modifier: Modifier = Modifier) {
    val fill = LocalContentColor.current
    Box(modifier.fillMaxWidth().height(10.dp).background(fill.copy(alpha = 0.3f), RoundedCornerShape(50))) {
        if (fraction > 0f) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(10.dp).background(fill, RoundedCornerShape(50)))
        }
    }
}
