package com.aistudio.axewatch.trader.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MarketSourcesTest {
    @Test fun officialIndexCsvKeepsEveryRowAndDoesNotInventQuotes() {
        val csv = "Company Name,Industry,Symbol,Series,ISIN Code\n" +
            "\"Alpha, Industries Ltd\",Metals,ALPHA,EQ,INE000\n" +
            "Beta Bank Ltd,Financial Services,BETA,EQ,INE111\n"
        val rows = IndexMembershipService.parseConstituents(csv)
        assertEquals(2, rows.size)
        assertEquals("Alpha, Industries Ltd", rows[0].name)
        assertEquals(0.0, rows[0].lastPrice, 0.0)
        assertEquals(0.0, rows[0].weightPercent, 0.0)
        assertEquals(emptyList<com.aistudio.axewatch.trader.data.model.IndexConstituent>(),
            IndexMembershipService.parseConstituents("<html>blocked</html>"))
    }

    @Test fun datedPolicyRssRetainsSourceAndLink() {
        val xml = """<rss><channel><item><title>Policy rate decision</title>
            <link>https://rbi.org.in/decision</link>
            <pubDate>Fri, 25 Sep 2026 19:10:00</pubDate></item></channel></rss>"""
        val service = NewsSentimentService()
        val items = service.parseRssItems(xml, 10, "RBI")
        assertEquals(1, items.size)
        assertEquals("RBI", items[0].source)
        assertEquals("https://rbi.org.in/decision", items[0].link)
        assertTrue(items[0].publishedAtMillis > 0)
        assertTrue(service.parseNewsDate("24 Sep, 2026 +0530") > 0)
    }
}
