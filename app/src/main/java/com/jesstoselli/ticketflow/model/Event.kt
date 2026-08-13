package com.jesstoselli.ticketflow.model

data class Event(
    val id: String,
    val name: String,
    val description: String,
    val dateTime: String,
    val location: String,
    val unitPriceInCents: Long,
    val availableTickets: Int,
) {
    init {
        require(unitPriceInCents >= 0)
        require(availableTickets >= 0)
    }
}
