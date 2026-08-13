package com.jesstoselli.ticketflow.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_attempts",
    foreignKeys = [ForeignKey(
        entity = PurchaseEntity::class,
        parentColumns = ["id"],
        childColumns = ["purchaseId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("purchaseId"), Index(value = ["reference"], unique = true)],
)
data class PaymentAttemptEntity(
    @PrimaryKey val id: String,
    val purchaseId: String,
    val reference: String,
    val status: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
