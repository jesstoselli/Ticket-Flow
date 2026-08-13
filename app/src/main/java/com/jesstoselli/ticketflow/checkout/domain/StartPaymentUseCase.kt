package com.jesstoselli.ticketflow.checkout.domain

import com.jesstoselli.ticketflow.model.PaymentResult
import com.jesstoselli.ticketflow.model.Purchase
import com.jesstoselli.ticketflow.model.PurchaseStatus
import com.jesstoselli.ticketflow.payment.domain.PaymentGateway
import com.jesstoselli.ticketflow.payment.domain.PaymentLaunchRequest
import com.jesstoselli.ticketflow.payment.domain.PaymentLaunchResult
import com.jesstoselli.ticketflow.purchase.domain.PurchaseRepository
import com.jesstoselli.ticketflow.purchase.domain.StartAttemptResult
import javax.inject.Inject

/**
 * Resultado da tentativa de iniciar um pagamento a partir do checkout.
 *
 * O caso de uso é o único ponto autorizado a abrir a Cielo, e sempre depois de
 * a tentativa estar persistida como `PAYMENT_IN_PROGRESS`. Todos os desfechos
 * carregam o `purchaseId` (quando existe) para que a UI navegue ao resultado.
 */
sealed interface StartPaymentOutcome {
    /** Deep link disparado; a compra permanece `PAYMENT_IN_PROGRESS` até o callback. */
    data class Launched(val purchaseId: String) : StartPaymentOutcome

    /** O gate rejeitou o início: já existe pagamento em progresso, pendente ou aprovado. */
    data class Blocked(val status: PurchaseStatus?) : StartPaymentOutcome

    /** Nenhum app resolve o deep link da Cielo; a compra volta a ser pagável. */
    data class HandlerUnavailable(val purchaseId: String) : StartPaymentOutcome

    /** Credenciais ausentes; a compra volta a ser pagável para retry explícito. */
    data class ConfigurationMissing(
        val missingKeys: Set<String>,
        val purchaseId: String,
    ) : StartPaymentOutcome
}

class StartPaymentUseCase @Inject constructor(
    private val purchaseRepository: PurchaseRepository,
    private val paymentGateway: PaymentGateway,
) {
    suspend operator fun invoke(purchaseId: String): StartPaymentOutcome =
        when (val start = purchaseRepository.startAttempt(purchaseId)) {
            is StartAttemptResult.Rejected -> StartPaymentOutcome.Blocked(start.purchase?.status)
            is StartAttemptResult.Started -> launch(start.purchase, start.reference)
        }

    private suspend fun launch(purchase: Purchase, reference: String): StartPaymentOutcome {
        val request = PaymentLaunchRequest(
            reference = reference,
            eventName = purchase.eventName,
            quantity = purchase.quantity,
            unitPriceInCents = purchase.unitPriceInCents,
            totalInCents = purchase.totalInCents,
        )
        return when (val result = paymentGateway.launch(request)) {
            PaymentLaunchResult.Launched -> StartPaymentOutcome.Launched(purchase.id)

            PaymentLaunchResult.HandlerUnavailable -> {
                // A tentativa já está travada: registra falha sanitizada para liberar retry.
                purchaseRepository.applyPaymentResult(
                    reference,
                    PaymentResult.Failed(HANDLER_UNAVAILABLE, "Nenhum app compatível para concluir o pagamento."),
                )
                StartPaymentOutcome.HandlerUnavailable(purchase.id)
            }

            is PaymentLaunchResult.ConfigurationError -> {
                purchaseRepository.applyPaymentResult(
                    reference,
                    PaymentResult.Failed(CONFIGURATION_MISSING, "Configuração de pagamento ausente."),
                )
                StartPaymentOutcome.ConfigurationMissing(result.missingKeys, purchase.id)
            }
        }
    }

    private companion object {
        const val HANDLER_UNAVAILABLE = "HANDLER_UNAVAILABLE"
        const val CONFIGURATION_MISSING = "CONFIGURATION_MISSING"
    }
}
