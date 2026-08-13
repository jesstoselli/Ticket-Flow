package com.jesstoselli.ticketflow.model

data class PaymentAttempt(
    val id: String,
    val purchaseId: String,
    val reference: String,
    val status: PurchaseStatus,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
