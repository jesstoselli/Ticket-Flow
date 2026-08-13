package com.jesstoselli.ticketflow.payment.cielo

import com.jesstoselli.ticketflow.model.PaymentResult
import java.net.URLEncoder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalEncodingApi::class)
class CieloCallbackParserTest {
    private val parser = CieloCallbackParser(Json { ignoreUnknownKeys = true })

    @Test
    fun approvedOrderIsNormalizedFromFirstPayment() {
        val uri = callbackUri(
            """{"reference":"attempt-123","status":"ENTERED","payments":[{"id":"payment-1","authCode":"140126"}]}""",
        )

        val parsed = parser.parse(uri) as CallbackParseResult.Valid

        assertEquals("attempt-123", parsed.reference)
        assertEquals(PaymentResult.Approved("payment-1", "140126"), parsed.result)
    }

    @Test
    fun cancelledCallbackIsNormalized() {
        val uri = callbackUri("""{"code":1,"reason":"CANCELADO PELO USUÁRIO"}""")

        val parsed = parser.parse(uri) as CallbackParseResult.Valid

        assertEquals(PaymentResult.Cancelled("CANCELADO PELO USUÁRIO"), parsed.result)
    }

    @Test
    fun genericErrorIsNormalized() {
        val uri = callbackUri("""{"code":2,"reason":"FALHA DE INTEGRAÇÃO"}""")

        val parsed = parser.parse(uri) as CallbackParseResult.Valid

        assertEquals(PaymentResult.Failed("2", "FALHA DE INTEGRAÇÃO"), parsed.result)
    }

    @Test
    fun callbackWithRawLineBreaksFromEmulatorIsNormalized() {
        val encoded = Base64.encode("""{"code":2,"reason":"Parâmetros inválidos"}""".toByteArray())
        val wrapped = encoded.chunked(20).joinToString("\n") + "\n"

        val parsed = parser.parse("ticketflow://payment-result?response=$wrapped") as CallbackParseResult.Valid

        assertEquals(PaymentResult.Failed("2", "Parâmetros inválidos"), parsed.result)
    }

    @Test
    fun wrongCallbackOriginIsRejected() {
        val parsed = parser.parse("other://payment-result?response=abc")

        assertTrue(parsed is CallbackParseResult.Invalid)
    }

    @Test
    fun malformedBase64IsRejected() {
        val parsed = parser.parse("ticketflow://payment-result?response=%25%25%25")

        assertTrue(parsed is CallbackParseResult.Invalid)
    }

    private fun callbackUri(payload: String): String =
        "ticketflow://payment-result?response=" +
            URLEncoder.encode(Base64.encode(payload.toByteArray()), Charsets.UTF_8.name())
}
