package com.jesstoselli.ticketflow.payment.cielo

import com.jesstoselli.ticketflow.model.PaymentResult
import com.jesstoselli.ticketflow.purchase.domain.ApplyResult
import com.jesstoselli.ticketflow.purchase.domain.PurchaseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HandleCieloCallbackUseCaseTest {
    private val parser = mockk<CieloCallbackParser>()
    private val repository = mockk<PurchaseRepository>()
    private val useCase = HandleCieloCallbackUseCase(parser, repository)

    @Test fun nullUriIsInvalidAndNeverTouchesRepository() = runTest {
        val outcome = useCase(null)

        assertTrue(outcome is HandleCallbackOutcome.Invalid)
        coVerify(exactly = 0) { repository.applyPaymentResult(any(), any()) }
    }

    @Test fun malformedCallbackNeverApprovesPurchase() = runTest {
        every { parser.parse("bad") } returns CallbackParseResult.Invalid("Resposta possui estrutura inválida")

        val outcome = useCase("bad")

        assertTrue(outcome is HandleCallbackOutcome.Invalid)
        coVerify(exactly = 0) { repository.applyPaymentResult(any(), any()) }
    }

    @Test fun approvedCallbackWithReferenceIsApplied() = runTest {
        val approved = PaymentResult.Approved("tx-1", "auth-1")
        every { parser.parse("ok") } returns CallbackParseResult.Valid("tf-1", approved)
        coEvery { repository.applyPaymentResult("tf-1", approved) } returns ApplyResult.Applied
        coEvery { repository.findPurchaseIdByReference("tf-1") } returns "purchase-1"

        val outcome = useCase("ok") as HandleCallbackOutcome.Handled

        assertEquals("tf-1", outcome.reference)
        assertEquals("purchase-1", outcome.purchaseId)
        assertEquals(ApplyResult.Applied, outcome.applyResult)
        coVerify(exactly = 1) { repository.applyPaymentResult("tf-1", approved) }
    }

    @Test fun cancelledCallbackWithoutReferenceUsesActiveAttempt() = runTest {
        val cancelled = PaymentResult.Cancelled("CANCELADO PELO USUÁRIO")
        every { parser.parse("cancel") } returns CallbackParseResult.Valid(null, cancelled)
        coEvery { repository.activePaymentReference() } returns "tf-1"
        coEvery { repository.applyPaymentResult("tf-1", cancelled) } returns ApplyResult.Applied
        coEvery { repository.findPurchaseIdByReference("tf-1") } returns "purchase-1"

        val outcome = useCase("cancel") as HandleCallbackOutcome.Handled

        assertEquals("tf-1", outcome.reference)
        coVerify(exactly = 1) { repository.applyPaymentResult("tf-1", cancelled) }
    }

    @Test fun duplicateApprovedCallbackDelegatesIdempotencyToRepository() = runTest {
        val approved = PaymentResult.Approved("tx-1", "auth-1")
        every { parser.parse("ok") } returns CallbackParseResult.Valid("tf-1", approved)
        coEvery { repository.findPurchaseIdByReference("tf-1") } returns "purchase-1"
        coEvery { repository.applyPaymentResult("tf-1", approved) } returnsMany
            listOf(ApplyResult.Applied, ApplyResult.AlreadyApplied)

        val first = useCase("ok") as HandleCallbackOutcome.Handled
        val second = useCase("ok") as HandleCallbackOutcome.Handled

        assertEquals(ApplyResult.Applied, first.applyResult)
        assertEquals(ApplyResult.AlreadyApplied, second.applyResult)
    }

    @Test fun missingReferenceWithoutActiveAttemptDoesNotApply() = runTest {
        every { parser.parse("cancel") } returns
            CallbackParseResult.Valid(null, PaymentResult.Cancelled("X"))
        coEvery { repository.activePaymentReference() } returns null

        val outcome = useCase("cancel")

        assertEquals(HandleCallbackOutcome.NoActiveAttempt, outcome)
        coVerify(exactly = 0) { repository.applyPaymentResult(any(), any()) }
    }
}
