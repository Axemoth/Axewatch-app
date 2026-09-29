package com.aistudio.axewatch.trader

import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.aistudio.axewatch.trader.ui.AppNavigation
import com.aistudio.axewatch.trader.ui.screens.IpoScreen
import com.aistudio.axewatch.trader.ui.screens.MarketScreen
import com.aistudio.axewatch.trader.ui.theme.AxewatchTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-mdpi", sdk = [35])
class NavigationScreenshotTest {
    @get:Rule val compose = createComposeRule()
    @Test fun marketEntryOpensFullScreenIpoWithBackAndRefresh() {
        compose.setContent {
            AxewatchTheme(darkTheme = true) {
                AppNavigation(false, 0L, {}, 0L,
                    market = { open ->
                        MarketScreen(emptyList(), emptyList(), emptyList(), emptyList(),
                            onStockClick = {}, onOpenIpo = open)
                    },
                    portfolio = { Text("Portfolio") },
                    ipo = { IpoScreen(emptyList(), emptyList(), emptyList()) })
            }
        }
        compose.onNodeWithTag("market_ipo_entry").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/reports/market-ipo-entry.png")
        compose.onNodeWithTag("market_ipo_entry").performClick()
        compose.onNodeWithContentDescription("Refresh IPOs").assertIsDisplayed()
        compose.onNodeWithTag("bottom_nav_market").assertDoesNotExist()
        compose.onRoot().captureRoboImage("build/reports/ipo-full-screen.png")
        compose.onNodeWithTag("ipo_back").performClick()
        compose.onNodeWithTag("bottom_nav_market").assertIsSelected()
    }
}
