package com.jesstoselli.ticketflow.events.ui

import app.cash.turbine.test
import com.jesstoselli.ticketflow.events.domain.EventRepository
import com.jesstoselli.ticketflow.model.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
class EventListViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun eventsAreExposedAsContent() = runTest(dispatcher) {
        val events = listOf(event())
        val viewModel = EventListViewModel(FakeEventRepository(events))

        viewModel.uiState.test {
            assertEquals(EventListUiState.Loading, awaitItem())
            assertEquals(EventListUiState.Content(events), awaitItem())
        }
    }

    private fun event() = Event(
        id = "aurora",
        name = "Festival Aurora",
        description = "Música ao vivo",
        dateTime = "12 SET · 20:00",
        location = "São Paulo",
        unitPriceInCents = 10_000,
        availableTickets = 20,
    )
}

private class FakeEventRepository(private val events: List<Event>) : EventRepository {
    override fun observeEvents(): Flow<List<Event>> = flowOf(events)
    override fun findById(id: String): Event? = events.find { it.id == id }
}
