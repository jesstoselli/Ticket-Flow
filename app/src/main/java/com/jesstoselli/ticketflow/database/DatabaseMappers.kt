package com.jesstoselli.ticketflow.database

import com.jesstoselli.ticketflow.model.Purchase
import com.jesstoselli.ticketflow.model.PurchaseStatus
import com.jesstoselli.ticketflow.model.Ticket

fun PurchaseEntity.toDomain() = Purchase(
    id = id,
    eventId = eventId,
    eventName = eventName,
    eventDateTime = eventDateTime,
    eventLocation = eventLocation,
    quantity = quantity,
    unitPriceInCents = unitPriceInCents,
    status = PurchaseStatus.valueOf(status),
    currentAttemptReference = currentAttemptReference,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    transactionId = transactionId,
    authorizationCode = authorizationCode,
    resultReason = resultReason,
)

fun TicketEntity.toDomain() = Ticket(
    id = id,
    purchaseId = purchaseId,
    eventId = eventId,
    qrPayload = qrPayload,
    issuedAtEpochMillis = issuedAtEpochMillis,
)
