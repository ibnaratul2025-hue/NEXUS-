package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.nexus.core.model.ModelManagerState
import com.example.nexus.data.repository.SystemMetrics
import com.example.nexus.ui.components.NexusStatusRibbon
import com.example.ui.theme.MyApplicationTheme
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
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        NexusStatusRibbon(
          managerState = ModelManagerState(),
          systemMetrics = SystemMetrics(
            totalRamMb = 8192,
            availRamMb = 4096,
            usedRamMb = 4096,
            ramUsagePercent = 0.5f,
            isLowMemory = false,
            availableStorageMb = 64000,
            sandboxReady = true,
            cpuArch = "aarch64"
          )
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
