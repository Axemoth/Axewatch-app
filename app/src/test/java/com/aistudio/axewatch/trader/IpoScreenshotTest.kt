package com.aistudio.axewatch.trader

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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
            status = "Active", issueOpenDate = java.time.LocalDate.now().minusDays(1).toString(), issueCloseDate = java.time.LocalDate.now().plusDays(2).toString(),
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
            54.75, 54.05, 1.4, 0.1, 1.39, "25-Sep-26", "SME",
            registrar = "Maashitla", issueCloseDate = "22 Sep 2026",
            qibSub = 0.7, niiSub = 2.3, shniSub = 2.9,
            bhniSub = 1.9, riiSub = 4.2)
        compose.setContent {
            AxewatchTheme { IpoScreen(listOf(issue, longSme), listOf(gmp), listOf(past)) }
        }
        compose.onRoot().captureRoboImage(filePath = "build/reports/ipo-current.png")
        compose.onAllNodesWithText("2.5x total").assertCountEquals(2)
        compose.onNodeWithText("+₹95").assertExists()
        compose.onNodeWithText("Past Listings").performClick()
        compose.onNodeWithText("Registrar: Maashitla", substring = true).assertExists()
        compose.onNodeWithText("LAST RECORDED SUBSCRIPTION").assertExists()
        compose.onRoot().captureRoboImage(filePath = "build/reports/ipo-past.png")
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp-mdpi", sdk = [35])
    fun pastListingRetainsSubscriptionAtLargeFont() {
        val past = PastIpoItem("ROBOKIDZ", "Robokidz Eduventures", 106.0, 0.0,
            listingGainPercent = 0.0, totalSub = 9.29, category = "SME",
            registrar = "Maashitla", issueCloseDate = "23 Sep 2026",
            qibSub = 0.7, niiSub = 8.6, shniSub = 9.5, bhniSub = 8.1, riiSub = 14.8)
        compose.setContent {
            AxewatchTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) {
                    IpoScreen(emptyList(), emptyList(), listOf(past))
                }
            }
        }
        compose.onNodeWithText("Past Listings").performClick()
        compose.onNodeWithText("Listing data pending", substring = true).assertExists()
        compose.onNodeWithText("Maashitla", substring = true).assertExists()
        compose.onRoot().captureRoboImage(filePath = "build/reports/ipo-past-narrow.png")
    }
}
