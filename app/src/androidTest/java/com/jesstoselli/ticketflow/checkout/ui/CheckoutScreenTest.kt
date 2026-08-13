package com.jesstoselli.ticketflow.checkout.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jesstoselli.ticketflow.model.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CheckoutScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val event = Event(
        id = "aurora",
        name = "Festival Aurora",
        description = "Música ao vivo",
        dateTime = "12 SET · 20:00",
        location = "São Paulo",
        unitPriceInCents = 10_000,
        availableTickets = 5,
    )

    private fun content(quantity: Int = 1, isSubmitting: Boolean = false) = CheckoutUiState.Content(
        event = event,
        quantity = quantity,
        totalInCents = event.unitPriceInCents * quantity,
        maxQuantity = 5,
        isSubmitting = isSubmitting,
    )

    @Test fun payButtonInvokesCallback() {
        var paid = false
        composeRule.setContent {
            CheckoutScreen(content(), onIncrease = {}, onDecrease = {}, onPay = { paid = true }, onBack = {})
        }
        composeRule.onNodeWithText("Pagar com Cielo").assertIsEnabled().performClick()
        assertTrue(paid)
    }

    @Test fun submittingDisablesPayButton() {
        composeRule.setContent {
            CheckoutScreen(content(isSubmitting = true), onIncrease = {}, onDecrease = {}, onPay = {}, onBack = {})
        }
        composeRule.onNodeWithText("Processando…").assertIsNotEnabled()
    }

    @Test fun increaseUsesAccessibleContentDescription() {
        var increments = 0
        composeRule.setContent {
            CheckoutScreen(content(), onIncrease = { increments++ }, onDecrease = {}, onPay = {}, onBack = {})
        }
        composeRule.onNodeWithContentDescription("Aumentar quantidade").performClick()
        assertEquals(1, increments)
    }
}
