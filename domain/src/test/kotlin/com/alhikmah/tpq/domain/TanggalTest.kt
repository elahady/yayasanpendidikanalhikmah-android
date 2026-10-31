package com.alhikmah.tpq.domain

import com.alhikmah.tpq.domain.util.Tanggal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TanggalTest {

    @Test
    fun `hari ini memakai zona Asia Jakarta bukan UTC`() {
        // 2026-11-01 20:00 UTC = 2026-11-02 03:00 WIB.
        val clock = Clock.fixed(Instant.parse("2026-11-01T20:00:00Z"), ZoneOffset.UTC)
        assertEquals(LocalDate.of(2026, 11, 2), Tanggal.today(clock))
    }

    @Test
    fun `format api dan label indonesia`() {
        val date = LocalDate.of(2026, 11, 2)
        assertEquals("2026-11-02", Tanggal.toApi(date))
        assertEquals("Senin, 2 November 2026", Tanggal.label(date))
        assertEquals("2 Nov", Tanggal.short(date))
    }

    @Test
    fun `parse tanggal api`() {
        assertEquals(LocalDate.of(2026, 1, 5), Tanggal.fromApi("2026-01-05"))
        assertNull(Tanggal.fromApi("5/1/2026"))
        assertNull(Tanggal.fromApi(null))
    }
}
