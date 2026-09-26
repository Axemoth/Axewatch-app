package com.aistudio.axewatch.trader.data.remote

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MaashitlaStatusFlowTest {
    private fun check(
        searchCode: Int,
        searchBody: String,
        companyName: String = "Robokidz Eduventures",
        declared: Boolean = true
    ): Pair<AllotmentQueryResult, Int> {
        val searches = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/public-issue/companies") { exchange ->
            val bytes = """[{"company_id":"id-1","company_name":"ROBOKIDZ EDUVENTURES LIMITED"}]""".toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.createContext("/api/public-issue/search") { exchange ->
            searches.incrementAndGet()
            val bytes = searchBody.toByteArray()
            exchange.sendResponseHeaders(searchCode, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val service = IpoAllotmentService(maashitlaApiBase = "http://127.0.0.1:${server.address.port}/api")
            val result = runBlocking {
                service.queryAllotment(
                    pan = "ABCDE1234F", ipoSymbol = "ROBOKIDZ", ipoCompanyName = companyName,
                    ipoStatus = "Closed", allotmentDeclared = declared, registrarHint = "Maashitla"
                )
            }
            return result to searches.get()
        } finally {
            server.stop(0)
        }
    }

    @Test fun `official result shows allotted shares`() {
        val (result, searches) = check(200, """{"name":"TEST USER","shares_applied":1200,"shares_alloted":600}""")
        assertEquals(1, searches)
        assertEquals("ALLOTTED", result.status)
        assertEquals(600, result.sharesAllotted)
        assertEquals("TEST***", result.applicantName)
    }

    @Test fun `zero shares means applied but not allotted`() {
        val (result, _) = check(200, """{"shares_applied":1200,"shares_alloted":0}""")
        assertEquals("NOT_ALLOTTED", result.status)
        assertEquals(1200, result.sharesApplied)
    }

    @Test fun `not found means not applied only after declaration`() {
        assertEquals("NOT_APPLIED", check(404, """{"detail":"No records found."}""").first.status)
        assertEquals("RESULTS_NOT_OUT", check(404, """{"detail":"No records found."}""", declared = false).first.status)
    }

    @Test fun `gateway trouble never becomes not applied`() {
        val (result, searches) = check(503, """{"detail":"Unavailable"}""")
        assertEquals(1, searches)
        assertEquals("LOOKUP_FAILED", result.status)
    }

    @Test fun `unmatched issuer is never searched under another company`() {
        val (result, searches) = check(200, "{}", companyName = "Different Metals")
        assertEquals(0, searches)
        assertEquals("UNCOVERED", result.status)
    }
}
