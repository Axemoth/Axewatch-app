package com.aistudio.axewatch.trader.data.remote

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FundAndQuoteRegressionTest {
    private fun fund(rows: String, code: String = "42") = MutualFundService.parseScheme("42", """
        {"meta":{"scheme_code":$code,"scheme_name":"Verified Fund","scheme_category":"Equity Scheme - Flexi Cap Fund"},"data":[$rows]}
    """)

    @Test fun navHistoryProducesReturnsAndLeavesUnsupportedMetricsUnknown() {
        val result = fund("""
            {"date":"01-10-2026","nav":"121.0000"},
            {"date":"30-09-2026","nav":"120.0000"},
            {"date":"01-10-2025","nav":"110.0000"},
            {"date":"01-10-2023","nav":"90.0000"}
        """)!!
        assertEquals(121.0, result.nav, 0.0001)
        assertEquals(10.0, result.return1Yr!!, 0.03)
        assertEquals(10.37, result.return3Yr!!, 0.03)
        assertEquals("Flexi Cap", result.category)
        assertEquals("01-10-2026", result.navDate)
        assertNull(result.return5Yr)
        assertNull(result.expenseRatio)
        assertNull(result.aumCr)
        assertNull(result.equityPercent)
        assertEquals("", result.riskLevel)
    }

    @Test fun sparseInvalidOrMismatchedNavCannotInventReturns() {
        val result = fund("""{"date":"01-10-2026","nav":"10"},{"date":"01-01-2025","nav":"8"},{"date":"bad","nav":"12"}""")!!
        assertNull(result.return1Yr)
        assertNull(fund("""{"date":"01-10-2026","nav":"0"}"""))
        assertNull(fund("""{"date":"01-10-2026","nav":"10"}""", "43"))
        val single = fund("""{"date":"01-10-2026","nav":"10.1234"}""")!!
        assertNull(single.navPrev)
        assertNull(single.dayChangePercent)
        assertEquals(10.1234, single.nav, 0.00001)
    }

    @Test fun categoriesMatchPickerLabelsWithoutMixingLargeAndMidCap() {
        assertEquals("Large & Mid Cap", MutualFundService.categoryFor("Equity Schemes - Large & Mid Cap Fund"))
        assertEquals("Index", MutualFundService.categoryFor("Other Scheme - Index Funds"))
        assertEquals("Hybrid", MutualFundService.categoryFor("Hybrid Schemes - Aggressive Hybrid Fund"))
        assertTrue(MutualFundService.POPULAR_SCHEMES.any { it.first == "120251" })
        assertFalse(MutualFundService.POPULAR_SCHEMES.any { it.first == "120286" })
    }

    @Test fun mutualFundUnitsUsePublishedNavAndLegacyTypesNormalize() {
        val fund = fund("""{"date":"01-10-2026","nav":"10.1234"}""")!!
        val prices = com.aistudio.axewatch.trader.data.model.portfolioPriceMap(emptyList(), listOf(fund))
        assertEquals(10.1234, prices["42"]!!, 0.00001)
        assertEquals("MUTUAL_FUND", com.aistudio.axewatch.trader.data.model.normalizedHoldingType("mf"))
        assertEquals("MUTUAL_FUND", com.aistudio.axewatch.trader.data.model.normalizedHoldingType("Mutual_Fund"))
        assertEquals("STOCK", com.aistudio.axewatch.trader.data.model.normalizedHoldingType("equity"))
    }

    @Test fun holidayPlaceholderDoesNotTurnDailyStockChangeIntoZero() {
        val result = JSONObject("""{
          "meta":{"regularMarketTime":1790847900,"gmtoffset":19800},
          "timestamp":[1790739900,1790826300,1790912700],
          "indicators":{"quote":[{"close":[1187.0,1167.7,null]}]}
        }""")
        assertEquals(1187.0, YahooFinanceService.previousSessionClose(result)!!, 0.001)
        result.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0)
            .put("close", org.json.JSONArray("[null,1167.7,null]"))
        assertNull(YahooFinanceService.previousSessionClose(result))
    }
}
