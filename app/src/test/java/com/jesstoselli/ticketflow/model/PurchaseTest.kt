package com.jesstoselli.ticketflow.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseTest {
    @Test
    fun totalUsesIntegerCents() {
        val purchase = purchase(unitPriceInCents = 12_345, quantity = 3)

        assertEquals(37_035, purchase.totalInCents)
    }

    @Test
    fun payableStatesAllowAnExplicitPaymentAttempt() {
        val payable = listOf(
            PurchaseStatus.DRAFT,
            PurchaseStatus.DENIED,
            PurchaseStatus.CANCELLED,
            PurchaseStatus.FAILED,
        )

        payable.forEach { status -> assertTrue(status.name, purchase(status = status).canStartPayment) }
    }

    @Test
    fun unsafeStatesBlockAnotherPaymentAttempt() {
        val blocked = listOf(
            PurchaseStatus.PAYMENT_IN_PROGRESS,
            PurchaseStatus.APPROVED,
            PurchaseStatus.PENDING,
        )

        blocked.forEach { status -> assertFalse(status.name, purchase(status = status).canStartPayment) }
    }

    @Test
    fun quantityMustBePositive() {
        assertThrows(IllegalArgumentException::class.java) { purchase(quantity = 0) }
    }

    @Test
    fun unitPriceCannotBeNegative() {
        assertThrows(IllegalArgumentException::class.java) { purchase(unitPriceInCents = -1) }
    }

    private fun purchase(
        unitPriceInCents: Long = 10_00,
        quantity: Int = 1,
        status: PurchaseStatus = PurchaseStatus.DRAFT,
    ) = Purchase(
        id = "purchase-1",
        eventId = "event-1",
        eventName = "Festival Aurora",
        eventDateTime = "2026-09-20T20:00:00-03:00",
        eventLocation = "Arena Central",
        quantity = quantity,
        unitPriceInCents = unitPriceInCents,
        status = status,
        currentAttemptReference = null,
        createdAtEpochMillis = 1_000,
        updatedAtEpochMillis = 1_000,
    )
}
