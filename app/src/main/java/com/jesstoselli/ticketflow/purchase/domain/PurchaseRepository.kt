package com.jesstoselli.ticketflow.purchase.domain

import com.jesstoselli.ticketflow.model.PaymentResult
import com.jesstoselli.ticketflow.model.Purchase
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
}
