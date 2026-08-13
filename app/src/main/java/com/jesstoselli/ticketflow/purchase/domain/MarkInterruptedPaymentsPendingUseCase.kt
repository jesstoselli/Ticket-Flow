package com.jesstoselli.ticketflow.purchase.domain

import javax.inject.Inject

/**
 * Recuperação conservadora executada no início do app: tentativas que ficaram em
 * `PAYMENT_IN_PROGRESS` de um processo anterior (sem callback terminal) viram `PENDING`.
 * O app nunca assume aprovação, falha, nem reenvia cobrança automaticamente.
 */
class MarkInterruptedPaymentsPendingUseCase @Inject constructor(
    private val repository: PurchaseRepository,
) {
    suspend operator fun invoke(processStartedAtEpochMillis: Long): Int =
        repository.markInterruptedPaymentsPending(processStartedAtEpochMillis)
}
