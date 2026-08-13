package com.jesstoselli.ticketflow.events.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesstoselli.ticketflow.events.domain.EventRepository
import com.jesstoselli.ticketflow.model.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface EventListUiState {
    data object Loading : EventListUiState
    data class Content(val events: List<Event>) : EventListUiState
}

@HiltViewModel
class EventListViewModel @Inject constructor(
    repository: EventRepository,
) : ViewModel() {
    val uiState: StateFlow<EventListUiState> = repository.observeEvents()
        .map<List<Event>, EventListUiState>(EventListUiState::Content)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventListUiState.Loading)
}
