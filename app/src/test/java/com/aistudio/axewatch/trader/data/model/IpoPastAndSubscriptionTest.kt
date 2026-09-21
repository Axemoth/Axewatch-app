package com.aistudio.axewatch.trader.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class IpoPastAndSubscriptionTest {

    @Test
    fun `past ipo item computes accurate gain percentages`() {
        val issuePx = 186.0
        val listingPx = 250.0
        val curPx = 249.4

        val listingGain = (((listingPx - issuePx) / issuePx) * 100.0 * 100.0).roundToInt() / 100.0
        val curGain = (((curPx - issuePx) / issuePx) * 100.0 * 100.0).roundToInt() / 100.0

        val item = PastIpoItem(
            symbol = "544925",
            companyName = "Maharaja & Speedex India",
            issuePrice = issuePx,
            listingPrice = listingPx,
            currentPrice = curPx,
            listingGainPercent = listingGain,
            currentGainPercent = curGain,
            totalSub = 32.41,
            listingDate = "18-Sep-26"
        )

        assertEquals(34.41, item.listingGainPercent, 0.01)
        assertEquals(34.09, item.currentGainPercent, 0.01)
        assertEquals(curPx, item.currentPrice, 0.001)
        assertTrue(item.currentGainPercent > 0)
    }

    @Test
    fun `past ipo item handles listing loss correctly`() {
        val issuePx = 100.0
        val listingPx = 99.0
        val curPx = 95.95

        val listingGain = (((listingPx - issuePx) / issuePx) * 100.0 * 100.0).roundToInt() / 100.0
        val curGain = (((curPx - issuePx) / issuePx) * 100.0 * 100.0).roundToInt() / 100.0

        val item = PastIpoItem(
            symbol = "544930",
            companyName = "Injecto Polymers",
            issuePrice = issuePx,
            listingPrice = listingPx,
            currentPrice = curPx,
            listingGainPercent = listingGain,
            currentGainPercent = curGain,
            totalSub = 1.23,
            listingDate = "21-Sep-26"
        )

        assertEquals(-1.0, item.listingGainPercent, 0.01)
        assertEquals(-4.05, item.currentGainPercent, 0.01)
        assertTrue(item.currentGainPercent < 0)
    }

    @Test
    fun `ipo issue retains granular shni and bhni subscription values`() {
        val issue = IpoIssue(
            symbol = "ROBOKIDZ",
            companyName = "Robokidz Eduventures",
            category = "SME",
            status = "Active",
            issueOpenDate = "19-Sep-2026",
            issueCloseDate = "23-Sep-2026",
            priceBand = "₹106",
            issuePrice = 106.0,
            lotSize = 1200,
            issueSizeCr = 31.09,
            registrar = "Link Intime",
            qibSub = 0.07,
            niiSub = 8.63,
            shniSub = 9.52,
            bhniSub = 8.19,
            riiSub = 14.81,
            totalSub = 9.29,
            gmpAmount = 55.0,
            gmpPercent = 51.89
        )

        assertEquals(9.52, issue.shniSub, 0.001)
        assertEquals(8.19, issue.bhniSub, 0.001)
        assertEquals(8.63, issue.niiSub, 0.001)
        assertEquals(0.07, issue.qibSub, 0.001)
        assertEquals(14.81, issue.riiSub, 0.001)
        assertEquals(9.29, issue.totalSub, 0.001)
        assertTrue(!issue.isBiddingNotStarted())
    }
}
