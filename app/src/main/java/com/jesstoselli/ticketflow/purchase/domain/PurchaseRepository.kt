package com.jesstoselli.ticketflow.purchase.domain

import com.jesstoselli.ticketflow.model.PaymentResult
import com.jesstoselli.ticketflow.model.Purchase
import com.jesstoselli.ticketflow.model.Ticket
import kotlinx.coroutines.flow.Flow

data class PurchaseSelection(
    val eventId: String,
    val eventName: String,
    val eventDateTime: String,
    val eventLocation: String,
    val quantity: Int,
    val unitPriceInCents: Long,
)

sealed interface StartAttemptResult {
    data class Started(val purchase: Purchase, val reference: String) : StartAttemptResult
    data class Rejected(val purchase: Purchase?) : StartAttemptResult
}

sealed interface ApplyResult {
    data object Applied : ApplyResult
    data object AlreadyApplied : ApplyResult
    data object ConflictIgnored : ApplyResult
    data object AttemptNotFound : ApplyResult
}

interface PurchaseRepository {
    suspend fun createDraft(selection: PurchaseSelection): Purchase
    suspend fun startAttempt(purchaseId: String): StartAttemptResult
    suspend fun applyPaymentResult(reference: String, result: PaymentResult): ApplyResult
    fun observePurchase(id: String): Flow<Purchase?>
    fun observePurchases(): Flow<List<Purchase>>

    /** Ingresso vinculado a uma compra aprovada, se já emitido. */
    fun observeTicket(purchaseId: String): Flow<Ticket?>

    /** Referência da tentativa `PAYMENT_IN_PROGRESS` mais recente, para correlacionar callbacks sem referência. */
    suspend fun activePaymentReference(): String?

    /** Resolve a compra dona de uma referência de tentativa. */
    suspend fun findPurchaseIdByReference(reference: String): String?

    /**
     * Promove para `PENDING`, de forma conservadora, tentativas `PAYMENT_IN_PROGRESS`
     * iniciadas antes do início do processo atual (sem callback terminal). Retorna quantas foram afetadas.
     */
    suspend fun markInterruptedPaymentsPending(processStartedAtEpochMillis: Long): Int
}
