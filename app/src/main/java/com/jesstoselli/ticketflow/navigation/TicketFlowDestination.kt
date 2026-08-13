package com.jesstoselli.ticketflow.navigation

sealed class TicketFlowDestination(val route: String) {
    data object Events : TicketFlowDestination("events")
    data object Tickets : TicketFlowDestination("tickets")
    data object Checkout : TicketFlowDestination("checkout/{eventId}") {
        const val ARG_EVENT_ID = "eventId"
        fun create(eventId: String) = "checkout/$eventId"
    }

    data object PurchaseResult : TicketFlowDestination("purchase/{purchaseId}") {
        const val ARG_PURCHASE_ID = "purchaseId"
        fun create(purchaseId: String) = "purchase/$purchaseId"
    }

    data object Ticket : TicketFlowDestination("ticket/{purchaseId}") {
        const val ARG_PURCHASE_ID = "purchaseId"
        fun create(purchaseId: String) = "ticket/$purchaseId"
    }
}
