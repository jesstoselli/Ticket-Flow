package com.jesstoselli.ticketflow.ticket.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TicketScreen(
    uiState: TicketUiState,
    renderQrCode: (String, Int) -> ImageBitmap,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        TicketUiState.Loading -> Centered(modifier) { CircularProgressIndicator() }

        TicketUiState.Unavailable -> Centered(modifier) {
            Text(
                text = "Ingresso disponível apenas para compras aprovadas.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is TicketUiState.Content -> Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = uiState.eventName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "${uiState.eventDateTime} · ${uiState.eventLocation}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val qrCode = remember(uiState.qrPayload) { renderQrCode(uiState.qrPayload, QR_SIZE_PX) }
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(16.dp),
            ) {
                Image(
                    bitmap = qrCode,
                    contentDescription = "QR Code do ingresso",
                    modifier = Modifier.size(240.dp),
                )
            }

            Text(
                text = "ID ${uiState.ticketId}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val QR_SIZE_PX = 600

@Composable
private fun Centered(modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}
