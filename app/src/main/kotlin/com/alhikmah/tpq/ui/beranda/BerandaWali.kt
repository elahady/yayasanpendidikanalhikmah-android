package com.alhikmah.tpq.ui.beranda

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alhikmah.tpq.domain.beranda.JilidJourney
import com.alhikmah.tpq.domain.beranda.JilidStep
import com.alhikmah.tpq.domain.beranda.StepState
import com.alhikmah.tpq.domain.model.AnakDashboard
import com.alhikmah.tpq.domain.model.KehadiranAnak
import com.alhikmah.tpq.domain.model.StatusHadir
import com.alhikmah.tpq.domain.model.pilih
import com.alhikmah.tpq.domain.util.Inisial
import com.alhikmah.tpq.domain.util.Tanggal
import com.alhikmah.tpq.ui.common.Pill
import com.alhikmah.tpq.ui.theme.statusColors

/**
 * Bagian anak (mockup MobWali): pilih anak (bila lebih dari satu), kartu anak dengan perjalanan jilid, kehadiran
 * 30 hari, dan catatan ustadz. Dipakai varian WALI dan sebagai tambahan di bawah varian lain bila akun juga WALI.
 */
@Composable
internal fun AnakSection(anak: List<AnakDashboard>, terpilih: String?, onPilih: (String) -> Unit, onDetail: ((String) -> Unit)?) {
    val aktif = anak.pilih(terpilih)
    if (aktif == null) {
        SectionCard {
            SectionTitle("Belum ada anak")
            Text(
                "Belum ada anak yang tertaut ke akun Anda. Hubungi admin yayasan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    if (anak.size > 1) PilihAnak(anak, aktif.santri.id, onPilih)
    AnakHero(aktif, onDetail)
    KehadiranAnakCard(aktif.kehadiran)
    CatatanCard(aktif)
}

@Composable
private fun PilihAnak(anak: List<AnakDashboard>, aktifId: String, onPilih: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        anak.forEach { a ->
            val dipilih = a.santri.id == aktifId
            val nama = a.santri.nama.split(Regex("\\s+")).take(2).joinToString(" ")
            Surface(
                selected = dipilih,
                onClick = { onPilih(a.santri.id) },
                modifier = Modifier.heightIn(min = 48.dp),
                shape = RoundedCornerShape(50),
                color = if (dipilih) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (dipilih) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                border = if (dipilih) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(Modifier.padding(start = 8.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(32.dp).background(
                            if (dipilih) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surfaceContainerHighest,
                            CircleShape,
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            Inisial.dari(a.santri.nama),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (dipilih) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(nama, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun AnakHero(anak: AnakDashboard, onDetail: ((String) -> Unit)?) {
    val santri = anak.santri
    val redup = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
    HeroCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(santri.nama, style = MaterialTheme.typography.headlineSmall)
                if (santri.keteranganJenjangKelas.isNotEmpty()) {
                    Text(santri.keteranganJenjangKelas, style = MaterialTheme.typography.bodySmall, color = redup)
                }
            }
            Pill(santri.jilidSaatIni?.label ?: "Jilid belum dicatat", MaterialTheme.colorScheme.onPrimary, MaterialTheme.colorScheme.primary)
        }
        Column {
            Eyebrow("Perjalanan jilid", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
            Spacer(Modifier.height(10.dp))
            PerjalananJilid(JilidJourney.steps(santri.jilidSaatIni), JilidJourney.ringkasan(santri.jilidSaatIni))
        }
        Text(JilidJourney.ringkasan(santri.jilidSaatIni), style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp))
        if (onDetail != null) HeroGhostAction("Lihat detail ${santri.nama.substringBefore(' ')}", { onDetail(santri.id) })
    }
}

/** Delapan titik tahap dengan garis penghubung; tiap sel sama lebar supaya label rata di bawah titiknya. */
@Composable
private fun PerjalananJilid(steps: List<JilidStep>, deskripsi: String) {
    val terang = MaterialTheme.colorScheme.onPrimary
    val garisPucat = terang.copy(alpha = 0.4f)
    Row(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "Perjalanan jilid. $deskripsi" }) {
        steps.forEachIndexed { i, step ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth().height(22.dp), verticalAlignment = Alignment.CenterVertically) {
                    val kiri = if (i == 0) Color.Transparent else if (steps[i - 1].state == StepState.SELESAI) terang else garisPucat
                    val kanan = if (i == steps.lastIndex) Color.Transparent else if (step.state == StepState.SELESAI) terang else garisPucat
                    Box(Modifier.weight(1f).height(3.dp).background(kiri))
                    when (step.state) {
                        StepState.SELESAI -> Box(Modifier.size(14.dp).background(terang, CircleShape))
                        StepState.SEKARANG -> Box(Modifier.size(22.dp).background(terang.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) {
                            Box(Modifier.size(14.dp).background(terang, CircleShape))
                        }
                        StepState.BELUM -> Box(Modifier.size(14.dp).border(2.dp, terang.copy(alpha = 0.7f), CircleShape))
                    }
                    Box(Modifier.weight(1f).height(3.dp).background(kanan))
                }
                Text(
                    step.singkat,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Kehadiran 30 hari
// ---------------------------------------------------------------------------------------------

private const val KOLOM = 10

/** Deskripsi untuk pembaca layar: ringkasan angka + tanggal tidak hadir/belum dicatat. */
internal fun deskripsiKehadiran(k: KehadiranAnak): String {
    fun daftar(status: StatusHadir) = k.hari.filter { it.status == status }.mapNotNull { Tanggal.fromApi(it.tanggal)?.let(Tanggal::short) }
    val tidak = daftar(StatusHadir.TIDAK_HADIR)
    val belum = daftar(StatusHadir.BELUM_DICATAT)
    return buildString {
        append("Kehadiran 30 hari: ${k.hadir} hadir, ${k.tidakHadir} tidak hadir, ${k.belumDicatat} belum dicatat.")
        if (tidak.isNotEmpty()) append(" Tidak hadir pada ${tidak.joinToString(", ")}.")
        if (belum.isNotEmpty()) append(" Belum dicatat pada ${belum.joinToString(", ")}.")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KehadiranAnakCard(k: KehadiranAnak) {
    SectionCard(spacing = 14) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            SectionTitle("Kehadiran 30 hari")
            Text("${k.hadir} hadir", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        if (k.hari.isEmpty()) {
            Text(
                "Belum ada hari belajar yang tercatat dalam 30 hari terakhir.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = deskripsiKehadiran(k) }, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                k.hari.chunked(KOLOM).forEach { baris ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        baris.forEach { hari -> KotakHari(hari.status, Modifier.weight(1f).aspectRatio(1f)) }
                        repeat(KOLOM - baris.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Legenda(StatusHadir.HADIR, "Hadir ${k.hadir}")
                Legenda(StatusHadir.TIDAK_HADIR, "Tidak hadir ${k.tidakHadir}")
                Legenda(StatusHadir.BELUM_DICATAT, "Belum dicatat ${k.belumDicatat}")
            }
        }
    }
}

/** Kotak satu hari: hadir = penuh, tidak hadir = terracotta dengan tanda ×, belum dicatat = garis putus-putus (bukan hanya warna). */
@Composable
private fun KotakHari(status: StatusHadir, modifier: Modifier = Modifier) {
    val bentuk = RoundedCornerShape(6.dp)
    when (status) {
        StatusHadir.HADIR -> Box(modifier.background(MaterialTheme.colorScheme.primary, bentuk))
        StatusHadir.TIDAK_HADIR -> Box(modifier.background(MaterialTheme.statusColors.aksen, bentuk), contentAlignment = Alignment.Center) {
            Text("×", color = MaterialTheme.colorScheme.background, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        }
        StatusHadir.BELUM_DICATAT -> {
            val garis = MaterialTheme.colorScheme.onSurfaceVariant
            Box(modifier.drawBehind {
                drawRoundRect(
                    color = garis,
                    cornerRadius = CornerRadius(6.dp.toPx()),
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
                )
            })
        }
    }
}

@Composable
private fun Legenda(status: StatusHadir, teks: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        KotakHari(status, Modifier.size(12.dp))
        Text(teks, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------------------------------------
// Catatan
// ---------------------------------------------------------------------------------------------

@Composable
private fun CatatanCard(anak: AnakDashboard) {
    val catatan = anak.santri.catatanProgressMadin
    SectionCard(spacing = 10) {
        SectionTitle("Catatan dari ustadz")
        if (catatan.isNullOrBlank()) {
            Text("Belum ada catatan dari ustadz.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(catatan, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp))
            Tanggal.panjangDariServer(anak.diperbaruiPada)?.let {
                Text("Diperbarui $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
