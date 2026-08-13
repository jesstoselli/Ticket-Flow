package com.jesstoselli.ticketflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesstoselli.ticketflow.purchase.domain.MarkInterruptedPaymentsPendingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    processClock: ProcessClock,
    markInterruptedPaymentsPending: MarkInterruptedPaymentsPendingUseCase,
) : ViewModel() {
    init {
        viewModelScope.launch {
            markInterruptedPaymentsPending(processClock.startedAtEpochMillis)
        }
    }
}
