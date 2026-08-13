package com.jesstoselli.ticketflow

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val resultPurchaseId = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultPurchaseId.value = intent?.getStringExtra(EXTRA_PURCHASE_ID)
        setContent {
            TicketFlowApp(
                resultPurchaseId = resultPurchaseId,
                onResultConsumed = { resultPurchaseId.value = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        resultPurchaseId.value = intent.getStringExtra(EXTRA_PURCHASE_ID)
    }

    companion object {
        const val EXTRA_PURCHASE_ID = "purchaseId"
    }
}
