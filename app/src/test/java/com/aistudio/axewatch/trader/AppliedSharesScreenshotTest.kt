package com.aistudio.axewatch.trader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import com.aistudio.axewatch.trader.ui.theme.AxeDarkBg
import com.aistudio.axewatch.trader.ui.theme.AxeTextPrimary
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.aistudio.axewatch.trader.data.local.entity.AllotmentRecordEntity
import com.aistudio.axewatch.trader.ui.screens.AllotmentScreen
import com.aistudio.axewatch.trader.ui.screens.AllotmentShareCounts
import com.aistudio.axewatch.trader.ui.screens.IpoScreen
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
class AppliedSharesScreenshotTest {
    @get:Rule val compose = createComposeRule()
    @Test fun narrowLargeFontCountStates() {
        compose.setContent {
            AxewatchTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) {
                    Column(Modifier.fillMaxSize().background(AxeDarkBg).padding(16.dp)) {
                        for ((source, status) in listOf("REGISTRAR" to "ALLOTTED", "LEGACY" to "NOT_ALLOTTED", "UNKNOWN" to "LOOKUP_FAILED", "UNKNOWN" to "RESULTS_NOT_OUT")) {
                            Text(status, color = AxeTextPrimary)
                            AllotmentShareCounts(AllotmentRecordEntity(maskedPan = "AB*****F", ipoSymbol = "TEST",
                                ipoName = "Test", sharesApplied = 123450, sharesAllotted = if (status == "ALLOTTED") 150 else 0,
                                status = status, registrar = "MUFG", appliedSharesSource = source))
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/reports/applied-counts-large-font.png")
        compose.onNodeWithText("Saved count unverified").assertExists()
        compose.onAllNodesWithText("1,23,450").assertCountEquals(2)
    }
    @Test fun actualResultCardAndMissingVaultPromptAtLargeFont() {
        val record = AllotmentRecordEntity(1, "AB*****F", "TEST", "Shree Maharaja Engineering and Manufacturing Industries",
            1200, 150, "ALLOTTED", "MUFG Intime")
        compose.setContent {
            AxewatchTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) {
                    AllotmentScreen(emptyList(), emptyList(), listOf(record),
                        onCheckAllotment = { _, _, _ -> }, onSavePan = { _, _, _ -> }, onDeletePan = {})
                }
            }
        }
        compose.onNodeWithTag("allotment_screen").performScrollToNode(hasTestTag("allotment_result_1"))
        compose.onNodeWithText("Applied shares").assertExists()
        compose.onNodeWithText("Allotted shares").assertExists()
        compose.onRoot().captureRoboImage("build/reports/allotment-result-large-font.png")
        compose.onNodeWithText("Refresh details").performScrollTo().performClick()
        compose.onNodeWithText("Refresh applied shares").assertExists()
        compose.onRoot().captureRoboImage("build/reports/allotment-refresh-pan.png")
    }

    @Test fun narrowLargeFontLoadingAndEmptyIpoSections() {
        compose.setContent {
            AxewatchTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) {
                    IpoScreen(emptyList(), emptyList(), emptyList(), isLoading = true)
                }
            }
        }
        compose.onRoot().captureRoboImage("build/reports/ipo-loading-large-font.png")
        compose.onNodeWithText("Past Listings").performClick()
        compose.onRoot().captureRoboImage("build/reports/ipo-empty-large-font.png")
        compose.onNodeWithText("No past listings loaded yet").assertExists()
    }
}
