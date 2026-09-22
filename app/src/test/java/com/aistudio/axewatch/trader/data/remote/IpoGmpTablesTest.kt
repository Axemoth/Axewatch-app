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
}
