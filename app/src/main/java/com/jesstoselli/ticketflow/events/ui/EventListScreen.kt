package com.jesstoselli.ticketflow.events.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jesstoselli.ticketflow.designsystem.EventCard

@Composable
fun EventListScreen(
    uiState: EventListUiState,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        EventListUiState.Loading -> Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) { CircularProgressIndicator(Modifier.padding(24.dp)) }

        is EventListUiState.Content -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = "Sua próxima história começa aqui.",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
                    text = "Eventos escolhidos para viver de perto.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(uiState.events, key = { it.id }) { event ->
                EventCard(event = event, onClick = { onEventClick(event.id) })
            }
        }
    }
}
