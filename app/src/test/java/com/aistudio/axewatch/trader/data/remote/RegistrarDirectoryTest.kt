package com.aistudio.axewatch.trader.data.remote

import com.aistudio.axewatch.trader.data.remote.IpoAllotmentService.Companion.canonIpoName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the registrar-directory port from the web backend. A mis-mapping
 * here sends checks to the wrong registrar (or "Unknown"), which is the
 * exact "can't detect this IPO" complaint these tests guard against.
 * Pure JVM tests — no Android APIs involved.
 */
class RegistrarDirectoryTest {

    // ---- registrar key mapping (must mirror backend) ----

    @Test
    fun `text mapping covers all known RTAs`() {
        assertEquals("mufg", RegistrarDirectory.registrarKeyFromText("MUFG Intime India"))
        assertEquals("mufg", RegistrarDirectory.registrarKeyFromText("Link Intime"))
        assertEquals("kfin", RegistrarDirectory.registrarKeyFromText("KFin Technologies"))
        assertEquals("bigshare", RegistrarDirectory.registrarKeyFromText("Bigshare Services"))
        assertEquals("skyline", RegistrarDirectory.registrarKeyFromText("Skyline Financial"))
        assertEquals("cameo", RegistrarDirectory.registrarKeyFromText("Cameo India"))
        assertEquals("purva", RegistrarDirectory.registrarKeyFromText("Purva Sharegistry"))
        assertEquals("maashitla", RegistrarDirectory.registrarKeyFromText("Maashitla Securities"))
        assertEquals("beetal", RegistrarDirectory.registrarKeyFromText("Beetal Financial"))
        assertNull(RegistrarDirectory.registrarKeyFromText("Some Unknown RTA"))
        assertNull(RegistrarDirectory.registrarKeyFromText(null))
    }

    @Test
    fun `href mapping covers all portal domains`() {
        assertEquals("kfin", RegistrarDirectory.registrarKeyFromHref("https://ipostatus.kfintech.com/"))
        assertEquals("bigshare", RegistrarDirectory.registrarKeyFromHref("https://ipo1.bigshareonline.com/ipo_status.html"))
        assertEquals("mufg", RegistrarDirectory.registrarKeyFromHref("https://in.mpms.mufg.com/Initial_Offer/public-issues.html"))
        assertNull(RegistrarDirectory.registrarKeyFromHref("https://example.com/"))
    }

    // ---- ipomarket.in tables ----

    private val ipomarketHtml = """
        <h3>Mainboard IPOs</h3>
        <table><tr><th>Company</th><th>Allotment Date</th><th>Registrar</th></tr>
        <tr><td>RentoMojo Ltd</td><td>18 Sep 2026</td><td>KFintech</td></tr>
        <tr><td>ABC SME Ltd</td><td>20 Sep 2026</td><td>Bigshare Services</td></tr>
        </table>
        <h3>SME IPOs</h3>
        <table><tr><td>XYZ Ltd</td><td>TBA</td><td>Unknown RTA</td></tr></table>
    """.trimIndent()

    @Test
    fun `ipomarket rows map to registrars with dates`() {
        val rows = RegistrarDirectory.parseIpomarket(ipomarketHtml)
        assertEquals(2, rows.size)
        val kfin = rows.first { it.registrar == "kfin" }
        assertEquals("RentoMojo Ltd", kfin.name)
        assertEquals("18 Sep 2026", kfin.allotmentDate)
        val bs = rows.first { it.registrar == "bigshare" }
        assertEquals("20 Sep 2026", bs.allotmentDate)
    }

    // ---- IPOWatch tables (strict header guard) ----

    private val ipowatchHtml = """
        <table><tr><th>IPO</th><th>IPO Date</th><th>Allotment Date</th><th>Allotment Status</th></tr>
        <tr><td>Foo Bars Ltd</td><td>10 Sep</td><td>19 Sep 2026</td><td><a href="https://ipostatus.kfintech.com/">Check Status</a></td></tr>
        <tr><td>Baz Qux Pvt Ltd</td><td>12 Sep</td><td></td><td><a href="https://ipo.bigshareonline.com/ipo_status.html">Bigshare</a></td></tr>
        </table>
        <table><tr><th>IPO</th><th>GMP</th><th>Price</th><th>Trend</th></tr>
        <tr><td>Junk Row Ltd</td><td>10</td><td>100</td><td>Up</td></tr>
        </table>
    """.trimIndent()

    @Test
    fun `ipowatch allotment tables map via links, gmp lookalikes ignored`() {
        val rows = RegistrarDirectory.parseIpowatch(ipowatchHtml)
        assertEquals(2, rows.size)
        val foo = rows.first { it.name == "Foo Bars Ltd" }
        assertEquals("kfin", foo.registrar)
        assertEquals("19 Sep 2026", foo.allotmentDate)
        val baz = rows.first { it.name == "Baz Qux Pvt Ltd" }
        assertEquals("bigshare", baz.registrar)
        assertTrue(rows.none { it.name == "Junk Row Ltd" })
    }

    @Test
    fun `ipowatch falls back to status text when no link`() {
        val html = """
            <table><tr><th>IPO</th><th>IPO Date</th><th>Allotment Date</th><th>Allotment Status</th></tr>
            <tr><td>Plain Text Co Ltd</td><td>1 Sep</td><td>5 Sep 2026</td><td>Cameo India</td></tr>
            </table>
        """.trimIndent()
        val rows = RegistrarDirectory.parseIpowatch(html)
        assertEquals(1, rows.size)
        assertEquals("cameo", rows[0].registrar)
    }

    // ---- Bigshare dropdown ----

    @Test
    fun `bigshare options parse scoped to ddlCompany`() {
        val html = """
            <select id="other"><option value="1">Noise</option></select>
            <select id="ddlCompany"><option value="0">Select Company</option>
            <option value="12">RentoMojo Limited</option>
            <option value="34">Foo Bars Ltd</option></select>
        """.trimIndent()
        val opts = RegistrarDirectory.parseBigshareOptions(html)
        assertEquals(2, opts.size)
        assertTrue(opts.any { it.first == "12" && it.second == "RentoMojo Limited" })
    }

    @Test
    fun `cell text strips tags and entities`() {
        assertEquals("Smith & Wesson", RegistrarDirectory.cellText("<b>Smith &amp; Wesson</b>"))
        assertEquals("A B", RegistrarDirectory.cellText("A&nbsp;&nbsp;B"))
    }

    // ---- merge rules ----

    @Test
    fun `authoritative entries are never replaced, dates backfill`() {
        val dir = mutableMapOf(
            canonIpoName("Manika Plastech") to DirectoryEntry("mufg", "Manika Plastech", authoritative = true),
            canonIpoName("Foo Bars Ltd") to DirectoryEntry("kfin", "Foo Bars Ltd")
        )
        RegistrarDirectory.mergeInto(
            dir,
            listOf(
                DirectoryEntry("bigshare", "Manika Plastech", "21 Sep 2026"),
                DirectoryEntry("kfin", "Foo Bars Ltd", "19 Sep 2026"),
                DirectoryEntry("kfin", "New Co Ltd", "22 Sep 2026")
            ),
            ::canonIpoName
        )
        // MUFG stays MUFG, but gains the published date.
        val manika = dir[canonIpoName("Manika Plastech")]!!
        assertEquals("mufg", manika.registrar)
        assertEquals("21 Sep 2026", manika.allotmentDate)
        // Weak entry gains its date; genuinely new rows are added.
        assertEquals("19 Sep 2026", dir[canonIpoName("Foo Bars Ltd")]!!.allotmentDate)
        assertEquals("kfin", dir[canonIpoName("New Co Ltd")]!!.registrar)
    }

    // ---- display + automation flags ----

    @Test
    fun `only mufg and kfin are automated`() {
        assertTrue(DirectoryEntry("mufg", "X", authoritative = true).automated())
        assertTrue(DirectoryEntry("kfin", "X").automated())
        for (r in listOf("bigshare", "skyline", "cameo", "maashitla", "purva", "beetal")) {
            assertTrue(!DirectoryEntry(r, "X").automated())
        }
        assertEquals("MUFG Intime", DirectoryEntry("mufg", "X").displayName())
        assertEquals("KFintech", DirectoryEntry("kfin", "X").displayName())
        assertEquals("Unknown", DirectoryEntry("nope", "X").displayName())
    }
}
