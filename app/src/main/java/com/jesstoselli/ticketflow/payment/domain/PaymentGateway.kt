package com.jesstoselli.ticketflow.payment.domain

data class PaymentLaunchRequest(
    val reference: String,
    val eventName: String,
    val quantity: Int,
    val unitPriceInCents: Long,
    val totalInCents: Long,
)

interface PaymentGateway {
    suspend fun launch(request: PaymentLaunchRequest): PaymentLaunchResult
}

sealed interface PaymentLaunchResult {
    data object Launched : PaymentLaunchResult
    data object HandlerUnavailable : PaymentLaunchResult
    data class ConfigurationError(val missingKeys: Set<String>) : PaymentLaunchResult
}
