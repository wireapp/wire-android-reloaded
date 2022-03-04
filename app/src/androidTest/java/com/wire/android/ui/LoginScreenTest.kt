package com.wire.android.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.wire.android.ui.authentication.login.LoginScreen
import com.wire.android.ui.authentication.welcome.WelcomeScreen
import com.wire.android.ui.authentication.welcome.WelcomeViewModel
import com.wire.android.ui.theme.WireTheme
import com.wire.android.utils.WorkManagerTestRule
import com.wire.android.utils.getViewModel
import com.wire.kalium.logic.configuration.ServerConfig
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
    ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class
)
@HiltAndroidTest
class LoginScreenTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    // Second, as we are using a WorkManager
    // In an instrumented test we need to ensure this gets initialized before launching any Compose/Activity Rule
    @get:Rule(order = 1)
    var workManagerTestRule = WorkManagerTestRule()

    // Third, we create the compose rule using an AndroidComposeRule, as we are depending on instrumented environment ie: Hilt, WorkManager
    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<WireActivity>()

    @Test
    fun iSeeLoginScreen() {
        hiltRule.inject()

        // Start the app
        composeTestRule.setContent {
            WireTheme {
                LoginScreen(serverConfig = ServerConfig.DEFAULT)
            }
        }

        composeTestRule.onNode(hasTestTag("emailField"), useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNode(hasTestTag("emailField")).performTextClearance()
        composeTestRule.onNode(hasTestTag("emailField")).performTextInput("mustafa+1@wire.com")

        composeTestRule.onNode(hasTestTag("passwordField"), useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNode(hasTestTag("passwordField")).performTextClearance()
        composeTestRule.onNode(hasTestTag("passwordField")).performTextInput("123456")

        composeTestRule.onNodeWithText("Login").assertHasClickAction()
        composeTestRule.onNodeWithText("Login").performClick()
    }

    @Test
    fun iSeeForgotPasswordScreen() {
        hiltRule.inject()

        // Start the app
        composeTestRule.setContent {
            WireTheme {
                LoginScreen(serverConfig = ServerConfig.DEFAULT)
            }
        }

        composeTestRule.onNodeWithText("Forgot password?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Forgot password?").performClick()
//        composeTestRule.onNodeWithText("Reset password", ignoreCase = true).assertIsDisplayed()
    }
}
