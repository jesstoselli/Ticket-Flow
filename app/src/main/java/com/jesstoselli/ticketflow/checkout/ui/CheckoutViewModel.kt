package com.jesstoselli.ticketflow.checkout.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesstoselli.ticketflow.checkout.domain.StartPaymentOutcome
import com.jesstoselli.ticketflow.checkout.domain.StartPaymentUseCase
import com.jesstoselli.ticketflow.events.domain.EventRepository
import com.jesstoselli.ticketflow.model.Event
import com.jesstoselli.ticketflow.purchase.domain.PurchaseRepository
import com.jesstoselli.ticketflow.purchase.domain.PurchaseSelection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CheckoutUiState {
    data object Loading : CheckoutUiState
    data object EventUnavailable : CheckoutUiState
    data class Content(
        val event: Event,
        val quantity: Int,
        val totalInCents: Long,
        val maxQuantity: Int,
        val isSubmitting: Boolean,
    ) : CheckoutUiState
}

sealed interface CheckoutEvent {
    data class NavigateToResult(val purchaseId: String) : CheckoutEvent
}

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    eventRepository: EventRepository,
    private val purchaseRepository: PurchaseRepository,
    private val startPayment: StartPaymentUseCase,
) : ViewModel() {
    private val eventId: String = checkNotNull(savedStateHandle["eventId"]) {
        "Rota de checkout exige o argumento eventId"
    }
    private val event: Event? = eventRepository.findById(eventId)
    private val maxQuantity: Int =
        event?.let { minOf(it.availableTickets, MAX_TICKETS_PER_PURCHASE) } ?: 0

    private val quantity = MutableStateFlow(if (maxQuantity >= 1) 1 else 0)
    private val isSubmitting = MutableStateFlow(false)

    /** Rascunho reutilizado entre toques na mesma sessão de checkout, para não duplicar compras. */
    private var draftId: String? = null

    val uiState: StateFlow<CheckoutUiState> =
        combine(quantity, isSubmitting) { qty, submitting ->
            if (event == null || maxQuantity < 1) {
                CheckoutUiState.EventUnavailable
            } else {
                CheckoutUiState.Content(
                    event = event,
                    quantity = qty,
                    totalInCents = event.unitPriceInCents * qty,
                    maxQuantity = maxQuantity,
                    isSubmitting = submitting,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CheckoutUiState.Loading)

    private val _events = Channel<CheckoutEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun increaseQuantity() = quantity.update { (it + 1).coerceAtMost(maxQuantity) }

    fun decreaseQuantity() = quantity.update { (it - 1).coerceAtLeast(1) }

    fun pay() {
        val currentEvent = event ?: return
        if (isSubmitting.value) return // guarda de UI contra duplo toque
        isSubmitting.value = true
        viewModelScope.launch {
            val purchaseId = draftId
                ?: purchaseRepository.createDraft(selectionFor(currentEvent)).id.also { draftId = it }
            val outcome = startPayment(purchaseId)
            _events.send(CheckoutEvent.NavigateToResult(purchaseId))
            // Mantém o botão travado enquanto a Cielo abre; libera para retry nos demais desfechos.
            if (outcome !is StartPaymentOutcome.Launched) isSubmitting.value = false
        }
    }

    private fun selectionFor(event: Event) = PurchaseSelection(
        eventId = event.id,
        eventName = event.name,
        eventDateTime = event.dateTime,
        eventLocation = event.location,
        quantity = quantity.value,
        unitPriceInCents = event.unitPriceInCents,
    )

    private companion object {
        const val MAX_TICKETS_PER_PURCHASE = 10
    }
}
