package com.alhikmah.tpq.ui.beranda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.alhikmah.tpq.domain.menu.MenuEntry
import com.alhikmah.tpq.domain.menu.MenuId
import com.alhikmah.tpq.domain.model.Ringkasan

/**
 * Varian ADMIN / TU / PENGAWAS (mockup MobAdmin): cincin kehadiran santri + ustadz hadir, "Perlu perhatian"
 * (bisa diketuk bila menunya tersedia), tangga jilid, dan pintasan menu.
 */
@Composable
internal fun AdminSections(ringkasan: Ringkasan, menu: List<MenuEntry>, allowed: Set<MenuId>, onOpen: (MenuId) -> Unit) {
    AdminHero(ringkasan)
    PerhatianCard(ringkasan.perhatian, allowed, onOpen)
    TanggaJilidCard(ringkasan.santri)
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
