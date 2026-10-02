package com.aistudio.axewatch.trader

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.aistudio.axewatch.trader.data.model.IpoIssue
import com.aistudio.axewatch.trader.ui.AppNavigation
import com.aistudio.axewatch.trader.ui.screens.IpoScreen
import com.aistudio.axewatch.trader.ui.theme.AxewatchTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import android.content.Intent
import com.aistudio.axewatch.trader.data.work.AllotWatcherWorker
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NavigationTest {
    @get:Rule val compose = createComposeRule()
    private val request = mutableLongStateOf(0L)
    private var handled = 0
    private val issues = (1..30).map { index ->
        IpoIssue(symbol = "ISSUE$index", companyName = "Issue Company $index", category = "Mainboard",
            status = "Active", issueOpenDate = java.time.LocalDate.now().minusDays(1).toString(), issueCloseDate = java.time.LocalDate.now().plusDays(2).toString(),
            priceBand = "", issuePrice = 0.0, lotSize = 0, issueSizeCr = 0.0, registrar = "Unknown")
    }
    private fun setup(coldNotification: Boolean = false): StateRestorationTester {
        if (coldNotification) request.longValue = 1L
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            AxewatchTheme {
                AppNavigation(false, 0L, {}, request.longValue,
                    market = { open -> Button(onClick = open) { Text("Open IPO workspace") } },
                    portfolio = { Text("Portfolio content") },
                    ipo = {
                        IpoScreen(issues, emptyList(), emptyList(),
                            allotmentSectionTick = request.longValue,
                            onAllotmentSectionHandled = { request.longValue = 0L; handled++ })
                    })
            }
        }
        return restoration
    }

    @Test fun activityNotificationIntentIsHandledOnlyOnce() {
        val intent = Intent().putExtra(AllotWatcherWorker.EXTRA_OPEN_TAB, AllotWatcherWorker.TAB_ALLOTMENT)
        assertTrue(consumeAllotmentIntent(intent))
        assertFalse(consumeAllotmentIntent(intent))
        assertFalse(consumeAllotmentIntent(null))
        val next = Intent().putExtra(AllotWatcherWorker.EXTRA_OPEN_TAB, AllotWatcherWorker.TAB_ALLOTMENT)
        assertTrue(consumeAllotmentIntent(next))
    }

    @Test fun rootTabsIpoBackSearchAndRestoration() {
        val restore = setup()
        compose.onNodeWithTag("bottom_nav_market").assertIsSelected()
        compose.onNodeWithTag("bottom_nav_portfolio").performClick()
        compose.onNodeWithText("Portfolio content").assertExists()
        compose.onNodeWithTag("bottom_nav_market").performClick()
        compose.onNodeWithText("Open IPO workspace").performClick()
        compose.onNodeWithTag("bottom_nav_market").assertDoesNotExist()
        compose.onNodeWithText("Current").assertExists()
        compose.onNodeWithTag("ipo_search_input").performTextInput("current query")
        compose.onNodeWithText("Past Listings").performClick()
        compose.onNodeWithTag("ipo_search_input").performTextInput("past query")
        compose.onNodeWithText("Allotment").performClick()
        compose.onNodeWithTag("allotment_screen").assertExists()
        compose.onNodeWithText("Past Listings").performClick()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("ipo_search_input").assertTextContains("past query")
        compose.onNodeWithTag("ipo_back").performClick()
        compose.onNodeWithTag("bottom_nav_market").assertIsSelected()
        compose.onNodeWithText("Open IPO workspace").performClick()
        compose.onNodeWithTag("ipo_search_input").assertTextContains("past query")
        compose.onNodeWithText("Current").performClick()
        compose.onNodeWithTag("ipo_search_input").assertTextContains("current query")
    }

    @Test fun listPositionSurvivesSectionSwitchBackAndRotation() {
        val restore = setup()
        compose.onNodeWithText("Open IPO workspace").performClick()
        compose.onNodeWithTag("ipo_list").performScrollToIndex(15)
        val visibleBefore = compose.onAllNodes(hasText("Issue Company", substring = true))
            .fetchSemanticsNodes().first().config[androidx.compose.ui.semantics.SemanticsProperties.Text].first().text
        compose.onNodeWithText("Past Listings").performClick()
        compose.onNodeWithText("Current").performClick()
        compose.onNodeWithText(visibleBefore).assertIsDisplayed()
        compose.onNodeWithTag("ipo_back").performClick()
        compose.onNodeWithText("Open IPO workspace").performClick()
        compose.onNodeWithText(visibleBefore).assertIsDisplayed()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText(visibleBefore).assertIsDisplayed()
    }

    @Test fun coldNotificationConsumedAndWarmNotificationFromPortfolioRoutesOnce() {
        val restore = setup(coldNotification = true)
        compose.onNodeWithTag("allotment_screen").assertExists()
        assertEquals(1, handled)
        compose.onNodeWithText("Current").performClick()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("ipo_search_input").assertExists()
        assertEquals(1, handled)
        compose.onNodeWithTag("ipo_back").performClick()
        compose.onNodeWithTag("bottom_nav_portfolio").performClick()
        compose.runOnIdle { request.longValue = 2L }
        compose.onNodeWithTag("allotment_screen").assertExists()
        assertEquals(2, handled)
        compose.onNodeWithTag("ipo_back").performClick()
        compose.onNodeWithTag("bottom_nav_market").assertIsSelected()
        compose.onNodeWithText("Open IPO workspace").performClick()
        compose.onNodeWithText("Past Listings").performClick()
        compose.onNodeWithTag("ipo_search_input").assertExists()
        assertEquals(2, handled)
    }
}
