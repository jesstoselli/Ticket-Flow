package com.jesstoselli.ticketflow.payment.cielo

import kotlinx.serialization.Serializable

data class CieloCredentials(
    val clientId: String,
    val accessToken: String,
) {
    val missingKeys: Set<String>
        get() = buildSet {
            if (clientId.isBlank()) add("CIELO_CLIENT_ID")
            if (accessToken.isBlank()) add("CIELO_ACCESS_TOKEN")
        }
}

@Serializable
data class CieloPaymentRequest(
    val accessToken: String,
    val clientID: String,
    val email: String,
    val installments: Int = 1,
    val reference: String,
    val items: List<CieloItem>,
    val value: Long,
)

@Serializable
data class CieloItem(
    val name: String,
    val quantity: Int,
    val sku: String,
    val unitOfMeasure: String = "unidade",
    val unitPrice: Long,
)
