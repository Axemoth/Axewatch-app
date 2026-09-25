package com.aistudio.axewatch.trader

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.aistudio.axewatch.trader.ui.screens.AllotmentScreen
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
class AllotmentScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emptyAllotmentScreenRendersWithoutNetwork() {
        compose.setContent {
            AxewatchTheme {
                AllotmentScreen(
                    ipos = emptyList(), savedPans = emptyList(), records = emptyList(),
                    onCheckAllotment = { _, _, _ -> }, onSavePan = { _, _, _ -> },
                    onDeletePan = {}, alertsEnabled = true
                )
            }
        }
        compose.onRoot().captureRoboImage(filePath = "build/reports/allotment-empty.png")
    }
}
