package com.alhikmah.tpq.domain

import com.alhikmah.tpq.domain.api.ApiException
import com.alhikmah.tpq.domain.api.ApiJson
import com.alhikmah.tpq.domain.api.ErrorCodes
import com.alhikmah.tpq.domain.model.Jenjang
import com.alhikmah.tpq.domain.model.Jilid
import com.alhikmah.tpq.domain.model.ProgressUpdate
import com.alhikmah.tpq.domain.model.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiJsonTest {

    @Test
    fun `parse login`() {
        val result = ApiJson.parseLogin(
            """{"accessToken":"jwt.abc","user":{"id":"1","nama":"Ustadz Hasan","roles":["PIKET","USTADZ"],"ustadzId":"u3"}}""",
        )
        assertEquals("jwt.abc", result.accessToken)
        assertEquals(setOf(Role.PIKET, Role.USTADZ), result.user.roles)
        assertEquals("u3", result.user.ustadzId)
    }

    @Test
    fun `ustadzId null dan role tak dikenal diabaikan`() {
        val user = ApiJson.parseUser("""{"id":"1","nama":"A","roles":["WALI","ROLE_BARU"],"ustadzId":null}""")
        assertNull(user.ustadzId)
        assertEquals(setOf(Role.WALI), user.roles)
    }

    @Test
    fun `parse daftar santri termasuk catatan null dan kelas angka`() {
        val list = ApiJson.parseSantriList(
            """{"items":[
              {"id":"s1","nama":"Aisyah","jenjang":"ULA","kelas":"1","jilidSaatIni":"JILID_2","catatanProgressMadin":null},
              {"id":"s2","nama":"Budi","jenjang":"WUSTHO","kelas":2,"jilidSaatIni":"AL_QURAN","catatanProgressMadin":"Fiqih thaharah"}
            ]}""",
        )
        assertEquals(2, list.size)
        assertEquals(Jenjang.ULA, list[0].jenjang)
        assertEquals(Jilid.JILID_2, list[0].jilidSaatIni)
        assertNull(list[0].catatanProgressMadin)
        assertEquals("2", list[1].kelas)
        assertEquals("Wustho - Kelas 2", list[1].keteranganKelas)
    }

    @Test
    fun `parse absensi memakai kunci subjek yang diminta`() {
        val body = """{"items":[{"id":"e1","santriId":"s1","tanggal":"2026-11-02","hadir":true,"keterangan":null,"dicatatOleh":"Ustadz A","bisaDiubah":true},
            {"id":"e2","santriId":"s2","tanggal":"2026-11-02","hadir":false,"keterangan":"Sakit","dicatatOleh":"Ustadz B","bisaDiubah":false}]}"""
        val entries = ApiJson.parseAbsensi(body, ApiJson.SUBJECT_SANTRI)
        assertEquals("s1", entries[0].subjectId)
        assertTrue(entries[0].bisaDiubah)
        assertFalse(entries[1].hadir)
        assertEquals("Sakit", entries[1].keterangan)
        assertFalse(entries[1].bisaDiubah)
    }

    @Test
    fun `parse entri absensi ustadz tunggal`() {
        val entry = ApiJson.parseAbsensiEntry(
            """{"id":"e1","ustadzId":"u1","tanggal":"2026-11-02","hadir":true,"keterangan":null,"dicatatOleh":"TU","bisaDiubah":true}""",
            ApiJson.SUBJECT_USTADZ,
        )
        assertEquals("u1", entry.subjectId)
    }

    @Test
    fun `parse rekap`() {
        val rekap = ApiJson.parseRekap(
            """{"tanggal":"2026-11-02","santri":{"hadir":40,"total":45,"belumDicatat":2},"ustadz":{"hadir":5,"total":6,"belumDicatat":0}}""",
        )
        assertEquals(40, rekap.santri.hadir)
        assertEquals(45, rekap.santri.total)
        assertEquals(2, rekap.santri.belumDicatat)
        assertEquals(6, rekap.ustadz.total)
    }

    @Test
    fun `bentuk tidak sesuai kontrak menjadi BAD_RESPONSE bukan crash`() {
        val e = assertFailsWith<ApiException> { ApiJson.parseRekap("""{"tanggal":"2026-11-02"}""") }
        assertEquals(ErrorCodes.BAD_RESPONSE, e.error.code)
        assertFailsWith<ApiException> { ApiJson.parseSantriList("bukan json") }
        assertFailsWith<ApiException> { ApiJson.parseLogin("""{"accessToken":"","user":{"id":"1","nama":"A","roles":[]}}""") }
    }

    @Test
    fun `body login dan absensi`() {
        val login = ApiJson.loginBody("admin", "rahasia")
        assertEquals("admin", login.getString("username"))
        val body = ApiJson.absensiBody(ApiJson.SUBJECT_USTADZ, "u1", "2026-11-02", false, "  Sakit  ")
        assertEquals("u1", body.getString("ustadzId"))
        assertEquals("2026-11-02", body.getString("tanggal"))
        assertFalse(body.getBoolean("hadir"))
        assertEquals("Sakit", body.getString("keterangan"))
        assertTrue(ApiJson.absensiBody(ApiJson.SUBJECT_SANTRI, "s1", "2026-11-02", true, "   ").isNull("keterangan"))
    }

    @Test
    fun `body progress hanya memuat field yang diubah`() {
        val onlyJilid = ApiJson.progressBody(ProgressUpdate(jilid = Jilid.JILID_3))
        assertEquals("JILID_3", onlyJilid.getString("jilidSaatIni"))
        assertFalse(onlyJilid.has("catatanProgressMadin"))

        val clearNote = ApiJson.progressBody(ProgressUpdate(updateCatatan = true, catatan = null))
        assertFalse(clearNote.has("jilidSaatIni"))
        assertTrue(clearNote.has("catatanProgressMadin"))
        assertTrue(clearNote.isNull("catatanProgressMadin"))
    }
}
