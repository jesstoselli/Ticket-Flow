package com.jesstoselli.ticketflow.payment.cielo

import com.jesstoselli.ticketflow.payment.domain.PaymentLaunchRequest
import java.net.URLEncoder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CieloDeepLinkFactory(
    json: Json,
) {
    private val requestJson = Json(from = json) { encodeDefaults = true }

    fun create(
        request: PaymentLaunchRequest,
        credentials: CieloCredentials,
    ): String {
        require(request.quantity > 0)
        require(request.unitPriceInCents >= 0)
        require(request.totalInCents == Math.multiplyExact(request.unitPriceInCents, request.quantity.toLong()))

        val payload = CieloPaymentRequest(
            accessToken = credentials.accessToken,
            clientID = credentials.clientId,
            email = SAMPLE_EMAIL,
            reference = request.reference,
            items = listOf(
                CieloItem(
                    name = request.eventName,
                    quantity = request.quantity,
                    sku = request.reference,
                    unitPrice = request.unitPriceInCents,
                ),
            ),
            value = request.totalInCents,
        )
        val encoded = encodeBase64(requestJson.encodeToString(payload))
        return "lio://payment?request=${encodeQuery(encoded)}&urlCallback=${encodeQuery(CALLBACK_URL)}"
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun encodeBase64(value: String): String = Base64.encode(value.toByteArray(Charsets.UTF_8))

    private fun encodeQuery(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    companion object {
        const val CALLBACK_URL = "ticketflow://payment-result"
        private const val SAMPLE_EMAIL = "ticketflow@sample.local"
    }
}
