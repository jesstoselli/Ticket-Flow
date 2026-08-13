package com.jesstoselli.ticketflow

import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jesstoselli.ticketflow.designsystem.TicketFlowTheme

@Composable
fun TicketFlowApp() {
    TicketFlowTheme {
        Surface {
            Text(text = stringResource(R.string.app_name))
        }
    }
}
