package com.jesstoselli.ticketflow.purchase.domain

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkInterruptedPaymentsPendingUseCaseTest {
    private val repository = mockk<PurchaseRepository>()
    private val useCase = MarkInterruptedPaymentsPendingUseCase(repository)

    @Test fun promotesUsingProcessStartAndReturnsAffectedCount() = runTest {
        coEvery { repository.markInterruptedPaymentsPending(1_000L) } returns 2

        val promoted = useCase(processStartedAtEpochMillis = 1_000L)

        assertEquals(2, promoted)
        coVerify(exactly = 1) { repository.markInterruptedPaymentsPending(1_000L) }
    }
}
