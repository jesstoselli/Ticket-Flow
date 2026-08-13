package com.jesstoselli.ticketflow.purchase.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.jesstoselli.ticketflow.checkout.domain.StartPaymentUseCase
import com.jesstoselli.ticketflow.model.Purchase
import com.jesstoselli.ticketflow.model.PurchaseStatus
import com.jesstoselli.ticketflow.purchase.domain.PurchaseRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PurchaseResultViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun deniedPurchaseCanRetry() = runTest(dispatcher) {
        val viewModel = viewModel(purchase(PurchaseStatus.DENIED))

        viewModel.uiState.test {
            awaitItem() // Loading
            val content = awaitItem() as PurchaseResultUiState.Content
            assertEquals(PurchaseStatus.DENIED, content.purchase.status)
            assertEquals(true, content.canRetry)
        }
    }

    @Test fun approvedPurchaseCannotRetry() = runTest(dispatcher) {
        val viewModel = viewModel(purchase(PurchaseStatus.APPROVED))

        viewModel.uiState.test {
            awaitItem() // Loading
            assertEquals(false, (awaitItem() as PurchaseResultUiState.Content).canRetry)
        }
    }

    @Test fun pendingPurchaseCannotRetry() = runTest(dispatcher) {
        val viewModel = viewModel(purchase(PurchaseStatus.PENDING))

        viewModel.uiState.test {
            awaitItem() // Loading
            assertEquals(false, (awaitItem() as PurchaseResultUiState.Content).canRetry)
        }
    }

    @Test fun unknownPurchaseIsNotFound() = runTest(dispatcher) {
        val viewModel = viewModel(purchase = null)

        viewModel.uiState.test {
            awaitItem() // Loading
            assertEquals(PurchaseResultUiState.NotFound, awaitItem())
        }
    }

    private fun viewModel(purchase: Purchase?): PurchaseResultViewModel {
        val repository = mockk<PurchaseRepository>()
        every { repository.observePurchase("purchase-1") } returns flowOf(purchase)
        return PurchaseResultViewModel(
            savedStateHandle = SavedStateHandle(mapOf("purchaseId" to "purchase-1")),
            purchaseRepository = repository,
            startPayment = mockk<StartPaymentUseCase>(relaxed = true),
        )
    }

    private fun purchase(status: PurchaseStatus) = Purchase(
        id = "purchase-1",
        eventId = "aurora",
        eventName = "Festival Aurora",
        eventDateTime = "12 SET · 20:00",
        eventLocation = "São Paulo",
        quantity = 2,
        unitPriceInCents = 10_000,
        status = status,
        currentAttemptReference = "tf-1",
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 1,
        transactionId = if (status == PurchaseStatus.APPROVED) "tx-1" else null,
        authorizationCode = if (status == PurchaseStatus.APPROVED) "auth-1" else null,
    )
}
