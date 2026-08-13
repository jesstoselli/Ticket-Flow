package com.jesstoselli.ticketflow.payment.cielo

import com.jesstoselli.ticketflow.model.PaymentResult
import java.net.URLDecoder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

sealed interface CallbackParseResult {
    data class Valid(
        val reference: String?,
        val result: PaymentResult,
    ) : CallbackParseResult

    data class Invalid(val reason: String) : CallbackParseResult
}

class CieloCallbackParser(
    private val json: Json,
) {
    fun parse(callbackUrl: String): CallbackParseResult {
        val prefix = "$CALLBACK_SCHEME://$CALLBACK_HOST?"
        if (!callbackUrl.startsWith(prefix)) {
            return CallbackParseResult.Invalid("Origem do callback inválida")
        }
        val rawQuery = callbackUrl.substring(prefix.length)
        if (rawQuery.isBlank()) {
            return CallbackParseResult.Invalid("URI de callback inválida")
        }
        val response = parseQuery(rawQuery)["response"]
            ?: return CallbackParseResult.Invalid("Callback sem resposta")
        val decoded = try {
            decodeBase64(response)
        } catch (_: IllegalArgumentException) {
            return CallbackParseResult.Invalid("Resposta não está em Base64 válido")
        }

        return try {
            parsePayload(decoded)
        } catch (_: SerializationException) {
            CallbackParseResult.Invalid("Resposta não contém JSON válido")
        } catch (_: IllegalArgumentException) {
            CallbackParseResult.Invalid("Resposta possui estrutura inválida")
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeBase64(value: String): String =
        Base64.Mime.decode(value).decodeToString()

    private fun parseQuery(rawQuery: String?): Map<String, String> = rawQuery
        ?.split('&')
        ?.mapNotNull { part ->
            val separator = part.indexOf('=')
            if (separator <= 0) null else {
                URLDecoder.decode(part.substring(0, separator), Charsets.UTF_8.name()) to
                    URLDecoder.decode(part.substring(separator + 1), Charsets.UTF_8.name())
            }
        }
        ?.toMap()
        .orEmpty()

    private fun parsePayload(payload: String): CallbackParseResult.Valid {
        val root = json.parseToJsonElement(payload).jsonObject
        return if (root.containsKey("code")) parseError(root) else parseOrder(payload)
    }

    private fun parseError(root: JsonObject): CallbackParseResult.Valid {
        val error = json.decodeFromJsonElement(CieloErrorResponse.serializer(), root)
        val normalizedReason = error.reason.orEmpty().uppercase()
        val result = when {
            error.code == 1 || "CANCEL" in normalizedReason -> PaymentResult.Cancelled(error.reason)
            "NEGAD" in normalizedReason || "DENIED" in normalizedReason -> PaymentResult.Denied(error.reason)
            else -> PaymentResult.Failed(error.code?.toString(), error.reason)
        }
        return CallbackParseResult.Valid(reference = error.reference, result = result)
    }

    private fun parseOrder(payload: String): CallbackParseResult.Valid {
        val order = json.decodeFromString<CieloOrderResponse>(payload)
        val payment = order.payments.firstOrNull()
            ?: throw IllegalArgumentException("Pedido sem pagamento")
        return CallbackParseResult.Valid(
            reference = order.reference,
            result = PaymentResult.Approved(
                transactionId = payment.id,
                authorizationCode = payment.authCode,
            ),
        )
    }

    companion object {
        const val CALLBACK_SCHEME = "ticketflow"
        const val CALLBACK_HOST = "payment-result"
    }
}

@Serializable
private data class CieloErrorResponse(
    val code: Int? = null,
    val reason: String? = null,
    val reference: String? = null,
)

@Serializable
private data class CieloOrderResponse(
    val reference: String? = null,
    val payments: List<CieloPaymentResponse> = emptyList(),
)

@Serializable
private data class CieloPaymentResponse(
    val id: String? = null,
    val authCode: String? = null,
)
