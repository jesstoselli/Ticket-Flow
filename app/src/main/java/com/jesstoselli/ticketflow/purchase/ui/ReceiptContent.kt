package com.jesstoselli.ticketflow.purchase.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jesstoselli.ticketflow.model.Purchase
import com.jesstoselli.ticketflow.model.formatBrl

/**
 * Comprovante reutilizável. Mostra apenas dados não sensíveis: referência, evento,
 * quantidade, total e identificadores da transação. Nunca exibe token ou credencial.
 */
@Composable
fun ReceiptContent(purchase: Purchase, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ReceiptRow("Referência", purchase.currentAttemptReference ?: "—")
        ReceiptRow("Evento", purchase.eventName)
        ReceiptRow("Data", purchase.eventDateTime)
        ReceiptRow("Local", purchase.eventLocation)
        ReceiptRow("Quantidade", purchase.quantity.toString())
        ReceiptRow("Total", formatBrl(purchase.totalInCents))
        purchase.transactionId?.let { ReceiptRow("Transação", it) }
        purchase.authorizationCode?.let { ReceiptRow("Autorização", it) }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
