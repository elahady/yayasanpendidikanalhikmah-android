package com.alhikmah.tpq.domain

import com.alhikmah.tpq.domain.fake.FakeData
import com.alhikmah.tpq.domain.model.Role
import com.alhikmah.tpq.domain.model.User
import com.alhikmah.tpq.domain.roster.Roster
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RosterTest {

    private val tanggal = "2026-11-02"

    @Test
    fun `gabung santri dengan entri hari itu dan urut nama`() {
        val people = FakeData.santri.map(Roster::person)
        val rows = Roster.merge(
            people,
            listOf(
                FakeData.entry("s3", tanggal, hadir = true),
                FakeData.entry("s1", tanggal, hadir = false, keterangan = "Sakit"),
            ),
        )
        assertEquals(listOf("Aisyah Putri", "Budi Santoso", "Citra Lestari", "Dimas Pratama", "Elok Maharani"), rows.map { it.person.nama })
        assertEquals("Sakit", rows[0].entry?.keterangan)
        assertNull(rows[1].entry)
        assertTrue(rows[1].belumDicatat)
        assertTrue(rows[2].entry!!.hadir)
    }

    @Test
    fun `entri tanpa orang di daftar diabaikan`() {
        val rows = Roster.merge(FakeData.ustadz.map(Roster::person), listOf(FakeData.entry("hantu", tanggal, true)))
        assertEquals(FakeData.ustadz.size, rows.size)
        assertTrue(rows.all { it.belumDicatat })
    }

    @Test
    fun `baris tanpa entri boleh diubah, entri ikut bisaDiubah dari server`() {
        val rows = Roster.merge(
            FakeData.santri.take(3).map(Roster::person),
            listOf(FakeData.entry("s1", tanggal, true, bisaDiubah = false), FakeData.entry("s2", tanggal, true, bisaDiubah = true)),
        )
        assertFalse(rows[0].bisaDiubah) // s1: dikunci server
        assertTrue(rows[1].bisaDiubah) // s2
        assertTrue(rows[2].bisaDiubah) // s3: belum ada entri
    }

    @Test
    fun `ringkasan hadir tidak hadir belum dicatat`() {
        val rows = Roster.merge(
            FakeData.santri.map(Roster::person),
            listOf(FakeData.entry("s1", tanggal, true), FakeData.entry("s2", tanggal, true), FakeData.entry("s3", tanggal, false)),
        )
        val summary = Roster.summarize(rows)
        assertEquals(2, summary.hadir)
        assertEquals(1, summary.tidakHadir)
        assertEquals(2, summary.belumDicatat)
    }

    @Test
    fun `ustadz yang tertaut ke akun sendiri disembunyikan kecuali admin atau tu`() {
        val piket = User("a", "Hasan", setOf(Role.PIKET, Role.USTADZ), "u3")
        assertEquals("u3", Roster.hiddenUstadzId(piket))
        val people = FakeData.ustadz.map(Roster::person)
        assertEquals(listOf("u1", "u2"), Roster.excluding(people, Roster.hiddenUstadzId(piket)).map { it.id })

        val tu = User("b", "TU", setOf(Role.TU, Role.USTADZ), "u3")
        assertNull(Roster.hiddenUstadzId(tu))
        assertNull(Roster.hiddenUstadzId(User("c", "Piket Murni", setOf(Role.PIKET), null)))
        assertEquals(3, Roster.excluding(people, null).size)
    }
}
