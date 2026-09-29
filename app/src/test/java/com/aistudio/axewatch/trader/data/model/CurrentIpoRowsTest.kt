package com.aistudio.axewatch.trader.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrentIpoRowsTest {
    private fun issue(name: String, status: String, sub: Double = 0.0) = IpoIssue(
        symbol = name.uppercase().replace(" ", ""), companyName = name,
        category = "Mainboard", status = status,
        issueOpenDate = "", issueCloseDate = "", priceBand = "—",
        issuePrice = 0.0, lotSize = 0, issueSizeCr = 0.0, registrar = "Unknown",
        totalSub = sub
    )

    private fun gmp(name: String, status: String, amount: Double) = GmpItem(
        companyName = name, symbol = name.uppercase().replace(" ", ""),
        issuePrice = 100.0, gmpAmount = amount, gmpPercent = amount,
        estListingPrice = 100 + amount, status = status, fireRating = 1,
        lastUpdated = "", category = "SME"
    )

    @Test fun `open issues lead upcoming and use measured gmp with preserved subscription`() {
        val rows = currentIpoRows(
            listOf(issue("Alpha Industries", "Active", 4.2),
                issue("Beta Industries", "Active", 2.1),
                issue("Gamma Industries", "Forthcoming"),
                issue("Old Industries", "Closed")),
            listOf(gmp("Alpha Industries", "Open", 35.0),
                gmp("Beta Industries", "Open", 70.0),
                gmp("Gamma Industries", "Upcoming", 90.0))
        )
        assertEquals(listOf("Beta Industries", "Alpha Industries", "Gamma Industries"),
            rows.map { it.issue.companyName })
        assertEquals(2.1, rows[0].issue.totalSub, 0.001)
        assertEquals(70.0, rows[0].issue.gmpAmount, 0.001)
        assertEquals("Mainboard", rows[0].issue.category)
        assertEquals(1, rows.last().stage)
    }

    @Test fun `gmp only row has unknown subscription and category from its source`() {
        val rows = currentIpoRows(emptyList(), listOf(gmp("New SME Company", "Open", 20.0)))
        assertEquals(1, rows.size)
        assertEquals("SME", rows[0].issue.category)
        assertEquals(0.0, rows[0].issue.totalSub, 0.001)
        assertTrue(rows[0].issue.issueCloseDate.isBlank())
    }

    @Test fun `closed Robokidz cannot remain open or reappear as a gmp only row`() {
        val robokidz = issue("Robokidz Eduventures", "Active").copy(
            issueOpenDate = "21 Sep 2026", issueCloseDate = "23 Sep 2026")
        val past = PastIpoItem(robokidz.symbol, robokidz.companyName, 106.0, 0.0,
            listingGainPercent = 0.0, issueCloseDate = robokidz.issueCloseDate)
        val rows = currentIpoRows(listOf(robokidz),
            listOf(gmp(robokidz.companyName, "Open", 20.0)), listOf(past), 20260929)
        assertTrue(rows.isEmpty())
        assertTrue(currentIpoRows(emptyList(), listOf(gmp(robokidz.companyName, "Open", 20.0)),
            listOf(past), 20260929).isEmpty())
        assertEquals("Bidding closed", robokidz.getAllotmentBadge(20260929).label)
    }

    @Test fun `recorded dates outrank stale forthcoming and open labels`() {
        val open = issue("Open Today", "Forthcoming").copy(
            issueOpenDate = "28 Sep 2026", issueCloseDate = "30 Sep 2026")
        val future = issue("Future Issue", "Active").copy(
            issueOpenDate = "03 Oct 2026", issueCloseDate = "05 Oct 2026")
        val rows = currentIpoRows(listOf(open, future), emptyList(), todayKey = 20260929)
        assertEquals(listOf(0, 1), rows.map { it.stage })
        assertTrue(!open.isBiddingNotStarted(20260929))
    }
}
