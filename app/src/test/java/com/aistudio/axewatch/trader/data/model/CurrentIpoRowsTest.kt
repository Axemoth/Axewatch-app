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
}
