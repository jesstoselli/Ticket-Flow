package com.jesstoselli.ticketflow.ticket.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.jesstoselli.ticketflow.model.Purchase
import com.jesstoselli.ticketflow.model.PurchaseStatus
import com.jesstoselli.ticketflow.model.Ticket
import com.jesstoselli.ticketflow.purchase.domain.PurchaseRepository
import com.jesstoselli.ticketflow.ticket.domain.QrCodeEncoder
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
class TicketViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun approvedPurchaseWithTicketExposesContent() = runTest(dispatcher) {
        val viewModel = viewModel(purchase(PurchaseStatus.APPROVED), ticket())

        viewModel.uiState.test {
            awaitItem() // Loading
            val content = awaitItem() as TicketUiState.Content
            assertEquals("ticket-1", content.ticketId)
            assertEquals("Festival Aurora", content.eventName)
            assertEquals("ticketflow:v1:ticket-1:purchase-1:aurora", content.qrPayload)
        }
    }

    @Test fun nonApprovedPurchaseIsUnavailable() = runTest(dispatcher) {
        val viewModel = viewModel(purchase(PurchaseStatus.DENIED), ticket = null)

        viewModel.uiState.test {
            awaitItem() // Loading
            assertEquals(TicketUiState.Unavailable, awaitItem())
        }
    }

    @Test fun approvedPurchaseWithoutTicketIsUnavailable() = runTest(dispatcher) {
        val viewModel = viewModel(purchase(PurchaseStatus.APPROVED), ticket = null)

        viewModel.uiState.test {
            awaitItem() // Loading
            assertEquals(TicketUiState.Unavailable, awaitItem())
        }
    }

    @Test fun missingPurchaseIsUnavailable() = runTest(dispatcher) {
        val viewModel = viewModel(purchase = null, ticket = null)

        viewModel.uiState.test {
            awaitItem() // Loading
            assertEquals(TicketUiState.Unavailable, awaitItem())
        }
    }

    private fun viewModel(purchase: Purchase?, ticket: Ticket?): TicketViewModel {
        val repository = mockk<PurchaseRepository>()
        every { repository.observePurchase("purchase-1") } returns flowOf(purchase)
        every { repository.observeTicket("purchase-1") } returns flowOf(ticket)
        return TicketViewModel(
            savedStateHandle = SavedStateHandle(mapOf("purchaseId" to "purchase-1")),
            purchaseRepository = repository,
            qrCodeEncoder = mockk<QrCodeEncoder>(relaxed = true),
        )
    }

    private fun ticket() = Ticket(
        id = "ticket-1",
        purchaseId = "purchase-1",
        eventId = "aurora",
        qrPayload = "ticketflow:v1:ticket-1:purchase-1:aurora",
        issuedAtEpochMillis = 1,
    )

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
    )
}
