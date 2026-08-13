package com.jesstoselli.ticketflow.purchase.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jesstoselli.ticketflow.model.PurchaseStatus

@Composable
fun PurchaseResultScreen(
    uiState: PurchaseResultUiState,
    onRetry: () -> Unit,
    onViewTicket: (String) -> Unit,
    onBackToEvents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        PurchaseResultUiState.Loading -> Centered(modifier) { CircularProgressIndicator() }

        PurchaseResultUiState.NotFound -> Centered(modifier) {
            Text(
                text = "Compra não encontrada.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is PurchaseResultUiState.Content -> ResultContent(
            state = uiState,
            onRetry = onRetry,
            onViewTicket = onViewTicket,
            onBackToEvents = onBackToEvents,
            modifier = modifier,
        )
    }
}

@Composable
private fun ResultContent(
    state: PurchaseResultUiState.Content,
    onRetry: () -> Unit,
    onViewTicket: (String) -> Unit,
    onBackToEvents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val purchase = state.purchase
    val copy = purchase.status.toCopy()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = copy.badge, style = MaterialTheme.typography.displaySmall)
        Text(text = copy.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(
            text = copy.message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (purchase.status == PurchaseStatus.APPROVED) {
            Spacer(Modifier.height(8.dp))
            ReceiptContent(purchase)
        }

        Spacer(Modifier.weight(1f))

        when {
            purchase.status == PurchaseStatus.APPROVED -> Button(
                onClick = { onViewTicket(purchase.id) },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("Ver ingresso", fontWeight = FontWeight.Bold) }

            state.canRetry -> Button(
                onClick = onRetry,
                enabled = !state.isRetrying,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text(if (state.isRetrying) "Processando…" else "Tentar novamente", fontWeight = FontWeight.Bold) }
        }

        OutlinedButton(
            onClick = onBackToEvents,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Voltar aos eventos") }
    }
}

@Composable
private fun Centered(modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

private data class ResultCopy(val badge: String, val title: String, val message: String)

private fun PurchaseStatus.toCopy(): ResultCopy = when (this) {
    PurchaseStatus.APPROVED -> ResultCopy(
        "✅", "Pagamento aprovado",
        "Seu ingresso já está disponível. Bom evento!",
    )
    PurchaseStatus.DENIED -> ResultCopy(
        "⛔", "Pagamento negado",
        "A operadora não autorizou a cobrança. Você pode tentar novamente.",
    )
    PurchaseStatus.CANCELLED -> ResultCopy(
        "↩️", "Pagamento cancelado",
        "O pagamento foi cancelado. Nenhuma cobrança foi concluída.",
    )
    PurchaseStatus.FAILED -> ResultCopy(
        "⚠️", "Falha no pagamento",
        "Algo deu errado durante o pagamento. Você pode tentar novamente.",
    )
    PurchaseStatus.PENDING -> ResultCopy(
        "⏳", "Pagamento pendente",
        "Não recebemos confirmação. Consulte o histórico antes de tentar de novo — não reenviamos a cobrança automaticamente.",
    )
    PurchaseStatus.PAYMENT_IN_PROGRESS -> ResultCopy(
        "⏳", "Processando pagamento",
        "Estamos aguardando o retorno da Cielo.",
    )
    PurchaseStatus.DRAFT -> ResultCopy(
        "🧾", "Compra iniciada",
        "Esta compra ainda não foi paga.",
    )
}
