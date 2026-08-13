package com.jesstoselli.ticketflow.events.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jesstoselli.ticketflow.designsystem.TicketFlowTheme
import com.jesstoselli.ticketflow.model.Event
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EventListScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun clickingEventOpensCheckout() {
        var openedEventId: String? = null
        composeRule.setContent {
            TicketFlowTheme(darkTheme = false) {
                EventListScreen(
                    uiState = EventListUiState.Content(listOf(event())),
                    onEventClick = { openedEventId = it },
                )
            }
        }

        composeRule.onNodeWithText("Festival Aurora").performClick()

        assertEquals("event-aurora", openedEventId)
    }

    private fun event() = Event(
        id = "event-aurora",
        name = "Festival Aurora",
        description = "Música ao vivo",
        dateTime = "12 SET · 20:00",
        location = "São Paulo",
        unitPriceInCents = 10_000,
        availableTickets = 20,
    )
}
