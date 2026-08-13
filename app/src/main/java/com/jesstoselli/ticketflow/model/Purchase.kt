package com.jesstoselli.ticketflow.model

data class Purchase(
    val id: String,
    val eventId: String,
    val eventName: String,
    val eventDateTime: String,
    val eventLocation: String,
    val quantity: Int,
    val unitPriceInCents: Long,
    val status: PurchaseStatus,
    val currentAttemptReference: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val transactionId: String? = null,
    val authorizationCode: String? = null,
    val resultReason: String? = null,
) {
    init {
        require(quantity > 0)
        require(unitPriceInCents >= 0)
    }

    val totalInCents: Long
        get() = Math.multiplyExact(unitPriceInCents, quantity.toLong())

    val canStartPayment: Boolean
        get() = status in PAYABLE_STATUSES

    private companion object {
        val PAYABLE_STATUSES = setOf(
            PurchaseStatus.DRAFT,
            PurchaseStatus.DENIED,
            PurchaseStatus.CANCELLED,
            PurchaseStatus.FAILED,
        )
    }
}
