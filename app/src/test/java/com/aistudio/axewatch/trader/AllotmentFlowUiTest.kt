package com.aistudio.axewatch.trader

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.aistudio.axewatch.trader.data.local.entity.PanVaultEntity
import com.aistudio.axewatch.trader.data.model.IpoIssue
import com.aistudio.axewatch.trader.ui.screens.AllotmentScreen
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
@Config(qualifiers = "w320dp-h800dp-mdpi", sdk = [35])
class AllotmentFlowUiTest {
    @get:Rule val compose = createComposeRule()
    private val issue = IpoIssue("TEST", "Example Engineering and Manufacturing", "SME", "Closed",
        "28-Sep-2026", "30-Sep-2026", "100", 100.0, 1200, 20.0, "Maashitla")
    private val vault = listOf(PanVaultEntity("ABCDE1234F", "Test applicant one"),
        PanVaultEntity("BCDEF2345G", "Test applicant two"))

    @Test fun pickerKeepsCompaniesWithTheSameShortSymbolSeparate() {
        val other = issue.copy(companyName = "Example Electronics", registrar = "KFintech")
        var checkedName = ""
        compose.setContent {
            AxewatchTheme {
                AllotmentScreen(listOf(issue, other), vault, emptyList(), onCheckAllotment = { _, _, _ -> },
                    onCheckSelectedAllotment = { _, chosen, _ -> checkedName = chosen.companyName },
                    onSavePan = { _, _, _ -> }, onDeletePan = {})
            }
        }
        compose.onNodeWithText(issue.companyName).performClick()
        compose.onNodeWithText(other.companyName).performClick()
        compose.onNodeWithText("Test applicant one (Self)").performClick()
        compose.onNodeWithTag("check_allotment_button").performClick()
        org.junit.Assert.assertEquals(other.companyName, checkedName)
    }

    @Test fun checkingFamilyDisablesDuplicateActionsAndShowsProgress() {
        compose.setContent {
            AxewatchTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) {
                    AllotmentScreen(listOf(issue), vault, emptyList(), checkBusy = true,
                        checkProgress = "Checked 1 of 2 saved PANs", onCheckAllotment = { _, _, _ -> },
                        onSavePan = { _, _, _ -> }, onDeletePan = {})
                }
            }
        }
        compose.onNodeWithTag("check_allotment_button").assertIsNotEnabled()
        compose.onNodeWithText("Check all 2 PANs").assertIsNotEnabled()
        compose.onRoot().captureRoboImage("build/reports/allotment-family-progress.png")
    }

    @Test fun applicantIsExplicitAndToolsAreCollapsed() {
        compose.setContent {
            AxewatchTheme {
                AllotmentScreen(listOf(issue), vault, emptyList(), onCheckAllotment = { _, _, _ -> },
                    onSavePan = { _, _, _ -> }, onDeletePan = {})
            }
        }
        compose.onNodeWithTag("check_allotment_button").assertIsNotEnabled()
        compose.onNodeWithText("Test applicant one (Self)").performClick()
        compose.onNodeWithTag("check_allotment_button").assertIsEnabled()
        compose.onNodeWithText("PAN VAULT (FAMILY ACCOUNTS)").assertDoesNotExist()
        compose.onRoot().captureRoboImage("build/reports/allotment-ready.png")
    }
}
