package com.jesstoselli.ticketflow.model

sealed interface PaymentResult {
    data class Approved(
        val transactionId: String?,
        val authorizationCode: String?,
    ) : PaymentResult

    data class Denied(val reason: String?) : PaymentResult

    data class Cancelled(val reason: String?) : PaymentResult

    data class Failed(
        val code: String?,
        val reason: String?,
    ) : PaymentResult
}
