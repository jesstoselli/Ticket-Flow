package com.jesstoselli.ticketflow.checkout.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.jesstoselli.ticketflow.checkout.domain.StartPaymentOutcome
import com.jesstoselli.ticketflow.checkout.domain.StartPaymentUseCase
import com.jesstoselli.ticketflow.events.domain.EventRepository
import com.jesstoselli.ticketflow.model.Event
import com.jesstoselli.ticketflow.model.Purchase
import com.jesstoselli.ticketflow.model.PurchaseStatus
import com.jesstoselli.ticketflow.purchase.domain.PurchaseRepository
import com.jesstoselli.ticketflow.purchase.domain.PurchaseSelection
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private val event = Event(
        id = "aurora",
        name = "Festival Aurora",
        description = "Música ao vivo",
        dateTime = "12 SET · 20:00",
        location = "São Paulo",
        unitPriceInCents = 10_000,
        availableTickets = 5,
    )

    @Test fun contentExposesEventWithDefaultQuantityAndTotal() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(CheckoutUiState.Loading, awaitItem())
            val content = awaitItem() as CheckoutUiState.Content
            assertEquals(event, content.event)
            assertEquals(1, content.quantity)
            assertEquals(10_000, content.totalInCents)
            assertEquals(5, content.maxQuantity)
            assertEquals(false, content.isSubmitting)
        }
    }

    @Test fun quantityStepperRespectsBounds() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitItem() // Loading
            assertEquals(1, (awaitItem() as CheckoutUiState.Content).quantity) // initial

            viewModel.decreaseQuantity() // already at min: no change, no emission
            repeat(10) { viewModel.increaseQuantity() } // clamp at maxQuantity (5)
            advanceUntilIdle()
            val clamped = expectMostRecentItem() as CheckoutUiState.Content
            assertEquals(5, clamped.quantity)
            assertEquals(50_000, clamped.totalInCents)

            viewModel.decreaseQuantity()
            assertEquals(4, (awaitItem() as CheckoutUiState.Content).quantity)
        }
    }

    @Test fun payCreatesDraftThenStartsPaymentAndNavigatesToResult() = runTest(dispatcher) {
        val repository = mockk<PurchaseRepository>()
        val useCase = mockk<StartPaymentUseCase>()
        coEvery { repository.createDraft(any()) } returns draft("purchase-1")
        coEvery { useCase("purchase-1") } returns StartPaymentOutcome.Launched("purchase-1")
        val viewModel = viewModel(repository = repository, startPayment = useCase)

        viewModel.events.test {
            viewModel.pay()
            advanceUntilIdle()
            assertEquals(CheckoutEvent.NavigateToResult("purchase-1"), awaitItem())
        }
        coVerify(exactly = 1) { repository.createDraft(any()) }
        coVerify(exactly = 1) { useCase("purchase-1") }
    }

    @Test fun secondTapWhileSubmittingIsIgnored() = runTest(dispatcher) {
        val repository = mockk<PurchaseRepository>()
        val useCase = mockk<StartPaymentUseCase>()
        coEvery { repository.createDraft(any()) } returns draft("purchase-1")
        coEvery { useCase("purchase-1") } returns StartPaymentOutcome.Launched("purchase-1")
        val viewModel = viewModel(repository = repository, startPayment = useCase)

        viewModel.pay()
        viewModel.pay() // ignored: first submission still in flight
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.createDraft(any()) }
        coVerify(exactly = 1) { useCase(any()) }
    }

    @Test fun unknownEventShowsUnavailableState() = runTest(dispatcher) {
        val viewModel = viewModel(events = emptyList(), eventId = "missing")

        viewModel.uiState.test {
            assertEquals(CheckoutUiState.Loading, awaitItem())
            assertEquals(CheckoutUiState.EventUnavailable, awaitItem())
        }
    }

    private fun viewModel(
        events: List<Event> = listOf(event),
        eventId: String = "aurora",
        repository: PurchaseRepository = mockk(relaxed = true),
        startPayment: StartPaymentUseCase = mockk(relaxed = true),
    ) = CheckoutViewModel(
        savedStateHandle = SavedStateHandle(mapOf("eventId" to eventId)),
        eventRepository = FakeEventRepository(events),
        purchaseRepository = repository,
        startPayment = startPayment,
    )

    private fun draft(id: String) = Purchase(
        id = id,
        eventId = event.id,
        eventName = event.name,
        eventDateTime = event.dateTime,
        eventLocation = event.location,
        quantity = 1,
        unitPriceInCents = event.unitPriceInCents,
        status = PurchaseStatus.DRAFT,
        currentAttemptReference = null,
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 1,
    )
}

private class FakeEventRepository(private val events: List<Event>) : EventRepository {
    override fun observeEvents(): Flow<List<Event>> = flowOf(events)
    override fun findById(id: String): Event? = events.find { it.id == id }
}
