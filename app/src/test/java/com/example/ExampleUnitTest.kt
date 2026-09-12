package com.example

import com.example.data.local.entity.AllotmentRecordEntity
import com.example.data.provider.IndexConstituentsProvider
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
        val nifty50 = IndexConstituentsProvider.getConstituentsForIndex("NIFTY 50")
        assertTrue("Nifty 50 constituents should not be empty", nifty50.isNotEmpty())
        assertTrue("Nifty 50 should contain HDFCBANK", nifty50.any { it.symbol == "HDFCBANK" })
        assertTrue("Nifty 50 should contain RELIANCE", nifty50.any { it.symbol == "RELIANCE" })

        val bankNifty = IndexConstituentsProvider.getConstituentsForIndex("NIFTY BANK")
        assertTrue("Bank Nifty should have bank stocks", bankNifty.any { it.symbol == "SBIN" })

        val niftyIt = IndexConstituentsProvider.getConstituentsForIndex("NIFTY IT")
        assertTrue("Nifty IT should have TCS", niftyIt.any { it.symbol == "TCS" })
    }

    @Test
    fun testAllotmentRecordEntityStatuses() {
        val allotted = AllotmentRecordEntity(
            maskedPan = "ABCDE****F",
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
}
