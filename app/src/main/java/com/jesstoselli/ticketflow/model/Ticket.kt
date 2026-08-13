package com.jesstoselli.ticketflow.model

data class Ticket(
    val id: String,
    val purchaseId: String,
    val eventId: String,
    val qrPayload: String,
    val issuedAtEpochMillis: Long,
)
