package com.jesstoselli.ticketflow

import com.jesstoselli.ticketflow.model.PaymentResult
import com.jesstoselli.ticketflow.payment.domain.PaymentGateway
import com.jesstoselli.ticketflow.payment.domain.PaymentLaunchRequest
import com.jesstoselli.ticketflow.payment.domain.PaymentLaunchResult
import com.jesstoselli.ticketflow.purchase.domain.PurchaseRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gateway determinístico para as jornadas instrumentadas. Não abre nenhum app externo:
 * captura a referência da tentativa e permite ao teste concluir o resultado, exercitando
 * o mesmo caminho de persistência idempotente do callback real.
 */
@Singleton
class FakePaymentGateway @Inject constructor(
    private val repository: PurchaseRepository,
) : PaymentGateway {

    @Volatile var lastRequest: PaymentLaunchRequest? = null
        private set

    @Volatile var nextLaunchResult: PaymentLaunchResult = PaymentLaunchResult.Launched

    override suspend fun launch(request: PaymentLaunchRequest): PaymentLaunchResult {
        lastRequest = request
        return nextLaunchResult
    }

    val latestReference: String get() = requireNotNull(lastRequest) { "Nenhum pagamento lançado" }.reference

    suspend fun completeApproved(reference: String = latestReference) =
        repository.applyPaymentResult(reference, PaymentResult.Approved("tx-fake", "auth-fake"))

    suspend fun completeCancelled(reference: String = latestReference) =
        repository.applyPaymentResult(reference, PaymentResult.Cancelled("CANCELADO PELO USUÁRIO"))

    suspend fun completeDenied(reference: String = latestReference) =
        repository.applyPaymentResult(reference, PaymentResult.Denied("NEGADO"))
}
