package com.jesstoselli.ticketflow.navigation

sealed class TicketFlowDestination(val route: String) {
    data object Events : TicketFlowDestination("events")
    data object Tickets : TicketFlowDestination("tickets")
    data object Checkout : TicketFlowDestination("checkout/{eventId}") {
        fun create(eventId: String) = "checkout/$eventId"
    }
}
