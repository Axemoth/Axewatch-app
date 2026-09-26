package com.aistudio.axewatch.trader.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins GMP table selection + header aliasing against live-observed shapes:
 * InvestorGain serves explicit SHNI/BHNI columns, IPOWatch serves
 * QIB/NII/Retail/Total only, and table positions are layout, not contract.
 */
class IpoGmpTablesTest {

    private val svc = IpoGmpService()

    private fun table(vararg cols: String, nrows: Int = 2): IpoGmpService.ParsedTable {
        val colMap = cols.mapIndexed { i, c -> c to i }.toMap()
        val rows = (0 until nrows).map { r -> cols.associateWith { "v$r" } }
        return IpoGmpService.ParsedTable(cols.toList(), colMap, rows)
    }

    @Test
    fun `investorgain shni bhni headers map granular`() {
        val sub = IpoGmpService.SUB_COLUMN_ALIASES
        assertEquals("shni", svc.matchColumnAlias("SHNI ▲ ▼", sub))
        assertEquals("bhni", svc.matchColumnAlias("BHNI ▲ ▼", sub))
        assertEquals("qib", svc.matchColumnAlias("QIB ▲ ▼", sub))
        assertEquals("nii", svc.matchColumnAlias("NII ▲ ▼", sub))
        assertEquals("retail", svc.matchColumnAlias("RII ▲ ▼", sub))
        assertEquals("total", svc.matchColumnAlias("Total ▲ ▼", sub))
        assertEquals("close_date", svc.matchColumnAlias("Closing Date ▲ ▼", sub))
    }

    @Test
    fun `ipowatch fallback headers map without shni bhni`() {
        val sub = IpoGmpService.SUB_COLUMN_ALIASES
        assertEquals("qib", svc.matchColumnAlias("QIB  (X)", sub))
        assertEquals("nii", svc.matchColumnAlias("NII  (X)", sub))
        assertEquals("retail", svc.matchColumnAlias("Retail  (X)", sub))
        assertEquals("total", svc.matchColumnAlias("Total (X)", sub))
    }

    @Test
    fun `investorgain gmp headers map and listing date stays unmapped`() {
        val cols = IpoGmpService.COLUMN_ALIASES
        assertEquals("name", svc.matchColumnAlias("Name ▲ ▼", cols))
        assertEquals("gmp", svc.matchColumnAlias("GMP ▲ ▼", cols))
        assertEquals("sub_x", svc.matchColumnAlias("Sub", cols))
        assertEquals("price", svc.matchColumnAlias("Price (₹) ▲ ▼", cols))
        assertEquals("size", svc.matchColumnAlias("IPO Size ▲ ▼", cols))
        assertEquals("lot", svc.matchColumnAlias("Lot", cols))
        assertEquals("boa_dt", svc.matchColumnAlias("BoA Dt ▲ ▼", cols))
        assertEquals("updated", svc.matchColumnAlias("Updated-On ▲ ▼", cols))
        // "Listing" is the listing DATE, not a price — must never feed GMP math.
        assertNull(svc.matchColumnAlias("Listing", cols))
        assertNull(svc.matchColumnAlias("Rating ▲ ▼", cols))
    }

    @Test
    fun `ipowatch gmp headers map including status`() {
        val cols = IpoGmpService.COLUMN_ALIASES
        assertEquals("name", svc.matchColumnAlias("IPO Name", cols))
        assertEquals("gmp", svc.matchColumnAlias("IPO GMP*", cols))
        assertEquals("est_listing", svc.matchColumnAlias("Est. Listing", cols))
        assertEquals("status", svc.matchColumnAlias("Status", cols))
    }

    @Test
    fun `gmp tables skip non gmp tables and cap at two`() {
        val promo = table("name", "note")
        val main = table("name", "gmp", "status")
        val sme = table("name", "gmp", "status")
        val past = table("name", "gmp", "listing_price")
        val picked = svc.pickGmpTables(listOf(promo, main, sme, past))
        assertEquals(2, picked.size)
        assertEquals("Mainboard", picked[0].second)
        assertEquals("SME", picked[1].second)
        assertEquals(main, picked[0].first)
        assertEquals(sme, picked[1].first)
    }

    @Test
    fun `past table found by listing price column at any index`() {
        val main = table("name", "gmp", "status")
        val past = table("name", "price", "gmp", "listing_price")
        assertEquals(past, svc.pickPastTable(listOf(main, past)))
        assertEquals(past, svc.pickPastTable(listOf(past, main)))
        assertNull(svc.pickPastTable(listOf(main)))
    }

    @Test
    fun `performance headers keep issue listing and current prices separate`() {
        val aliases = IpoGmpService.PERF_COLUMN_ALIASES
        assertEquals("price", svc.matchColumnAlias("Price ▲ ▼", aliases))
        assertEquals("listing_price", svc.matchColumnAlias("Listing Price ▲ ▼", aliases))
        assertEquals("current_price", svc.matchColumnAlias("Closing Price (LTP) ▲ ▼", aliases))
        assertEquals("listing_date", svc.matchColumnAlias("Listing Dt ▲ ▼", aliases))
    }

    @Test
    fun `live shaped performance rows retain each issuer own prices`() {
        val html = """<table><thead><tr><th>IPO ▲ ▼</th><th>Symbol ▲ ▼</th><th>Listing Dt ▲ ▼</th><th>Sub ▲ ▼</th><th>Price ▲ ▼</th><th>Listing Price ▲ ▼</th><th>Closing Price (LTP) ▲ ▼</th></tr></thead><tbody>
          <tr><td>Axiom Gas Engineering SME</td><td>AXIOMGAS</td><td>25-Sep-26</td><td>1.39x</td><td>₹54.00</td><td>₹54.75 (1.39%)</td><td>₹54.05 (0.09%)</td></tr>
          <tr><td>NSE</td><td>544937</td><td>24-Sep-26</td><td>5.71x</td><td>₹1785.00</td><td>₹1800.00 (0.84%)</td><td>₹1792.65 (0.43%)</td></tr>
        </tbody></table>"""
        val rows = svc.parseInvestorGainPastRows(svc.parseTables(html, IpoGmpService.PERF_COLUMN_ALIASES))
        assertEquals(2, rows.size)
        assertEquals(54.0, rows[0].issuePrice, 0.001)
        assertEquals(54.75, rows[0].listingPrice, 0.001)
        assertEquals(54.05, rows[0].currentPrice, 0.001)
        assertEquals("SME", rows[0].category)
        assertEquals(1785.0, rows[1].issuePrice, 0.001)
        assertEquals(1800.0, rows[1].listingPrice, 0.001)
        assertEquals(1792.65, rows[1].currentPrice, 0.001)
    }

    @Test
    fun `missing listing price remains unavailable`() {
        val html = """<table><tr><th>IPO</th><th>Price</th><th>Listing Price</th><th>Closing Price (LTP)</th></tr>
          <tr><td>Example Industries</td><td>₹100</td><td>—</td><td>₹104</td></tr></table>"""
        val rows = svc.parseInvestorGainPastRows(svc.parseTables(html, IpoGmpService.PERF_COLUMN_ALIASES))
        assertEquals(1, rows.size)
        assertEquals(0.0, rows[0].listingPrice, 0.001)
        assertEquals(104.0, rows[0].currentPrice, 0.001)
    }

    @Test
    fun `ambiguous partial company name never merges market data`() {
        val keys = setOf("orientelectronics", "orientengineering")
        assertNull(svc.uniqueNormalizedMatch(keys, "orient"))
        assertNull(svc.uniqueNormalizedMatch(setOf("orientcables", "orientcablesltd"), "orientcablesltdindia"))
        assertEquals("orientelectronics", svc.uniqueNormalizedMatch(keys, "orientelectronicsindia"))
    }
}
