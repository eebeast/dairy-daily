package com.milksummer.tracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class AppNavigationTest {
    @get:Rule(order = 0) val unlockedDevice = UnlockedDeviceRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun hiltStartupAndAllBottomTabsOpen() {
        // Launching the real activity verifies the Application, ViewModels and
        // Room repository can be constructed through the production Hilt graph.
        if (compose.onAllNodesWithText("Later").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Later").performClick()
        }
        compose.onNodeWithText("MilkSummer").assertIsDisplayed()
        compose.onAllNodesWithText("History").onFirst().performClick()
        compose.onAllNodesWithText("History").onLast().assertIsDisplayed()
        compose.onAllNodesWithText("Summary").onFirst().performClick()
        compose.onNodeWithText("Monthly").assertIsDisplayed()
        compose.onAllNodesWithText("Settings").onFirst().performClick()
        compose.onNodeWithText("Entry reminders").assertIsDisplayed()
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithText("Add Entry").performClick()
        compose.onNodeWithText("Quantity (liters)").assertIsDisplayed()
    }
}
