package com.jesstoselli.ticketflow

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jesstoselli.ticketflow.designsystem.TicketFlowTheme
import com.jesstoselli.ticketflow.navigation.TicketFlowDestination
import com.jesstoselli.ticketflow.navigation.TicketFlowNavHost

@Composable
fun TicketFlowApp() {
    TicketFlowTheme {
        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        val currentRoute = backStack?.destination?.route
        val showNavigation = currentRoute in setOf(
            TicketFlowDestination.Events.route,
            TicketFlowDestination.Tickets.route,
        )
        Scaffold(
            bottomBar = {
                if (showNavigation) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentRoute == TicketFlowDestination.Events.route,
                            onClick = { navController.navigate(TicketFlowDestination.Events.route) },
                            icon = { Text("✦") },
                            label = { Text("Eventos") },
                        )
                        NavigationBarItem(
                            selected = currentRoute == TicketFlowDestination.Tickets.route,
                            onClick = { navController.navigate(TicketFlowDestination.Tickets.route) },
                            icon = { Text("▣") },
                            label = { Text("Ingressos") },
                        )
                    }
                }
            },
        ) { padding ->
            TicketFlowNavHost(navController = navController, modifier = Modifier.padding(padding))
        }
    }
}
