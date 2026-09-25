package com.aistudio.axewatch.trader.data.model

import com.aistudio.axewatch.trader.data.remote.DirectoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pins the declaration watcher's due-decision: declared-only, deduped, split auto/manual. */
class AllotWatcherTest {

    @Test
    fun `blocked saved result remains pending while delivered result is deduped`() {
        val pan = com.aistudio.axewatch.trader.data.local.entity.PanVaultEntity("ABCDE1234F", "Self")
        val record = com.aistudio.axewatch.trader.data.local.entity.AllotmentRecordEntity(
            maskedPan = pan.maskedPan, ipoSymbol = "ACME", ipoName = "Acme", sharesApplied = 10,
            sharesAllotted = 10, status = "ALLOTTED", registrar = "MUFG", checkedAt = 1
        )
        val records = listOf(record, record.copy(status = "LOOKUP_FAILED", checkedAt = 2))
        assertEquals(1, pendingAllotNotifications(records, listOf(pan), emptySet()).size)
        assertTrue(pendingAllotNotifications(records, listOf(pan), setOf(allotNotifyKey("ACME", pan.maskedPan))).isEmpty())
        val collision = pan.copy(panNumber = "ABZZZ9999F")
        assertTrue(pendingAllotNotifications(records, listOf(pan, collision), emptySet()).isEmpty())
    }

    private fun issue(
        symbol: String,
        registrar: String = "MUFG Intime",
        date: String = "",
        status: String = "Closed"
    ) = WatchedIssue(symbol = symbol, name = "$symbol Ltd", registrar = registrar, allotmentDate = date, status = status)

    @Test
    fun `declared by past allotment date only`() {
        val today = 20260922L
        assertTrue(isDeclaredOut(issue("A", date = "21 Sep 2026"), todayKey = today))
        assertTrue(isDeclaredOut(issue("A", date = "22 Sep 2026"), todayKey = today))
        assertFalse(isDeclaredOut(issue("A", date = "25 Sep 2026"), todayKey = today))
        assertFalse(isDeclaredOut(issue("A"), todayKey = today))
        // Forthcoming with a date is not declared.
        assertFalse(isDeclaredOut(issue("A", date = "21 Sep 2026", status = "Forthcoming"), todayKey = today))
    }

    @Test
    fun `registrar listing alone does not prove results are declared`() {
        val dir = mapOf("zzz" to DirectoryEntry("bigshare", "Acme Forgings Ltd", authoritative = true))
        assertFalse(
            isDeclaredOut(
                WatchedIssue("ACME", "Acme Forgings Ltd", "Bigshare", status = "Active"),
                dir = dir, todayKey = 20260922L
            )
        )
    }

    @Test
    fun `allotted listed status declares without date`() {
        assertTrue(isDeclaredOut(issue("A", status = "Allotted"), todayKey = 20260922L))
        assertTrue(isDeclaredOut(issue("A", status = "Listed"), todayKey = 20260922L))
    }

    @Test
    fun `due splits auto vs manual and skips decided and notified`() {
        val snapshot = listOf(
            issue("M1", registrar = "MUFG Intime", date = "21 Sep 2026"),
            issue("K1", registrar = "KFintech", date = "21 Sep 2026"),
            issue("B1", registrar = "Bigshare", date = "21 Sep 2026"),
            issue("F1", registrar = "MUFG Intime", date = "25 Sep 2026"),
            issue("U1", registrar = "MUFG Intime")
        )
        val due = computeDueIssues(
            snapshot, decidedSymbols = setOf("K1"), notifiedSymbols = emptySet(), todayKey = 20260922L
        )
        assertEquals(listOf("M1"), due.auto.map { it.symbol })
        assertEquals(listOf("B1"), due.manual.map { it.symbol })

        // Manual nudge already sent: dropped.
        val due2 = computeDueIssues(
            snapshot, notifiedSymbols = setOf("B1"), todayKey = 20260922L
        )
        assertTrue(due2.manual.none { it.symbol == "B1" })
    }

    @Test
    fun `fresh directory registrar beats stale snapshot unknown`() {
        val dir = mapOf("zzz" to DirectoryEntry("mufg", "Acme Forgings Ltd", "21 Sep 2026", authoritative = true))
        val due = computeDueIssues(
            listOf(WatchedIssue("ACME", "Acme Forgings Ltd", "Unknown", status = "Active")),
            dir = dir, todayKey = 20260922L
        )
        assertEquals(listOf("ACME"), due.auto.map { it.symbol })
        assertEquals("mufg", due.auto.first().registrar)
        assertTrue(due.manual.isEmpty())
    }

    @Test
    fun `auto checkable covers registrar name variants`() {
        assertTrue(registrarAutoCheckable("mufg"))
        assertTrue(registrarAutoCheckable("MUFG Intime"))
        assertTrue(registrarAutoCheckable("Link Intime"))
        assertTrue(registrarAutoCheckable("kfin"))
        assertTrue(registrarAutoCheckable("KFintech"))
        assertTrue(registrarAutoCheckable("KFin Technologies"))
        assertFalse(registrarAutoCheckable("Bigshare"))
        assertFalse(registrarAutoCheckable("Skyline"))
        assertFalse(registrarAutoCheckable("Unknown"))
        assertFalse(registrarAutoCheckable(""))
    }

    @Test
    fun `notify keys are stable and text uses masked pan only`() {
        assertEquals("FARMPEACE|AB*****F", allotNotifyKey("FARMPEACE", "AB*****F"))
        val (title, body) = AllotNotifyText.allotted("Farm Peace", "Father", "AB*****F", 2)
        assertTrue(title.contains("Farm Peace"))
        assertTrue(body.contains("Father") && body.contains("AB*****F") && body.contains("2 shares"))
        val (t2, _) = AllotNotifyText.manualNudge("Lumino", "Bigshare")
        assertTrue(t2.contains("Lumino"))
    }
}
