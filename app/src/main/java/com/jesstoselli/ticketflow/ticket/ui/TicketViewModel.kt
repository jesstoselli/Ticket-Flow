package com.jesstoselli.ticketflow.ticket.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesstoselli.ticketflow.model.PurchaseStatus
import com.jesstoselli.ticketflow.purchase.domain.PurchaseRepository
import com.jesstoselli.ticketflow.ticket.domain.QrCodeEncoder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface TicketUiState {
    data object Loading : TicketUiState

    /** Compra inexistente, não aprovada ou sem ingresso emitido: nunca renderiza QR. */
    data object Unavailable : TicketUiState

    data class Content(
        val ticketId: String,
        val eventName: String,
        val eventDateTime: String,
        val eventLocation: String,
        val qrPayload: String,
    ) : TicketUiState
}

@HiltViewModel
class TicketViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    purchaseRepository: PurchaseRepository,
    private val qrCodeEncoder: QrCodeEncoder,
) : ViewModel() {
    private val purchaseId: String = checkNotNull(savedStateHandle["purchaseId"]) {
        "Rota de ingresso exige o argumento purchaseId"
    }

    val uiState: StateFlow<TicketUiState> = combine(
        purchaseRepository.observePurchase(purchaseId),
        purchaseRepository.observeTicket(purchaseId),
    ) { purchase, ticket ->
        if (purchase == null || purchase.status != PurchaseStatus.APPROVED || ticket == null) {
            TicketUiState.Unavailable
        } else {
            TicketUiState.Content(
                ticketId = ticket.id,
                eventName = purchase.eventName,
                eventDateTime = purchase.eventDateTime,
                eventLocation = purchase.eventLocation,
                qrPayload = ticket.qrPayload,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TicketUiState.Loading)

    fun renderQrCode(payload: String, sizePx: Int): ImageBitmap = qrCodeEncoder.encode(payload, sizePx)
}
