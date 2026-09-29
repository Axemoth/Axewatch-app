package com.aistudio.axewatch.trader.data

import com.aistudio.axewatch.trader.data.local.entity.*
import com.aistudio.axewatch.trader.data.model.*
import com.aistudio.axewatch.trader.data.remote.IpoAllotmentService
import com.aistudio.axewatch.trader.data.remote.AllotmentTransportException
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppliedSharesTest {
    private val service = IpoAllotmentService()
    private fun row(id: String, applied: String = "150", allotted: String = "0", company: String = "Acme Industries") =
        """{"Company":"$company","Appln_No":"$id","App_Shares":$applied,"All_Shares":$allotted}"""
    private fun result(source: String = "LEGACY", applied: Int = 150, status: String = "ALLOTTED") =
        AllotmentRecordEntity(7, "AB*****F", "ACME", "Acme Industries", applied, 50, status, "KFintech", appliedSharesSource = source)

    @Test fun countsRetainMissingVersusExplicitZero() {
        for (value in listOf(null, "", "N/A", "null", "-1", "1.5", "2147483648"))
            assertNull(IpoAllotmentService.reportedShareCount(value))
        assertEquals(0, IpoAllotmentService.reportedShareCount("0"))
        assertEquals(123456, IpoAllotmentService.reportedShareCount("1,23,456"))
        assertEquals(150, IpoAllotmentService.reportedShareCount(150))
        assertEquals(150, IpoAllotmentService.reportedShareCount("150.0"))
        assertEquals("12,34,567", formatShareQuantity(1234567))
    }

    @Test fun kfinSumsDistinctApplicationsOnlyForRequestedCompany() {
        val a = row("one", "\"1,200\"", "50")
        val parsed = service.parseKfinJson("[$a,$a,${row("two", "300")},${row("other", "9000", "900", "Unrelated Company")}]", "Acme Industries")
        assertEquals(1500, parsed.sharesApplied)
        assertEquals(50, parsed.sharesAllotted)
        assertTrue(parsed.appliedSharesComplete)
        assertEquals("ALLOTTED", parsed.status)
    }

    @Test fun missingAppliedDoesNotChangeOutcomeOrClaimPartialTotal() {
        val parsed = service.parseKfinJson("[${row("one")},${row("two", "null")}]", "Acme Industries")
        assertEquals("NOT_ALLOTTED", parsed.status)
        assertFalse(parsed.appliedSharesComplete)
        assertEquals(0, parsed.sharesApplied)
        val allotted = service.parseKfinJson("[${row("one", "null", "50")}]", "Acme Industries")
        assertEquals("ALLOTTED", allotted.status)
        assertFalse(allotted.appliedSharesComplete)
    }

    @Test fun conflictingDuplicatesAreFailedNotNegativeOutcomes() {
        assertThrows(AllotmentTransportException::class.java) {
            service.parseKfinJson("[${row("one")},${row("one", "300")}]", "Acme Industries")
        }
        assertThrows(AllotmentTransportException::class.java) {
            service.parseKfinJson("[${row("one", allotted = "null")}]", "Acme Industries")
        }
        assertEquals("NOT_APPLIED", service.parseKfinJson("[]", "Acme Industries").status)
    }

    @Test fun mufgTotalsDeduplicateAndExcludeOtherCompanies() {
        val a = "<Table><SHARES>1,200</SHARES><ALLOT>50</ALLOT><PEMNDG>one</PEMNDG></Table>"
        val b = "<Table><SHARES>300</SHARES><ALLOT>0</ALLOT><PEMNDG>two</PEMNDG></Table>"
        val other = "<Table><COMPANY>Other Industries</COMPANY><SHARES>9000</SHARES><ALLOT>900</ALLOT></Table>"
        val parsed = service.parseMufgSearchXml("<NewDataSet>$a$a$b$other</NewDataSet>", "Acme Industries")
        assertEquals(1500, parsed.sharesApplied)
        assertEquals(50, parsed.sharesAllotted)
        assertTrue(parsed.appliedSharesComplete)
        val missing = service.parseMufgSearchXml("<NewDataSet><Table><ALLOT>0</ALLOT><PEMNDG>one</PEMNDG></Table></NewDataSet>", "Acme Industries")
        assertEquals("NOT_ALLOTTED", missing.status)
        assertFalse(missing.appliedSharesComplete)
    }

    @Test fun maashitlaMissingIsNotZeroAndKeepsVerdict() {
        val parsed = service.parseMaashitlaJson("""{"shares_alloted":0}""", "Acme Industries")
        assertFalse(parsed.appliedSharesComplete)
        assertEquals("NOT_ALLOTTED", parsed.status)
        assertEquals(1200, service.parseMaashitlaJson("""{"shares_alloted":50,"shares_applied":"1,200"}""", "Acme Industries").sharesApplied)
    }

    @Test fun maashitlaMultipleApplicationsAreDistinctAndComplete() {
        val one = """{"application_no":"one","shares_alloted":50,"shares_applied":"1,200"}"""
        val two = """{"application_no":"two","shares_alloted":0,"shares_applied":300}"""
        val other = """{"company_name":"Unrelated Company","shares_alloted":900,"shares_applied":9000}"""
        val result = service.parseMaashitlaJson("[$one,$one,$two,$other]", "Acme Industries")
        assertEquals(1500, result.sharesApplied)
        assertEquals(50, result.sharesAllotted)
        assertTrue(result.appliedSharesComplete)
    }

    @Test fun refreshPreservesFinalResultsAndKnownCountsOnFailuresOrConflicts() {
        val old = result()
        for (status in listOf("LOOKUP_FAILED", "RESULTS_NOT_OUT", "NOT_APPLIED", "NOT_ALLOTTED", "MANUAL_CHECK_REQUIRED"))
            assertEquals(old, mergeAllotmentDetails(old, result("UNKNOWN", 0, status)))
        val refreshed = mergeAllotmentDetails(old, result("REGISTRAR", 1200).copy(id = 0))
        assertEquals(7L, refreshed.id)
        assertEquals(1200, refreshed.sharesApplied)
        assertEquals("REGISTRAR", refreshed.appliedSharesSource)
        assertEquals(150, mergeAllotmentDetails(old, result("UNKNOWN", 0)).sharesApplied)
        assertEquals(old, mergeAllotmentDetails(old, result("REGISTRAR").copy(ipoSymbol = "OTHER")))
    }

    @Test fun maskedVaultMatchMustBeUnique() {
        val first = PanVaultEntity("ABCDE1234F", "Test")
        val second = PanVaultEntity("ABXYZ9876F", "Other test")
        assertNull(uniqueSavedPan(result(), emptyList()))
        assertEquals(first, uniqueSavedPan(result(), listOf(first)))
        assertNull(uniqueSavedPan(result(), listOf(first, second)))
    }

    @Test fun manualAppliedSharesAreIndependentAndLegacyIsUnverified() {
        assertTrue(validManualQuantities("ALLOTTED", 50, null))
        assertTrue(validManualQuantities("ALLOTTED", 50, 150))
        assertFalse(validManualQuantities("ALLOTTED", 50, 20))
        assertFalse(validManualQuantities("NOT_APPLIED", 0, 50))
        assertEquals("Not reported", result("UNKNOWN", 0).appliedSharesLabel)
        assertEquals("Saved count unverified", result().appliedSourceLabel)
        assertEquals("150", result().appliedSharesLabel)
        assertEquals("0", result("USER", 0).appliedSharesLabel)
    }
}
