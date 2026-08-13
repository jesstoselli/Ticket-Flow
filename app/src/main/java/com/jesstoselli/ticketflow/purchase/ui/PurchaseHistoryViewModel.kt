package com.jesstoselli.ticketflow.purchase.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesstoselli.ticketflow.model.Purchase
import com.jesstoselli.ticketflow.model.PurchaseStatus
import com.jesstoselli.ticketflow.purchase.domain.PurchaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface PurchaseHistoryUiState {
    data object Loading : PurchaseHistoryUiState
    data class Content(val purchases: List<Purchase>) : PurchaseHistoryUiState
}

@HiltViewModel
class PurchaseHistoryViewModel @Inject constructor(
    repository: PurchaseRepository,
) : ViewModel() {
    val uiState: StateFlow<PurchaseHistoryUiState> = repository.observePurchases()
        // Rascunhos que nunca chegaram a iniciar pagamento não são ruído útil no histórico.
        .map { purchases -> purchases.filterNot { it.status == PurchaseStatus.DRAFT } }
        .map<List<Purchase>, PurchaseHistoryUiState>(PurchaseHistoryUiState::Content)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PurchaseHistoryUiState.Loading)
}
