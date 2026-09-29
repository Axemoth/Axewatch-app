package com.aistudio.axewatch.trader.data.repository

import android.content.Context
import com.aistudio.axewatch.trader.data.model.IpoIssue
import com.aistudio.axewatch.trader.data.model.PastIpoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class IpoHistoryCacheTest {
    private fun issue(name: String, total: Double) = IpoIssue(
        symbol = name.uppercase().replace(" ", ""), companyName = name,
        category = "SME", status = "Active", issueOpenDate = "21 Sep 2026",
        issueCloseDate = "23 Sep 2026", priceBand = "₹106", issuePrice = 106.0,
        lotSize = 1200, issueSizeCr = 31.0, registrar = "Maashitla",
        qibSub = 0.7, niiSub = 8.6, shniSub = 9.5, bhniSub = 8.1,
        riiSub = 14.8, totalSub = total
    )

    @Test fun `recent subscription and registrar survive restart and sparse refresh`() {
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences(
            "ipo_history_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val cache = IpoHistoryCache(prefs)
        cache.record(listOf(issue("Robokidz Eduventures", 9.29)), 20260924)
        val restarted = IpoHistoryCache(prefs)
        val sparse = issue("Robokidz Eduventures", 0.0).copy(
            registrar = "Unknown", category = "Unknown", issueCloseDate = "",
            qibSub = 0.0, niiSub = 0.0, shniSub = 0.0,
            bhniSub = 0.0, riiSub = 0.0)
        restarted.record(listOf(sparse), 20260929)
        val past = mergePastIpoHistory(emptyList(), IpoHistoryCache(prefs).issues(), 20260929)
        assertEquals(1, past.size)
        assertEquals("Maashitla", past.single().registrar)
        assertEquals("SME", past.single().category)
        assertEquals(9.29, past.single().totalSub, 0.001)
        assertEquals(9.5, past.single().shniSub, 0.001)
        assertEquals(0.0, past.single().listingPrice, 0.001)
        assertTrue(past.single().listingDate.isBlank())
    }

    @Test fun `listing join keeps its own values and never borrows another company's subscription`() {
        val issue = issue("Robokidz Eduventures", 9.29)
        val listed = PastIpoItem(issue.symbol, issue.companyName, 106.0, 115.0,
            listingGainPercent = 8.5, listingDate = "28 Sep 2026")
        val other = PastIpoItem("OTHER", "Other Industries", 100.0, 90.0,
            listingGainPercent = -10.0, listingDate = "28 Sep 2026")
        val merged = mergePastIpoHistory(listOf(listed, other), listOf(issue), 20260929)
        assertEquals(115.0, merged[0].listingPrice, 0.001)
        assertEquals(9.29, merged[0].totalSub, 0.001)
        assertEquals("Maashitla", merged[0].registrar)
        assertEquals(0.0, merged[1].totalSub, 0.001)
        assertEquals("Unknown", merged[1].registrar)
    }

    @Test fun `closed issue remains cached after one week and expires after sixty days`() {
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences(
            "ipo_history_retention_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val cache = IpoHistoryCache(prefs)
        cache.record(listOf(issue("Robokidz Eduventures", 9.29)), 20260923)
        cache.record(emptyList(), 20261001)
        assertEquals(1, IpoHistoryCache(prefs).issues().size)
        cache.record(emptyList(), 20261123)
        assertTrue(IpoHistoryCache(prefs).issues().isEmpty())
    }
}
