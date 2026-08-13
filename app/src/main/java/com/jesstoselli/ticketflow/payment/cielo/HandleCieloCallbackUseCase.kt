package com.jesstoselli.ticketflow.payment.cielo

import com.jesstoselli.ticketflow.model.PaymentResult
import com.jesstoselli.ticketflow.purchase.domain.ApplyResult
import com.jesstoselli.ticketflow.purchase.domain.PurchaseRepository
import javax.inject.Inject

sealed interface HandleCallbackOutcome {
    /** Callback válido aplicado (ou já aplicado) ao repositório de forma idempotente. */
    data class Handled(
        val purchaseId: String?,
        val reference: String,
        val result: PaymentResult,
        val applyResult: ApplyResult,
    ) : HandleCallbackOutcome

    /** Callback ausente, malformado ou de origem inválida. Nunca aprova uma compra. */
    data class Invalid(val reason: String) : HandleCallbackOutcome

    /** Resultado sem referência e sem tentativa ativa para correlacionar. Estado preservado. */
    data object NoActiveAttempt : HandleCallbackOutcome
}

/**
 * Normaliza e aplica o retorno da Cielo. A idempotência é garantida pelo repositório:
 * um callback repetido resulta em [ApplyResult.AlreadyApplied] e não gera segundo ingresso.
 * Callbacks de cancelamento/erro não trazem referência e são correlacionados com a
 * tentativa `PAYMENT_IN_PROGRESS` ativa.
 */
class HandleCieloCallbackUseCase @Inject constructor(
    private val parser: CieloCallbackParser,
    private val repository: PurchaseRepository,
) {
    suspend operator fun invoke(callbackUrl: String?): HandleCallbackOutcome {
        if (callbackUrl == null) return HandleCallbackOutcome.Invalid("Callback sem URI")
        return when (val parsed = parser.parse(callbackUrl)) {
            is CallbackParseResult.Invalid -> HandleCallbackOutcome.Invalid(parsed.reason)
            is CallbackParseResult.Valid -> apply(parsed)
        }
    }

    private suspend fun apply(parsed: CallbackParseResult.Valid): HandleCallbackOutcome {
        val reference = parsed.reference
            ?: repository.activePaymentReference()
            ?: return HandleCallbackOutcome.NoActiveAttempt
        val applyResult = repository.applyPaymentResult(reference, parsed.result)
        return HandleCallbackOutcome.Handled(
            purchaseId = repository.findPurchaseIdByReference(reference),
            reference = reference,
            result = parsed.result,
            applyResult = applyResult,
        )
    }
}
