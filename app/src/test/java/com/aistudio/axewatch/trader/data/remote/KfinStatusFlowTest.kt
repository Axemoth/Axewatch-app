package com.aistudio.axewatch.trader.data.remote

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class KfinStatusFlowTest {
    private fun check(status: Int, payload: String, declared: Boolean = true): Pair<AllotmentQueryResult, Int> {
        val calls = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/query") { exchange ->
            calls.incrementAndGet()
            val bytes = payload.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val service = IpoAllotmentService("http://127.0.0.1:${server.address.port}/query")
            return runBlocking {
                service.queryAllotment(
                    pan = "ABCDE1234F", ipoSymbol = "SSRETAIL", ipoCompanyName = "SS Retail",
                    ipoStatus = "Closed", allotmentDeclared = declared, registrarHint = "KFintech"
                )
            } to calls.get()
        } finally {
            server.stop(0)
        }
    }

    @Test fun `allotted application includes share count`() {
        val (result, calls) = check(200, """[{"Company":"SS Retail Limited","App_Shares":"1200","All_Shares":"1200"}]""")
        assertEquals(1, calls)
        assertEquals("ALLOTTED", result.status)
        assertEquals(1200, result.sharesAllotted)
    }

    @Test fun `applied with zero shares is not allotted`() {
        val (result, _) = check(200, """[{"Company":"SS Retail Limited","App_Shares":"1200","All_Shares":"0"}]""")
        assertEquals("NOT_ALLOTTED", result.status)
        assertEquals(1200, result.sharesApplied)
        assertEquals(0, result.sharesAllotted)
    }

    @Test fun `no application response is not applied`() {
        val (result, _) = check(400, "no record")
        assertEquals("NOT_APPLIED", result.status)
        assertFalse(result.found)
    }

    @Test fun `closed issue without declared result does not claim a loss or no application`() {
        val (zeroShares, _) = check(200, """[{"Company":"SS Retail Limited","App_Shares":"1200","All_Shares":"0"}]""", false)
        assertEquals("RESULTS_NOT_OUT", zeroShares.status)
        val (noRecord, _) = check(400, "no record", false)
        assertEquals("RESULTS_NOT_OUT", noRecord.status)
    }

    @Test fun `positive allotment is shown even before scheduled declaration`() {
        val (result, _) = check(200, """[{"Company":"SS Retail Limited","App_Shares":"1200","All_Shares":"600"}]""", false)
        assertEquals("ALLOTTED", result.status)
        assertEquals(600, result.sharesAllotted)
    }

    @Test fun `rate limit is lookup failed and is not retried`() {
        val (result, calls) = check(429, "rate limited")
        assertEquals("LOOKUP_FAILED", result.status)
        assertEquals(1, calls)
    }

    @Test fun `gateway error never becomes not applied`() {
        val (result, calls) = check(502, "gateway error")
        assertEquals("LOOKUP_FAILED", result.status)
        assertEquals(2, calls)
    }

    @Test fun `malformed success is lookup failed`() {
        val (result, _) = check(200, """{"error":"gateway trouble"}""")
        assertEquals("LOOKUP_FAILED", result.status)
    }
}
