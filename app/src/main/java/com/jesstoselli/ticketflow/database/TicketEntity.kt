package com.jesstoselli.ticketflow.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tickets",
    foreignKeys = [ForeignKey(
        entity = PurchaseEntity::class,
        parentColumns = ["id"],
        childColumns = ["purchaseId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["purchaseId"], unique = true)],
)
data class TicketEntity(
    @PrimaryKey val id: String,
    val purchaseId: String,
    val eventId: String,
    val qrPayload: String,
    val issuedAtEpochMillis: Long,
)
