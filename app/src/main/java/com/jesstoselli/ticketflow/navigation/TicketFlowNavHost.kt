package com.jesstoselli.ticketflow.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jesstoselli.ticketflow.checkout.ui.CheckoutEvent
import com.jesstoselli.ticketflow.checkout.ui.CheckoutScreen
import com.jesstoselli.ticketflow.checkout.ui.CheckoutViewModel
import com.jesstoselli.ticketflow.events.ui.EventListScreen
import com.jesstoselli.ticketflow.events.ui.EventListViewModel
import com.jesstoselli.ticketflow.purchase.ui.PurchaseHistoryScreen
import com.jesstoselli.ticketflow.purchase.ui.PurchaseHistoryViewModel
import com.jesstoselli.ticketflow.purchase.ui.PurchaseResultScreen
import com.jesstoselli.ticketflow.purchase.ui.PurchaseResultViewModel
import com.jesstoselli.ticketflow.ticket.ui.TicketScreen
import com.jesstoselli.ticketflow.ticket.ui.TicketViewModel
import androidx.compose.runtime.LaunchedEffect

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
            val viewModel: PurchaseHistoryViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            PurchaseHistoryScreen(
                uiState = uiState,
                onOpenPurchase = { navController.navigate(TicketFlowDestination.PurchaseResult.create(it)) },
            )
        }
        composable(
            route = TicketFlowDestination.Checkout.route,
            arguments = listOf(navArgument(TicketFlowDestination.Checkout.ARG_EVENT_ID) { type = NavType.StringType }),
        ) {
            val viewModel: CheckoutViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {
                        is CheckoutEvent.NavigateToResult -> navController.navigate(
                            TicketFlowDestination.PurchaseResult.create(event.purchaseId),
                        ) {
                            popUpTo(TicketFlowDestination.Checkout.route) { inclusive = true }
                        }
                    }
                }
            }
            CheckoutScreen(
                uiState = uiState,
                onIncrease = viewModel::increaseQuantity,
                onDecrease = viewModel::decreaseQuantity,
                onPay = viewModel::pay,
                onBack = { navController.navigateUp() },
            )
        }
        composable(
            route = TicketFlowDestination.PurchaseResult.route,
            arguments = listOf(navArgument(TicketFlowDestination.PurchaseResult.ARG_PURCHASE_ID) { type = NavType.StringType }),
        ) {
            val viewModel: PurchaseResultViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            PurchaseResultScreen(
                uiState = uiState,
                onRetry = viewModel::retry,
                onViewTicket = { navController.navigate(TicketFlowDestination.Ticket.create(it)) },
                onBackToEvents = {
                    navController.navigate(TicketFlowDestination.Events.route) {
                        popUpTo(TicketFlowDestination.Events.route) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = TicketFlowDestination.Ticket.route,
            arguments = listOf(navArgument(TicketFlowDestination.Ticket.ARG_PURCHASE_ID) { type = NavType.StringType }),
        ) {
            val viewModel: TicketViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            TicketScreen(
                uiState = uiState,
                renderQrCode = viewModel::renderQrCode,
                onBack = { navController.navigateUp() },
            )
        }
    }
}
