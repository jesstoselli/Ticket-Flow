package com.jesstoselli.ticketflow.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jesstoselli.ticketflow.events.ui.EventListScreen
import com.jesstoselli.ticketflow.events.ui.EventListViewModel

@Composable
fun TicketFlowNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TicketFlowDestination.Events.route,
        modifier = modifier,
    ) {
        composable(TicketFlowDestination.Events.route) {
            val viewModel: EventListViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            EventListScreen(
                uiState = uiState,
                onEventClick = { navController.navigate(TicketFlowDestination.Checkout.create(it)) },
            )
        }
        composable(TicketFlowDestination.Tickets.route) {
            Text("Seus ingressos aparecerão aqui")
        }
        composable(TicketFlowDestination.Checkout.route) {
            Text("Checkout")
        }
    }
}
