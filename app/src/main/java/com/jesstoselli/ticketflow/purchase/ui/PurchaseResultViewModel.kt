package com.jesstoselli.ticketflow.purchase.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesstoselli.ticketflow.checkout.domain.StartPaymentUseCase
import com.jesstoselli.ticketflow.model.Purchase
import com.jesstoselli.ticketflow.model.PurchaseStatus
import com.jesstoselli.ticketflow.purchase.domain.PurchaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PurchaseResultUiState {
    data object Loading : PurchaseResultUiState
    data object NotFound : PurchaseResultUiState
    data class Content(
        val purchase: Purchase,
        val canRetry: Boolean,
        val isRetrying: Boolean,
    ) : PurchaseResultUiState
}

@HiltViewModel
class PurchaseResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val purchaseRepository: PurchaseRepository,
    private val startPayment: StartPaymentUseCase,
) : ViewModel() {
    private val purchaseId: String = checkNotNull(savedStateHandle["purchaseId"]) {
        "Rota de resultado exige o argumento purchaseId"
    }
    private val isRetrying = MutableStateFlow(false)

    val uiState: StateFlow<PurchaseResultUiState> =
        combine(purchaseRepository.observePurchase(purchaseId), isRetrying) { purchase, retrying ->
            when (purchase) {
                null -> PurchaseResultUiState.NotFound
                else -> PurchaseResultUiState.Content(
                    purchase = purchase,
                    canRetry = purchase.status in RETRYABLE_STATUSES,
                    isRetrying = retrying,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PurchaseResultUiState.Loading)

    /** Nova tentativa explícita após negativa, cancelamento ou falha — cria nova referência. */
    fun retry() {
        if (isRetrying.value) return
        isRetrying.value = true
        viewModelScope.launch {
            startPayment(purchaseId)
            isRetrying.value = false
        }
    }

    private companion object {
        val RETRYABLE_STATUSES = setOf(
            PurchaseStatus.DENIED,
            PurchaseStatus.CANCELLED,
            PurchaseStatus.FAILED,
        )
    }
}
