package com.aistudio.axewatch.trader.data.model

import org.junit.Assert.*
import org.junit.Test

class AllotmentIdentityTest {
    private fun issue(name: String, registrar: String) = IpoIssue("COLLISION", name, "SME", "Closed",
        "", "", "100", 100.0, 100, 10.0, registrar)

    @Test fun ambiguousShortSymbolsCannotChooseTheFirstCompany() {
        val a = issue("Example Engineering", "MUFG")
        val b = issue("Example Electronics", "KFintech")
        assertNull(resolveAllotmentIssue(listOf(a, b), "COLLISION"))
        assertEquals(b, resolveAllotmentIssue(listOf(a, b), "COLLISION", b.companyName))
        assertNotEquals(allotmentIssueKey(a.symbol, a.companyName), allotmentIssueKey(b.symbol, b.companyName))
        assertNull(resolveAllotmentIssue(listOf(a, b), "COLLISION", "Unrelated Company"))
    }

    @Test fun corporateNameVariantsResolveWithoutChangingTheStoredSymbol() {
        val a = issue("Example Engineering Limited", "MUFG")
        assertEquals(a, resolveAllotmentIssue(listOf(a), "COLLISION", "Example Engineering Ltd"))
        assertEquals(a, resolveAllotmentIssue(listOf(a), "COLLISION"))
    }
}
