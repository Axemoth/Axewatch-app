package com.aistudio.axewatch.trader.data.model

import com.aistudio.axewatch.trader.data.analysis.TechnicalAnalysisEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TechnicalOutlookLoadingTest {
    private val history = (1..60).map { day ->
        val close = 100.0 + day * 0.5
        CandleBar("day $day", close - 0.5, close + 1, close - 1, close)
    }

    @Test fun oneMonthChartCannotMasqueradeAsFiftyDayPrediction() {
        val short = TechnicalAnalysisEngine.computeOutlook("TEST", history.takeLast(22))
        assertEquals("INSUFFICIENT DATA", short.signal)
        assertEquals(0.0, short.target1, 0.0)
        val full = TechnicalAnalysisEngine.computeOutlook("TEST", history)
        assertNotEquals("INSUFFICIENT DATA", full.signal)
    }
}
