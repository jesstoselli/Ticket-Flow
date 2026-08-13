package com.jesstoselli.ticketflow.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Acentos vibrantes usados como detalhe (trilha do card). Legíveis sobre fundo escuro.
val GuavaPunch = Color(0xFFDD6452)
val GoldenrodHour = Color(0xFFFDB913)
val GulfSwim = Color(0xFF68C9D0)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC8BFFF),
    onPrimary = Color(0xFF29215C),
    primaryContainer = Color(0xFF3A3270),
    onPrimaryContainer = Color(0xFFE7E1FF),
    secondary = Color(0xFFFFB4A8),
    onSecondary = Color(0xFF5A190F),
    secondaryContainer = Color(0xFF43273F),
    onSecondaryContainer = Color(0xFFFFDAD4),
    tertiary = Color(0xFF80DDE4),
    onTertiary = Color(0xFF00373B),
    background = Color(0xFF1C1928),
    onBackground = Color(0xFFF1ECF7),
    surface = Color(0xFF252131),
    onSurface = Color(0xFFF1ECF7),
    surfaceVariant = Color(0xFF393444),
    onSurfaceVariant = Color(0xFFCFC8DB),
    outline = Color(0xFF8B8598),
)

/**
 * Ticket Flow adota tema escuro fixo — a experiência foi desenhada e verificada em dark.
 * Componentes devem consumir cores via `MaterialTheme.colorScheme`, nunca literais que só
 * funcionem em um fundo.
 */
@Composable
fun TicketFlowTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
