package com.alhikmah.tpq.domain.util

/** Inisial untuk avatar: dua kata atau lebih -> huruf pertama dua kata pertama ("Ust. Hasan" -> "UH"); satu kata -> dua huruf pertama. */
object Inisial {
    fun dari(nama: String): String {
        val kata = nama.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val huruf = when {
            kata.isEmpty() -> "?"
            kata.size == 1 -> kata[0].filter { it.isLetterOrDigit() }.take(2)
            else -> kata.take(2).map { w -> w.firstOrNull { it.isLetterOrDigit() } ?: w.first() }.joinToString("")
        }
        return huruf.ifEmpty { "?" }.uppercase()
    }
}
