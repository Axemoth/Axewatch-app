package com.aistudio.axewatch.trader

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.aistudio.axewatch.trader.data.model.GmpItem
import com.aistudio.axewatch.trader.data.model.IpoIssue
import com.aistudio.axewatch.trader.data.model.PastIpoItem
import com.aistudio.axewatch.trader.ui.screens.IpoScreen
import com.aistudio.axewatch.trader.ui.theme.AxewatchTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class IpoScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun ipoTabsRenderCompactCards() {
        val issue = IpoIssue(
            symbol = "ORIENT", companyName = "Orient Cables", category = "Mainboard",
            status = "Active", issueOpenDate = "25 Sep 2026", issueCloseDate = "29 Sep 2026",
            priceBand = "₹272", issuePrice = 272.0, lotSize = 55,
            issueSizeCr = 120.0, registrar = "MUFG", totalSub = 2.5,
            gmpAmount = 95.0, gmpPercent = 34.9, estListingPrice = 367.0
        )
        val gmp = GmpItem("Orient Cables", "ORIENT", 272.0, 95.0, 34.9, 367.0,
            "Open", 4, "", "Mainboard")
        val longSme = issue.copy(symbol = "SMELONG",
            companyName = "Shree Maharaja Engineering and Manufacturing Industries",
            category = "SME", gmpAmount = 42.0)
        val past = PastIpoItem("AXIOMGAS", "Axiom Gas Engineering", 54.0,
            54.75, 54.05, 1.4, 0.1, 1.39, "25-Sep-26", "SME")
        compose.setContent {
            AxewatchTheme { IpoScreen(listOf(issue, longSme), listOf(gmp), listOf(past)) }
        }
        compose.onRoot().captureRoboImage(filePath = "build/reports/ipo-current.png")
        compose.onAllNodesWithText("2.5x total").assertCountEquals(2)
        compose.onNodeWithText("+₹95").assertExists()
        compose.onNodeWithText("Past Listings").performClick()
        compose.onRoot().captureRoboImage(filePath = "build/reports/ipo-past.png")
    }
}
