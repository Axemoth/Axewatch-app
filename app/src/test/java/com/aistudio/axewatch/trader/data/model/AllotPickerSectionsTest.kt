package com.aistudio.axewatch.trader.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pins the web-dashboard-mirrored picker lifecycle sections + registrar strip. */
class AllotPickerSectionsTest {

    private fun issue(
        status: String,
        close: String = "",
        open: String = "",
        date: String = "",
        registrar: String = "Unknown"
    ) = IpoIssue(
        symbol = "X", companyName = "X Co", category = "Mainboard",
        status = status, issueOpenDate = open, issueCloseDate = close,
        priceBand = "", issuePrice = 0.0, lotSize = 0, issueSizeCr = 0.0,
        registrar = registrar, allotmentDate = date
    )

    private fun cal20260922() = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.YEAR, 2026)
        set(java.util.Calendar.MONTH, java.util.Calendar.SEPTEMBER)
        set(java.util.Calendar.DAY_OF_MONTH, 22)
        set(java.util.Calendar.HOUR_OF_DAY, 12)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }

    @Test
    fun `days since counts calendar days across month boundary`() {
        val now = cal20260922()
        assertEquals(30L, daysSinceLooseDate("23 Aug 2026", now))
        assertEquals(2L, daysSinceLooseDate("20 Sep 2026", now))
        assertEquals(-3L, daysSinceLooseDate("25 Sep 2026", now))
        assertNull(daysSinceLooseDate("TBA", now))
        assertNull(daysSinceLooseDate("", now))
    }

    @Test
    fun `decided or declared beats everything`() {
        val now = cal20260922()
        // Hand-logged / registrar-answered outcome forces section 0.
        assertEquals(0, allotPickerSection(issue("Closed", close = "20 Sep 2026"), decided = true, now = now))
        // Declared allotment date passed forces section 0 even when closed.
        assertEquals(
            0,
            allotPickerSection(
                issue("Closed", close = "18 Sep 2026", date = "21 Sep 2026"),
                todayKey = 20260922L, now = now
            )
        )
    }

    @Test
    fun `open upcoming closed older precedence`() {
        val now = cal20260922()
        assertEquals(1, allotPickerSection(issue("Active", open = "10 Sep 2026", close = "24 Sep 2026"), now = now))
        assertEquals(2, allotPickerSection(issue("Forthcoming", open = "25 Sep 2026"), now = now))
        assertEquals(2, allotPickerSection(issue("Active", open = "25 Sep 2026"), now = now))
        // Recent close, no declared date: awaiting.
        assertEquals(3, allotPickerSection(issue("Closed", close = "20 Sep 2026"), now = now))
        // Exactly 30d is still awaiting; 31d is older.
        assertEquals(3, allotPickerSection(issue("Closed", close = "23 Aug 2026"), now = now))
        assertEquals(4, allotPickerSection(issue("Closed", close = "22 Aug 2026"), now = now))
    }

    @Test
    fun `allotment list has open and last thirty days only`() {
        val now = cal20260922()
        assertTrue(isRecentAllotmentIssue(issue("Active", open = "21 Sep 2026", close = "25 Sep 2026"), now))
        assertTrue(isRecentAllotmentIssue(issue("Closed", close = "23 Aug 2026"), now))
        assertTrue(!isRecentAllotmentIssue(issue("Closed", close = "22 Aug 2026"), now))
        assertTrue(!isRecentAllotmentIssue(issue("Forthcoming", open = "25 Sep 2026"), now))
        assertTrue(!isRecentAllotmentIssue(issue("Active", open = "25 Sep 2026"), now))
    }

    @Test
    fun `registrar counts normalize tracker free text`() {
        val ipos = listOf(
            issue("Active", registrar = "MUFG Intime India"),
            issue("Active", registrar = "Link Intime"),
            issue("Closed", registrar = "KFin Technologies"),
            issue("Closed", registrar = "Bigshare Services"),
            issue("Closed", registrar = "Skyline Financial"),
            issue("Closed", registrar = "Unknown"),
            issue("Closed", registrar = "")
        )
        val counts = registrarCounts(ipos).toMap()
        assertEquals(2, counts["MUFG"])
        assertEquals(1, counts["KFintech"])
        assertEquals(1, counts["Bigshare"])
        assertEquals(1, counts["Skyline"])
        assertEquals(2, counts["Unknown"])
        // Sorted most-first.
        val ordered = registrarCounts(ipos)
        assertTrue(ordered.first().second >= ordered.last().second)
    }
}
