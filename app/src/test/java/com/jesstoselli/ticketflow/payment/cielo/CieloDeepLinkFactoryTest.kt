package com.jesstoselli.ticketflow.payment.cielo

import com.jesstoselli.ticketflow.payment.domain.PaymentLaunchRequest
import java.net.URI
import java.net.URLDecoder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

@OptIn(ExperimentalEncodingApi::class)
class CieloDeepLinkFactoryTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val factory = CieloDeepLinkFactory(json)

    @Test
    fun requestUsesIntegerCentsStableReferenceAndCallback() {
        val uri = factory.create(
            request = PaymentLaunchRequest(
                reference = "attempt-123",
                eventName = "Festival Aurora",
                quantity = 2,
                unitPriceInCents = 12_500,
                totalInCents = 25_000,
            ),
            credentials = CieloCredentials(clientId = "client", accessToken = "token"),
        )

        val parsedUri = URI(uri)
        val query = parseQuery(parsedUri.rawQuery)
        assertEquals("lio", parsedUri.scheme)
        assertEquals("payment", parsedUri.host)
        assertEquals(CieloDeepLinkFactory.CALLBACK_URL, query["urlCallback"])
        val encoded = requireNotNull(query["request"])
        val decoded = Base64.decode(encoded).decodeToString()
        val rawPayload = json.parseToJsonElement(decoded).jsonObject
        assertEquals(1, rawPayload.getValue("installments").jsonPrimitive.int)
        assertEquals(
            "unidade",
            rawPayload.getValue("items").jsonArray.single().jsonObject
                .getValue("unitOfMeasure").jsonPrimitive.content,
        )
        val payload = json.decodeFromString<CieloPaymentRequest>(decoded)
        assertEquals("attempt-123", payload.reference)
        assertEquals(25_000, payload.value)
        assertEquals(1, payload.installments)
        assertEquals("ticketflow@sample.local", payload.email)
        assertEquals(2, payload.items.single().quantity)
        assertEquals(12_500, payload.items.single().unitPrice)
        assertEquals("unidade", payload.items.single().unitOfMeasure)
    }

    @Test
    fun base64RequestHasNoLineBreaks() {
        val uri = factory.create(
            request = PaymentLaunchRequest("ref", "Evento", 1, 1_000, 1_000),
            credentials = CieloCredentials("client", "token"),
        )

        val encoded = requireNotNull(parseQuery(URI(uri).rawQuery)["request"])
        assertEquals(false, encoded.contains('\n'))
    }

    @Test
    fun inconsistentTotalIsRejectedBeforeOpeningCielo() {
        assertThrows(IllegalArgumentException::class.java) {
            factory.create(
                request = PaymentLaunchRequest("ref", "Evento", 2, 1_000, 1_000),
                credentials = CieloCredentials("client", "token"),
            )
        }
    }

    private fun parseQuery(rawQuery: String): Map<String, String> = rawQuery
        .split('&')
        .associate { part ->
            val (name, value) = part.split('=', limit = 2)
            URLDecoder.decode(name, Charsets.UTF_8.name()) to
                URLDecoder.decode(value, Charsets.UTF_8.name())
        }
}
