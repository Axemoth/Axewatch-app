package com.aistudio.axewatch.trader.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GmpBoardOrderTest {
    private fun row(name: String, status: String, premium: Double) = GmpItem(
        name, name, 100.0, premium, premium, 100.0 + premium,
        status, 1, "", "SME"
    )

    @Test fun `open issues show highest measured gmp first ahead of older stages`() {
        val rows = listOf(
            row("Closed high", "Closed", 200.0),
            row("Open low", "Open", 5.0),
            row("Upcoming", "Upcoming", 80.0),
            row("Open high", "Open", 30.0)
        )
        assertEquals(
            listOf("Open high", "Open low", "Upcoming", "Closed high"),
            sortGmpBoard(rows).map { it.companyName }
        )
    }
}
