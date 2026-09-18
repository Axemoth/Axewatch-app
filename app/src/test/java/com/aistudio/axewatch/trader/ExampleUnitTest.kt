package com.aistudio.axewatch.trader

import com.aistudio.axewatch.trader.data.local.entity.AllotmentRecordEntity
import com.aistudio.axewatch.trader.data.local.entity.PanVaultEntity
import com.aistudio.axewatch.trader.data.local.entity.maskMatches
import com.aistudio.axewatch.trader.data.provider.IndexConstituentsProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testIndexConstituentsProvider() {
        // Known indices ship static *composition* (membership/names/sectors/weights)
        // whose live prices come only from the quote feed — the provider carries
        // 0.0 prices so the UI must overlay live quotes and render "—" otherwise.
        val nifty50 = IndexConstituentsProvider.getConstituentsForIndex("NIFTY 50")
        assertTrue("Nifty 50 constituents should not be empty", nifty50.isNotEmpty())
        assertTrue("Nifty 50 should contain HDFCBANK", nifty50.any { it.symbol == "HDFCBANK" })
        assertTrue("Nifty 50 should contain RELIANCE", nifty50.any { it.symbol == "RELIANCE" })
        assertTrue(
            "Provider prices must stay unknown (0.0) so no static number can pose as a live quote",
            nifty50.filter { it.symbol == "HDFCBANK" || it.symbol == "RELIANCE" }
                .all { it.lastPrice == 0.0 && it.change == 0.0 && it.percentChange == 0.0 }
        )

        val bankNifty = IndexConstituentsProvider.getConstituentsForIndex("NIFTY BANK")
        assertTrue("Bank Nifty should have bank stocks", bankNifty.any { it.symbol == "SBIN" })

        // The shared static rows come from the static list (not a live feed), so
        // they must carry unknown prices — never a frozen quote posing as live.
        assertTrue(
            "Static list must not carry prices that could pose as live quotes",
            bankNifty.filter { it.symbol == "HDFCBANK" || it.symbol == "RELIANCE" }
                .all { it.lastPrice == 0.0 }
        )

        // Same for the shared sector lists.
        val niftyIt = IndexConstituentsProvider.getConstituentsForIndex("NIFTY IT")
        assertTrue("Nifty IT should have TCS", niftyIt.any { it.symbol == "TCS" })
        assertTrue(niftyIt.all { it.lastPrice == 0.0 })
    }

    @Test
    fun allProviderListsCarryUnknownPrices() {
        // Sweep every routable index: membership (symbol/name/sector/weight)
        // is static, but no row may carry a price that could pose as live.
        val inputs = listOf(
            "NIFTY 50", "NIFTY", "^NSEI",
            "NIFTY BANK", "NIFTY FINANCE", "NIFTY MIDCAP 50",
            "NIFTY IT", "SENSEX"
        )
        for (input in inputs) {
            val list = IndexConstituentsProvider.getConstituentsForIndex(input)
            assertTrue("$input must resolve to a membership list", list.isNotEmpty())
            assertTrue(
                "$input rows must carry unknown (0.0) prices, never static quotes",
                list.all { it.lastPrice == 0.0 && it.change == 0.0 && it.percentChange == 0.0 }
            )
        }
    }

    @Test
    fun unknownIndexSymbolReturnsEmptyConstituents() {
        // An unmatched index must return NOTHING, never a mislabeled NIFTY-50 list.
        assertTrue(IndexConstituentsProvider.getConstituentsForIndex("NIFTY AUTO").isEmpty())
        assertTrue(IndexConstituentsProvider.getConstituentsForIndex("INDIA VIX").isEmpty())
        assertTrue(IndexConstituentsProvider.getConstituentsForIndex("").isEmpty())
    }

    @Test
    fun testAllotmentRecordEntityStatuses() {
        val allotted = AllotmentRecordEntity(
            maskedPan = "AB*****F",
            ipoSymbol = "SWIGGY",
            ipoName = "Swiggy Ltd",
            sharesApplied = 38,
            sharesAllotted = 38,
            status = "ALLOTTED",
            registrar = "Link Intime"
        )
        assertTrue(allotted.isAllotted)
        assertEquals("Allotted", allotted.statusLabel)

        val notAllotted = allotted.copy(status = "NOT_ALLOTTED", sharesAllotted = 0)
        assertTrue(notAllotted.isNotAllotted)
        assertEquals("Not Allotted", notAllotted.statusLabel)

        val notApplied = allotted.copy(status = "NOT_APPLIED", sharesAllotted = 0)
        assertTrue(notApplied.isNotApplied)
        assertEquals("Not Applied", notApplied.statusLabel)

        val resultsNotOut = allotted.copy(status = "RESULTS_NOT_OUT", sharesAllotted = 0)
        assertTrue(resultsNotOut.isResultsNotOut)
        assertEquals("Results Not Out Yet", resultsNotOut.statusLabel)
    }

    // ---- PAN mask unification (PII safety: only AB*****F may be displayed) ----

    @Test
    fun panVaultMaskShowsOnlyFirstTwoAndLastChar() {
        // The old "ABCDE****F" mask leaked 5 of 10 PAN characters.
        assertEquals("AB*****F", PanVaultEntity(panNumber = "ABCDE1234F", holderName = "Self").maskedPan)
        assertEquals("AB*****F", PanVaultEntity(panNumber = " abcde1234f ", holderName = "Self").maskedPan)
    }

    @Test
    fun maskMatchesSupportsLegacyAndCurrentFormats() {
        // Legacy records ("ABCDE****F", 5 visible chars) must still resolve to
        // the same PAN as the compliant vault mask ("AB*****F").
        assertTrue(maskMatches("AB*****F", "ABCDE****F"))
        assertTrue(maskMatches("ABCDE****F", "AB*****F"))
        // Identical masks (the normal path) match trivially.
        assertTrue(maskMatches("AB*****F", "AB*****F"))
    }

    @Test
    fun maskMatchesRejectsDifferentPans() {
        // Different prefix (first two visible chars differ).
        assertFalse(maskMatches("AB*****F", "ZZWXY****F"))
        // Different final char.
        assertFalse(maskMatches("AB*****F", "ABCDE****P"))
        // Star-only / empty masks match nothing.
        assertFalse(maskMatches("AB*****F", "*****"))
        assertFalse(maskMatches("*****", "ABCDE****F"))
    }
}
