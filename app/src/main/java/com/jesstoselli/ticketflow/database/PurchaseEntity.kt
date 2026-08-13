package com.jesstoselli.ticketflow.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val eventName: String,
    val eventDateTime: String,
    val eventLocation: String,
    val quantity: Int,
    val unitPriceInCents: Long,
    val status: String,
    val currentAttemptReference: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val transactionId: String?,
    val authorizationCode: String?,
    val resultReason: String?,
)
