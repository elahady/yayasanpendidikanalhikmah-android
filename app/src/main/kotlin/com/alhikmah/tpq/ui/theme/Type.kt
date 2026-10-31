package com.alhikmah.tpq.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.alhikmah.tpq.R

/*
 * Tipografi sama dengan web: Fraunces untuk judul (font-display), Plus Jakarta Sans untuk isi.
 * Keduanya font variabel (lisensi OFL, salinan di assets/licenses); minSdk 26 mendukung font variabel.
 */

private fun fraunces(weight: Int) = Font(
    resId = R.font.fraunces,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private fun jakarta(weight: Int) = Font(
    resId = R.font.plus_jakarta_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val FrauncesFamily = FontFamily(fraunces(400), fraunces(600), fraunces(700))
val JakartaFamily = FontFamily(jakarta(400), jakarta(500), jakarta(600), jakarta(700))

private val base = Typography()

val AlHikmahTypography: Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = FrauncesFamily, fontWeight = FontWeight.Bold),
    displayMedium = base.displayMedium.copy(fontFamily = FrauncesFamily, fontWeight = FontWeight.Bold),
    displaySmall = base.displaySmall.copy(fontFamily = FrauncesFamily, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = FrauncesFamily, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = FrauncesFamily, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = FrauncesFamily, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = FrauncesFamily, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontFamily = JakartaFamily, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = JakartaFamily, fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontFamily = JakartaFamily),
    bodyMedium = base.bodyMedium.copy(fontFamily = JakartaFamily),
    bodySmall = base.bodySmall.copy(fontFamily = JakartaFamily),
    labelLarge = base.labelLarge.copy(fontFamily = JakartaFamily, fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontFamily = JakartaFamily, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(fontFamily = JakartaFamily, fontWeight = FontWeight.Medium),
)
