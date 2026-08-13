package com.jesstoselli.ticketflow.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val HyacinthBloom = Color(0xFF524B85)
val GuavaPunch = Color(0xFFDD6452)
val GoldenrodHour = Color(0xFFFDB913)
val GulfSwim = Color(0xFF68C9D0)
val TicketInk = Color(0xFF28243D)
val TicketPaper = Color(0xFFFFF9F2)
val TicketMist = Color(0xFFF3F0F7)

private val LightColors = lightColorScheme(
    primary = HyacinthBloom,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E1FF),
    onPrimaryContainer = TicketInk,
    secondary = GuavaPunch,
    onSecondary = TicketInk,
    secondaryContainer = Color(0xFFFFDAD4),
    onSecondaryContainer = TicketInk,
    tertiary = GulfSwim,
    onTertiary = TicketInk,
    tertiaryContainer = Color(0xFFC8F3F5),
    onTertiaryContainer = TicketInk,
    background = TicketPaper,
    onBackground = TicketInk,
    surface = Color.White,
    onSurface = TicketInk,
    surfaceVariant = TicketMist,
    onSurfaceVariant = Color(0xFF5F596B),
    outline = Color(0xFF7A7488),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC8BFFF),
    onPrimary = Color(0xFF29215C),
    secondary = Color(0xFFFFB4A8),
    onSecondary = Color(0xFF5A190F),
    tertiary = Color(0xFF80DDE4),
    onTertiary = Color(0xFF00373B),
    background = Color(0xFF1C1928),
    onBackground = Color(0xFFF1ECF7),
    surface = Color(0xFF252131),
    onSurface = Color(0xFFF1ECF7),
    surfaceVariant = Color(0xFF393444),
    onSurfaceVariant = Color(0xFFD1CAD9),
)

@Composable
fun TicketFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
