package com.aistudio.axewatch.trader.data.remote

import com.aistudio.axewatch.trader.data.local.entity.AllotmentRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Correctness pins for the allotment engine. Every rule here exists because
 * the opposite once shipped: transport failures reported as "not applied",
 * full PANs in logs, invented application numbers, and short-name
 * collisions attributing one IPO's result to another.
 */
@RunWith(RobolectricTestRunner::class)
class IpoAllotmentServiceTest {

    // ---- PAN validation & masking (PII safety) ----

    @Test
    fun `valid PAN passes strict check`() {
        assertTrue(IpoAllotmentService.isValidPan("ABCDE1234F"))
    }

    @Test
    fun `lowercase and padded PAN is normalized, not rejected`() {
        assertTrue(IpoAllotmentService.isValidPan("  abcde1234f "))
    }

    @Test
    fun `malformed PANs are rejected`() {
        assertFalse(IpoAllotmentService.isValidPan(""))
        assertFalse(IpoAllotmentService.isValidPan("ABCDE12345"))
        assertFalse(IpoAllotmentService.isValidPan("ABCD1234F"))
        assertFalse(IpoAllotmentService.isValidPan("ABCDE123FG"))
        // Leading/trailing whitespace is trimmed, so this is VALID:
        assertTrue(IpoAllotmentService.isValidPan("ABCDE1234F "))
        assertFalse(IpoAllotmentService.isValidPan("ABCDE1234FX"))
    }

    @Test
    fun `maskPan leaks only first-two and last chars`() {
        assertEquals("AB*****F", IpoAllotmentService.maskPan("ABCDE1234F"))
        assertEquals("***", IpoAllotmentService.maskPan("SHORT"))
    }

    @Test
    fun `applicant names are masked`() {
        // Masking preserves the registrar's casing; only length leaks.
        assertEquals("RAHU***", IpoAllotmentService.maskApplicantName("RAHUL SHARMA"))
        assertEquals("", IpoAllotmentService.maskApplicantName(""))
        assertEquals("***", IpoAllotmentService.maskApplicantName("AB"))
    }

    // ---- Canonical join key (must mirror web backend canon_ipo_name) ----

    @Test
    fun `canon strips corporate suffixes and status words`() {
        assertEquals("manikaplastech", IpoAllotmentService.canonIpoName("Manika Plastech Ltd"))
        assertEquals("waareeenergies", IpoAllotmentService.canonIpoName("Waaree Energies Limited LISTED"))
        assertEquals("heromotors", IpoAllotmentService.canonIpoName("Hero Motors Pvt Ltd Open"))
        assertEquals("", IpoAllotmentService.canonIpoName(null))
        assertEquals("", IpoAllotmentService.canonIpoName(""))
    }

    @Test
    fun `canon normalizes ampersand to and`() {
        assertEquals(
            IpoAllotmentService.canonIpoName("Smith and Wesson"),
            IpoAllotmentService.canonIpoName("Smith & Wesson")
        )
    }

    @Test
    fun `exact-after-strip names match`() {
        assertTrue(
            IpoAllotmentService.ipoNamesMatch("Manika Plastech Ltd", "Manika Plastech")
        )
    }

    @Test
    fun `long substring matches, short names never collide`() {
        assertTrue(
            IpoAllotmentService.ipoNamesMatch(
                "Veegaland Developers Limited",
                "Veegaland Developers"
            )
        )
        // 9 shared chars < 10 minimum: must NOT match.
        assertFalse(IpoAllotmentService.ipoNamesMatch("MANIKAPLA", "Manika Plastech Ltd"))
        // Short-name collision guard: different companies sharing a stem.
        assertFalse(IpoAllotmentService.ipoNamesMatch("ARCIL", "BARCIL"))
        assertFalse(IpoAllotmentService.ipoNamesMatch("", "Anything"))
        assertFalse(IpoAllotmentService.ipoNamesMatch(null, null))
    }

    // ---- Dropdown matching guards ----

    private val companies = listOf(
        MufgCompany("101", "Manika Plastech"),
        MufgCompany("202", "Veegaland Developers Limited"),
        MufgCompany("303", "Arcil")
    )
    private val svc = IpoAllotmentService()

    @Test
    fun `dropdown matches exact canonical name`() {
        assertEquals(
            "101",
            svc.findBestCompanyMatch("Manika Plastech Ltd", "MANIKAPLA", companies)?.id
        )
    }

    @Test
    fun `dropdown matches long substring`() {
        assertEquals(
            "202",
            svc.findBestCompanyMatch("Veegaland Developers", "VEEGALAND", companies)?.id
        )
    }

    @Test
    fun `short symbol never substring-collides`() {
        assertNull(svc.findBestCompanyMatch("Arcil Polymers Ltd", "ARC", companies))
        assertNull(svc.findBestCompanyMatch("Unknown Company", "UNKNOWNCO", companies))
    }

    // ---- MUFG XML contract ----

    private fun xml(shares: Int, allot: Int, appNo: String = "11223344", name: String = "RAHUL SHARMA") =
        "<NewDataSet><Table><SHARES>$shares</SHARES><ALLOT>$allot</ALLOT>" +
            "<PEMNDG>$appNo</PEMNDG><NAME1>$name</NAME1></Table></NewDataSet>"

    @Test
    fun `allotted XML yields ALLOTTED with real app number`() {
        val r = svc.parseMufgSearchXml(xml(150, 150), "Manika Plastech")
        assertTrue(r.found)
        assertEquals("ALLOTTED", r.status)
        assertEquals(150, r.sharesApplied)
        assertEquals(150, r.sharesAllotted)
        assertEquals("11223344", r.applicationNo)
        // Applicant name must already be masked at parse time.
        assertEquals("RAHU***", r.applicantName)
        assertFalse(r.applicantName.contains("RAHUL SHARMA"))
    }

    @Test
    fun `applied-but-zero-allotment yields NOT_ALLOTTED`() {
        val r = svc.parseMufgSearchXml(xml(150, 0), "Manika Plastech")
        assertTrue(r.found)
        assertEquals("NOT_ALLOTTED", r.status)
        assertEquals(150, r.sharesApplied)
        assertEquals(0, r.sharesAllotted)
    }

    @Test
    fun `empty result yields NOT_APPLIED, never LOOKUP_FAILED`() {
        val r = svc.parseMufgSearchXml("<NewDataSet></NewDataSet>", "Manika Plastech")
        assertFalse(r.found)
        assertEquals("NOT_APPLIED", r.status)
    }

    @Test
    fun `registrar error message yields NOT_APPLIED with note`() {
        val r = svc.parseMufgSearchXml(
            "<NewDataSet><Table><MSG>No records found</MSG></Table></NewDataSet>",
            "Manika Plastech"
        )
        assertFalse(r.found)
        assertEquals("NOT_APPLIED", r.status)
        assertEquals("No records found", r.note)
    }

    @Test
    fun `garbage XML never crashes and never reports applied`() {
        val r = svc.parseMufgSearchXml("this is not xml <<<", "Manika Plastech")
        assertFalse(r.found)
        assertEquals("NOT_APPLIED", r.status)
        assertEquals(0, r.sharesApplied)
        assertEquals(0, r.sharesAllotted)
    }

    @Test
    fun `missing app number stays blank, never invented`() {
        val r = svc.parseMufgSearchXml(
            "<NewDataSet><Table><SHARES>35</SHARES><ALLOT>35</ALLOT></Table></NewDataSet>",
            "Manika Plastech"
        )
        assertEquals("ALLOTTED", r.status)
        assertEquals("", r.applicationNo)
    }

    // ---- Record status vocabulary ----

    @Test
    fun `status labels distinguish every outcome`() {
        fun label(status: String) = AllotmentRecordEntity(
            maskedPan = "AB*****F",
            ipoSymbol = "X",
            ipoName = "X",
            sharesApplied = 0,
            sharesAllotted = 0,
            status = status,
            registrar = "MUFG Intime"
        ).statusLabel
        assertEquals("Allotted", label("ALLOTTED"))
        assertEquals("Not Allotted", label("NOT_ALLOTTED"))
        assertEquals("Not Applied", label("NOT_APPLIED"))
        assertEquals("Results Not Out Yet", label("RESULTS_NOT_OUT"))
        assertEquals("Lookup Failed — Retry", label("LOOKUP_FAILED"))
    }
}
