package com.jesstoselli.ticketflow.purchase.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jesstoselli.ticketflow.model.Purchase
import com.jesstoselli.ticketflow.model.PurchaseStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PurchaseHistoryScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private fun purchase(id: String, status: PurchaseStatus) = Purchase(
        id = id,
        eventId = "aurora",
        eventName = "Festival Aurora",
        eventDateTime = "12 SET · 20:00",
        eventLocation = "São Paulo",
        quantity = 2,
        unitPriceInCents = 10_000,
        status = status,
        currentAttemptReference = "tf-1",
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 1,
    )

    @Test fun approvedPurchaseIsListedAndOpensOnClick() {
        var opened: String? = null
        composeRule.setContent {
            PurchaseHistoryScreen(
                uiState = PurchaseHistoryUiState.Content(listOf(purchase("purchase-1", PurchaseStatus.APPROVED))),
                onOpenPurchase = { opened = it },
            )
        }

        composeRule.onNodeWithText("Aprovada").assertExists()
        composeRule.onNodeWithText("Festival Aurora").performClick()
        assertEquals("purchase-1", opened)
    }

    @Test fun emptyHistoryShowsPlaceholder() {
        composeRule.setContent {
            PurchaseHistoryScreen(
                uiState = PurchaseHistoryUiState.Content(emptyList()),
                onOpenPurchase = {},
            )
        }

        composeRule.onNodeWithText("Nenhuma compra ainda.\nEscolha um evento para começar.").assertExists()
    }
}
