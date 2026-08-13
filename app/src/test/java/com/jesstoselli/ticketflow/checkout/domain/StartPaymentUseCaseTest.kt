package com.jesstoselli.ticketflow.checkout.domain

import com.jesstoselli.ticketflow.model.PaymentResult
import com.jesstoselli.ticketflow.model.Purchase
import com.jesstoselli.ticketflow.model.PurchaseStatus
import com.jesstoselli.ticketflow.payment.domain.PaymentGateway
import com.jesstoselli.ticketflow.payment.domain.PaymentLaunchRequest
import com.jesstoselli.ticketflow.payment.domain.PaymentLaunchResult
import com.jesstoselli.ticketflow.purchase.domain.ApplyResult
import com.jesstoselli.ticketflow.purchase.domain.PurchaseRepository
import com.jesstoselli.ticketflow.purchase.domain.StartAttemptResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartPaymentUseCaseTest {
    private val repository = mockk<PurchaseRepository>()
    private val gateway = mockk<PaymentGateway>()
    private val useCase = StartPaymentUseCase(repository, gateway)

    private val purchase = Purchase(
        id = "purchase-1",
        eventId = "aurora",
        eventName = "Festival Aurora",
        eventDateTime = "12 SET · 20:00",
        eventLocation = "São Paulo",
        quantity = 2,
        unitPriceInCents = 10_000,
        status = PurchaseStatus.PAYMENT_IN_PROGRESS,
        currentAttemptReference = "tf-attempt-1",
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 1,
    )

    @Test fun attemptIsPersistedBeforeGatewayLaunch() = runTest {
        coEvery { repository.startAttempt("purchase-1") } returns
            StartAttemptResult.Started(purchase, "tf-attempt-1")
        coEvery { gateway.launch(any()) } returns PaymentLaunchResult.Launched

        val outcome = useCase("purchase-1")

        assertEquals(StartPaymentOutcome.Launched("purchase-1"), outcome)
        coVerifyOrder {
            repository.startAttempt("purchase-1")
            gateway.launch(any())
        }
    }

    @Test fun launchRequestCarriesTotalInCentsAndAttemptReference() = runTest {
        val request = slot<PaymentLaunchRequest>()
        coEvery { repository.startAttempt("purchase-1") } returns
            StartAttemptResult.Started(purchase, "tf-attempt-1")
        coEvery { gateway.launch(capture(request)) } returns PaymentLaunchResult.Launched

        useCase("purchase-1")

        assertEquals("tf-attempt-1", request.captured.reference)
        assertEquals(2, request.captured.quantity)
        assertEquals(10_000, request.captured.unitPriceInCents)
        assertEquals(20_000, request.captured.totalInCents)
    }

    @Test fun secondTapWhileInProgressDoesNotLaunchAgain() = runTest {
        coEvery { repository.startAttempt("purchase-1") } returns
            StartAttemptResult.Rejected(purchase.copy(status = PurchaseStatus.PAYMENT_IN_PROGRESS))

        val outcome = useCase("purchase-1")

        assertEquals(StartPaymentOutcome.Blocked(PurchaseStatus.PAYMENT_IN_PROGRESS), outcome)
        coVerify(exactly = 0) { gateway.launch(any()) }
    }

    @Test fun handlerUnavailableAfterLockReleasesRetryWithFailed() = runTest {
        val applied = slot<PaymentResult>()
        coEvery { repository.startAttempt("purchase-1") } returns
            StartAttemptResult.Started(purchase, "tf-attempt-1")
        coEvery { gateway.launch(any()) } returns PaymentLaunchResult.HandlerUnavailable
        coEvery { repository.applyPaymentResult("tf-attempt-1", capture(applied)) } returns ApplyResult.Applied

        val outcome = useCase("purchase-1")

        assertEquals(StartPaymentOutcome.HandlerUnavailable("purchase-1"), outcome)
        assertTrue(applied.captured is PaymentResult.Failed)
    }

    @Test fun configurationErrorAfterLockReleasesRetryWithFailed() = runTest {
        val applied = slot<PaymentResult>()
        coEvery { repository.startAttempt("purchase-1") } returns
            StartAttemptResult.Started(purchase, "tf-attempt-1")
        coEvery { gateway.launch(any()) } returns
            PaymentLaunchResult.ConfigurationError(setOf("CIELO_CLIENT_ID"))
        coEvery { repository.applyPaymentResult("tf-attempt-1", capture(applied)) } returns ApplyResult.Applied

        val outcome = useCase("purchase-1")

        assertEquals(
            StartPaymentOutcome.ConfigurationMissing(setOf("CIELO_CLIENT_ID"), "purchase-1"),
            outcome,
        )
        assertTrue(applied.captured is PaymentResult.Failed)
    }
}
