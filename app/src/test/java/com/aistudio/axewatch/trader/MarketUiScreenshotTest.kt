package com.aistudio.axewatch.trader

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.aistudio.axewatch.trader.data.model.IndexConstituent
import com.aistudio.axewatch.trader.data.model.MarketIndex
import com.aistudio.axewatch.trader.data.model.StockQuote
import com.aistudio.axewatch.trader.ui.dialogs.IndexDetailModal
import com.aistudio.axewatch.trader.ui.dialogs.StockDetailModal
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
class MarketUiScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test
    @Config(qualifiers = "w320dp-h800dp-mdpi", sdk = [35])
    fun fundDetailsKeepMissingMetricsReadableAtLargeFont() {
        val fund = com.aistudio.axewatch.trader.data.model.MutualFundScheme(
            "42", "Example Flexi Cap Fund - Direct Plan - Growth", "Example AMC", "Flexi Cap",
            nav = 100.1234, navPrev = null, dayChangePercent = null, navDate = "01-10-2026")
        compose.setContent {
            AxewatchTheme {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                        androidx.compose.ui.platform.LocalDensity.current.density, 1.3f)) {
                    com.aistudio.axewatch.trader.ui.dialogs.MutualFundDetailModal(
                        fund, onDismiss = {}, onAddToPortfolio = { _, _, _, _ -> })
                }
            }
        }
        compose.onNodeWithText("Change unavailable").assertExists()
        compose.onNodeWithText("NAV 01-10-2026").assertExists()
        compose.onNodeWithText("Risk: unknown").assertExists()
        compose.onRoot().captureRoboImage(filePath = "build/reports/fund-details-large-font.png")
    }

    @Test fun indexMembershipShowsUnquotedRowsWithoutInventedPrices() {
        compose.setContent {
            AxewatchTheme {
                IndexDetailModal(
                    index = MarketIndex("NIFTY 50", "NIFTY 50", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
                    constituents = listOf(
                        IndexConstituent("HDFCBANK", "HDFC Bank Ltd", "Banking", 0.0, 0.0, 0.0, 0.0),
                        IndexConstituent("RELIANCE", "Reliance Industries Ltd", "Energy", 0.0, 0.0, 0.0, 0.0)
                    ),
                    onStockClick = {}, onDismiss = {}
                )
            }
        }
        compose.onRoot().captureRoboImage(filePath = "build/reports/index-membership.png")
    }

    @Test fun stockOutlookHasVisibleLoadingState() {
        compose.setContent {
            AxewatchTheme {
                StockDetailModal(
                    stock = StockQuote("HDFCBANK", "HDFC Bank Ltd", 0.0, 0.0, 0.0),
                    candles = emptyList(), outlook = null, outlookLoading = true,
                    onDismiss = {}, onTakeTrade = { _, _ -> }
                )
            }
        }
        compose.onNodeWithText("₹0.00").assertDoesNotExist()
        compose.onNodeWithText("Live quote needed to trade").assertExists()
        compose.onNodeWithTag("take_this_trade_button").assertIsNotEnabled()
        compose.onRoot().captureRoboImage(filePath = "build/reports/stock-outlook-loading.png")
    }
}
