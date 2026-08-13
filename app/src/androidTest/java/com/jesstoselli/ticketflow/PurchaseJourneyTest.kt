package com.jesstoselli.ticketflow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class PurchaseJourneyTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var fakeGateway: FakePaymentGateway

    @Before fun setup() = hiltRule.inject()

    @Test fun eventToApprovedQrTicket() {
        composeRule.onNodeWithText("Festival Aurora").performClick()
        composeRule.onNodeWithContentDescription("Aumentar quantidade").performClick()
        composeRule.onNodeWithText("Pagar com Cielo").performClick()

        awaitLaunch()
        composeRule.runOnIdle { runBlocking { fakeGateway.completeApproved() } }

        awaitText("Pagamento aprovado")
        composeRule.onNodeWithText("Ver ingresso").performClick()
        composeRule.onNodeWithContentDescription("QR Code do ingresso").assertIsDisplayed()
    }

    @Test fun deniedThenRetryReachesApproval() {
        composeRule.onNodeWithText("Samba no Céu").performClick()
        composeRule.onNodeWithText("Pagar com Cielo").performClick()

        awaitLaunch()
        composeRule.runOnIdle { runBlocking { fakeGateway.completeDenied() } }

        awaitText("Pagamento negado")
        composeRule.onNodeWithText("Tentar novamente").performClick()

        awaitLaunch()
        composeRule.runOnIdle { runBlocking { fakeGateway.completeApproved() } }
        awaitText("Pagamento aprovado")
    }

    private fun awaitLaunch() {
        val alreadyLaunched = fakeGateway.lastRequest
        composeRule.waitUntil(timeoutMillis = 5_000) { fakeGateway.lastRequest !== alreadyLaunched }
    }

    private fun awaitText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }
}
