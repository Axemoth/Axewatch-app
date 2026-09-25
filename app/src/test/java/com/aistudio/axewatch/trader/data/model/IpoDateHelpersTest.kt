package com.aistudio.axewatch.trader.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pins the allotment picker's sectioning + recent-first ordering. */
class IpoDateHelpersTest {

    private fun issue(
        status: String,
        close: String = "",
        open: String = "",
        date: String = ""
    ) = IpoIssue(
        symbol = "X", companyName = "X Co", category = "Mainboard",
        status = status, issueOpenDate = open, issueCloseDate = close,
        priceBand = "", issuePrice = 0.0, lotSize = 0, issueSizeCr = 0.0,
        registrar = "Unknown", allotmentDate = date
    )

    @Test
    fun `loose dates parse across tracker formats`() {
        assertEquals(20260918L, parseLooseDate("18 Sep 2026"))
        assertEquals(20260918L, parseLooseDate("18 Sept 2026"))
        assertEquals(20260918L, parseLooseDate("2026-09-18"))
        assertEquals(20260918L, parseLooseDate("18-09-2026"))
        assertEquals(0L, parseLooseDate("—"))
        assertEquals(0L, parseLooseDate("TBA"))
        assertEquals(0L, parseLooseDate(""))
        assertEquals(0L, parseLooseDate("Sep 2026"))
    }

    @Test
    fun `missing year defaults to supplied year`() {
        assertEquals(20260916L, parseLooseDate("16 Sept", nowYear = 2026))
        assertEquals(20250916L, parseLooseDate("16 Sept", nowYear = 2025))
    }

    @Test
    fun `declared date always wins section zero`() {
        assertEquals(0, ipoSection(issue("Active", date = "18 Sep 2026")))
        assertEquals(0, ipoSection(issue("Forthcoming", date = "18 Sep 2026")))
        assertEquals(0, ipoSection(issue("Closed")))
        assertEquals(0, ipoSection(issue("Listed")))
        assertEquals(1, ipoSection(issue("Active")))
        assertEquals(1, ipoSection(issue("Open")))
        assertEquals(2, ipoSection(issue("Forthcoming")))
    }

    @Test
    fun `recency prefers allotment date over close over open`() {
        assertEquals(20260918L, ipoRecencyKey(issue("Closed", close = "10 Sep 2026", date = "18 Sep 2026")))
        assertEquals(20260920L, ipoRecencyKey(issue("Closed", close = "20 Sep 2026")))
        assertEquals(20260910L, ipoRecencyKey(issue("Active", open = "10 Sep 2026")))
        assertEquals(0L, ipoRecencyKey(issue("Active")))
    }

    @Test
    fun `picker ordering puts declared recent first`() {
        val items = listOf(
            issue("Forthcoming", open = "25 Sep 2026"),
            issue("Active", open = "10 Sep 2026", close = "16 Sep 2026"),
            issue("Closed", close = "12 Sep 2026", date = "19 Sep 2026")
        )
        val ordered = items.sortedWith(compareBy({ ipoSection(it) }, { -ipoRecencyKey(it) }))
        assertEquals("Closed", ordered[0].status)
        assertEquals("Active", ordered[1].status)
        assertEquals("Forthcoming", ordered[2].status)
    }

    @Test
    fun `registrar listing identifies the handler without claiming allotment is out`() {
        val ipo = issue("Closed", date = "20 Sep 2026")
        val bigshareDir = mapOf("X" to com.aistudio.axewatch.trader.data.remote.DirectoryEntry("bigshare", "X Co", "20 Sep 2026", true))
        val mufgDir = mapOf("X" to com.aistudio.axewatch.trader.data.remote.DirectoryEntry("mufg", "X Co", "20 Sep 2026", true))

        // Bigshare
        val bigBadge = ipo.getAllotmentBadge(20260922L, bigshareDir)
        org.junit.Assert.assertNotNull(bigBadge)
        assertEquals("Allotment date passed", bigBadge?.label)
        org.junit.Assert.assertFalse(bigBadge?.isGreenCheck == true)
        org.junit.Assert.assertTrue(ipo.isAllotmentOut(20260922L, bigshareDir))

        // Link Intime
        val mufgBadge = ipo.getAllotmentBadge(20260922L, mufgDir)
        org.junit.Assert.assertNotNull(mufgBadge)
        assertEquals("Allotment date passed", mufgBadge?.label)
        org.junit.Assert.assertFalse(mufgBadge?.isGreenCheck == true)
        org.junit.Assert.assertTrue(ipo.isAllotmentOut(20260922L, mufgDir))
    }

    @Test
    fun `allotment today or scheduled date badges`() {
        val ipoToday = issue("Closed", date = "22 Sep 2026")
        val todayBadge = ipoToday.getAllotmentBadge(20260922L, emptyMap())
        org.junit.Assert.assertNotNull(todayBadge)
        assertEquals("Allotment scheduled today", todayBadge?.label)
        org.junit.Assert.assertFalse(todayBadge?.isGreenCheck == true)
        org.junit.Assert.assertTrue(ipoToday.isAllotmentDayOrAfter(20260922L, emptyMap()))

        val ipoPast = issue("Closed", date = "21 Sep 2026")
        val pastBadge = ipoPast.getAllotmentBadge(20260922L, emptyMap())
        org.junit.Assert.assertNotNull(pastBadge)
        assertEquals("Allotment date passed", pastBadge?.label)
        org.junit.Assert.assertFalse(pastBadge?.isGreenCheck == true)
        org.junit.Assert.assertTrue(ipoPast.isAllotmentDayOrAfter(20260922L, emptyMap()))

        val ipoFuture = issue("Active", date = "25 Sep 2026")
        val futureBadge = ipoFuture.getAllotmentBadge(20260922L, emptyMap())
        org.junit.Assert.assertNotNull(futureBadge)
        assertEquals("Allot 25 Sep 2026", futureBadge?.label)
        org.junit.Assert.assertFalse(futureBadge?.isGreenCheck == true)
        org.junit.Assert.assertFalse(ipoFuture.isAllotmentDayOrAfter(20260922L, emptyMap()))
    }

    @Test
    fun `allotment day window crosses month boundary`() {
        val ipo = issue("Closed", date = "31 Aug 2026")
        assertEquals(20260831L, parseLooseDate(ipo.allotmentDate, 2026))
        assertEquals(1L, daysBetweenDateKeys(20260831L, 20260901L))
        org.junit.Assert.assertTrue(ipo.isAllotmentDayOrAfter(20260901L))
        org.junit.Assert.assertTrue(ipo.isAllotmentDayOrAfter(20260902L))
        org.junit.Assert.assertFalse(ipo.isAllotmentDayOrAfter(20260903L))
    }
}
