package com.jesstoselli.ticketflow.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {
    @Insert suspend fun insertPurchase(purchase: PurchaseEntity)
    @Insert suspend fun insertAttempt(attempt: PaymentAttemptEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertTicket(ticket: TicketEntity): Long

    @Query("SELECT * FROM purchases WHERE id = :id")
    suspend fun getPurchase(id: String): PurchaseEntity?

    @Query("SELECT * FROM purchases WHERE id = :id")
    fun observePurchase(id: String): Flow<PurchaseEntity?>

    @Query("SELECT * FROM purchases ORDER BY createdAtEpochMillis DESC")
    fun observePurchases(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM payment_attempts WHERE reference = :reference")
    suspend fun getAttempt(reference: String): PaymentAttemptEntity?

    @Query("SELECT COUNT(*) FROM payment_attempts WHERE purchaseId = :purchaseId")
    suspend fun countAttemptsForPurchase(purchaseId: String): Int

    @Query("SELECT COUNT(*) FROM tickets WHERE purchaseId = :purchaseId")
    suspend fun countTicketsForPurchase(purchaseId: String): Int

    @Query("""
        UPDATE purchases
        SET status = 'PAYMENT_IN_PROGRESS', currentAttemptReference = :reference,
            updatedAtEpochMillis = :now
        WHERE id = :purchaseId AND status IN ('DRAFT', 'DENIED', 'CANCELLED', 'FAILED')
    """)
    suspend fun markInProgressIfPayable(purchaseId: String, reference: String, now: Long): Int

    @Query("""
        UPDATE purchases SET status = :status, transactionId = :transactionId,
            authorizationCode = :authorizationCode, resultReason = :reason,
            updatedAtEpochMillis = :now
        WHERE id = :purchaseId AND status = 'PAYMENT_IN_PROGRESS'
            AND currentAttemptReference = :reference
    """)
    suspend fun applyResultIfActive(
        purchaseId: String,
        reference: String,
        status: String,
        transactionId: String?,
        authorizationCode: String?,
        reason: String?,
        now: Long,
    ): Int

    @Query("UPDATE payment_attempts SET status = :status, updatedAtEpochMillis = :now WHERE reference = :reference")
    suspend fun updateAttempt(reference: String, status: String, now: Long)
}
