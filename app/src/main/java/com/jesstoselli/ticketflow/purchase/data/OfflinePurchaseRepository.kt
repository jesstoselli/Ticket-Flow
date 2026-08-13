package com.jesstoselli.ticketflow.purchase.data

import androidx.room.withTransaction
import com.jesstoselli.ticketflow.database.PaymentAttemptEntity
import com.jesstoselli.ticketflow.database.PurchaseDao
import com.jesstoselli.ticketflow.database.PurchaseEntity
import com.jesstoselli.ticketflow.database.TicketEntity
import com.jesstoselli.ticketflow.database.TicketFlowDatabase
import com.jesstoselli.ticketflow.database.toDomain
import com.jesstoselli.ticketflow.model.PaymentResult
import com.jesstoselli.ticketflow.model.Purchase
import com.jesstoselli.ticketflow.model.PurchaseStatus
import com.jesstoselli.ticketflow.purchase.domain.ApplyResult
import com.jesstoselli.ticketflow.purchase.domain.PurchaseRepository
import com.jesstoselli.ticketflow.purchase.domain.PurchaseSelection
import com.jesstoselli.ticketflow.purchase.domain.StartAttemptResult
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun interface IdGenerator { fun create(): String }
fun interface TimeProvider { fun now(): Long }

class OfflinePurchaseRepository(
    private val database: TicketFlowDatabase,
    private val dao: PurchaseDao = database.purchaseDao(),
    private val idGenerator: IdGenerator = IdGenerator { UUID.randomUUID().toString() },
    private val timeProvider: TimeProvider = TimeProvider(System::currentTimeMillis),
) : PurchaseRepository {
    override suspend fun createDraft(selection: PurchaseSelection): Purchase {
        require(selection.quantity > 0)
        require(selection.unitPriceInCents >= 0)
        val now = timeProvider.now()
        val entity = PurchaseEntity(
            id = idGenerator.create(),
            eventId = selection.eventId,
            eventName = selection.eventName,
            eventDateTime = selection.eventDateTime,
            eventLocation = selection.eventLocation,
            quantity = selection.quantity,
            unitPriceInCents = selection.unitPriceInCents,
            status = PurchaseStatus.DRAFT.name,
            currentAttemptReference = null,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            transactionId = null,
            authorizationCode = null,
            resultReason = null,
        )
        dao.insertPurchase(entity)
        return entity.toDomain()
    }

    override suspend fun startAttempt(purchaseId: String): StartAttemptResult = database.withTransaction {
        val now = timeProvider.now()
        val attemptId = idGenerator.create()
        val reference = "tf-$attemptId"
        if (dao.markInProgressIfPayable(purchaseId, reference, now) == 0) {
            return@withTransaction StartAttemptResult.Rejected(dao.getPurchase(purchaseId)?.toDomain())
        }
        dao.insertAttempt(
            PaymentAttemptEntity(
                id = attemptId,
                purchaseId = purchaseId,
                reference = reference,
                status = PurchaseStatus.PAYMENT_IN_PROGRESS.name,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            ),
        )
        StartAttemptResult.Started(requireNotNull(dao.getPurchase(purchaseId)).toDomain(), reference)
    }

    override suspend fun applyPaymentResult(reference: String, result: PaymentResult): ApplyResult =
        database.withTransaction {
            val attempt = dao.getAttempt(reference) ?: return@withTransaction ApplyResult.AttemptNotFound
            val purchase = dao.getPurchase(attempt.purchaseId)
                ?: return@withTransaction ApplyResult.AttemptNotFound
            val target = result.status()
            if (purchase.status != PurchaseStatus.PAYMENT_IN_PROGRESS.name ||
                purchase.currentAttemptReference != reference
            ) {
                return@withTransaction if (purchase.status == target.name) {
                    ApplyResult.AlreadyApplied
                } else {
                    ApplyResult.ConflictIgnored
                }
            }

            val now = timeProvider.now()
            val approved = result as? PaymentResult.Approved
            val reason = result.reason()
            if (dao.applyResultIfActive(
                    purchaseId = purchase.id,
                    reference = reference,
                    status = target.name,
                    transactionId = approved?.transactionId,
                    authorizationCode = approved?.authorizationCode,
                    reason = reason,
                    now = now,
                ) == 0
            ) return@withTransaction ApplyResult.ConflictIgnored

            dao.updateAttempt(reference, target.name, now)
            if (target == PurchaseStatus.APPROVED) {
                val ticketId = "ticket-${purchase.id}"
                dao.insertTicket(
                    TicketEntity(
                        id = ticketId,
                        purchaseId = purchase.id,
                        eventId = purchase.eventId,
                        qrPayload = "ticketflow:v1:$ticketId:${purchase.id}:${purchase.eventId}",
                        issuedAtEpochMillis = now,
                    ),
                )
            }
            ApplyResult.Applied
        }

    override fun observePurchase(id: String): Flow<Purchase?> =
        dao.observePurchase(id).map { it?.toDomain() }

    override fun observePurchases(): Flow<List<Purchase>> =
        dao.observePurchases().map { purchases -> purchases.map(PurchaseEntity::toDomain) }
}

private fun PaymentResult.status(): PurchaseStatus = when (this) {
    is PaymentResult.Approved -> PurchaseStatus.APPROVED
    is PaymentResult.Denied -> PurchaseStatus.DENIED
    is PaymentResult.Cancelled -> PurchaseStatus.CANCELLED
    is PaymentResult.Failed -> PurchaseStatus.FAILED
}

private fun PaymentResult.reason(): String? = when (this) {
    is PaymentResult.Approved -> null
    is PaymentResult.Denied -> reason
    is PaymentResult.Cancelled -> reason
    is PaymentResult.Failed -> reason
}
