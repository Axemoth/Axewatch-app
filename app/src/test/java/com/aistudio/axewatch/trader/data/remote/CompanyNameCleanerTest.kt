package com.aistudio.axewatch.trader.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the glued-token name cleaner against REAL tracker cell shapes. A \b
 * boundary does not exist between two letter runs, so "SS RetailIPO" kept its
 * suffix, breaking the registrar-directory join and mis-attributing KFin IPOs.
 */
class CompanyNameCleanerTest {

    private val svc = IpoGmpService()
    private fun clean(raw: String) = svc.cleanCompanyNameForTest(raw)

    @Test
    fun `glued suffixes are removed`() {
        assertEquals("SS Retail", clean("SS RetailIPO"))
        assertEquals("Hero Motors", clean("Hero MotorsOPEN"))
        assertEquals("Jindal Supreme", clean("Jindal SupremeCLOSED"))
        assertEquals("Anand Seamless", clean("Anand SeamlessBSE SME"))
        assertEquals("Phychem Technologies", clean("Phychem TechnologiesBSE SME"))
    }

    @Test
    fun `glued prefixes are removed`() {
        assertEquals("Phychem Technologies", clean("NSE SMEPhychem Technologies"))
        assertEquals("Lumino Industries", clean("BSELumino Industries"))
    }

    @Test
    fun `NSE is a real ipo name and must survive`() {
        // Regression: the old split-on-exchange-token cleaned "NSE" to "" and
        // dropped the biggest mainboard IPO from the list entirely.
        assertEquals("NSE", clean("NSE"))
        assertEquals("NSE", clean("NSE L"))
        assertEquals("NSE", clean("NSE IPO"))
    }

    @Test
    fun `real company names keep their trailing letters`() {
        // Regression: a naive trailing-[UOCLA] strip turned "Tata" into "Tat".
        assertEquals("Tata", clean("Tata"))
        assertEquals("Tata Motors", clean("Tata Motors"))
        assertEquals("Lupin", clean("Lupin"))
        assertEquals("Adroit Industries", clean("Adroit Industries"))
        assertEquals("Panthera", clean("Panthera"))
        assertEquals("Lupin", clean("Lupin O"))
    }

    @Test
    fun `cleaned names join the registrar directory`() {
        // The exact regression behind "SS Retail (KFintech) showed as MUFG":
        // the uncleaned name could not match the directory, so the app probed
        // and stored the wrong registrar.
        val cleaned = clean("SS RetailIPO")
        assertEquals(
            IpoAllotmentService.canonIpoName("SS Retail"),
            IpoAllotmentService.canonIpoName(cleaned)
        )
        assertEquals("ssretail", IpoAllotmentService.canonIpoName(cleaned))
        assertEquals(
            IpoAllotmentService.canonIpoName("Anand Seamless"),
            IpoAllotmentService.canonIpoName(clean("Anand SeamlessBSE SME"))
        )
    }
}
