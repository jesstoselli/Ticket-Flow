package com.jesstoselli.ticketflow

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Marca o início do processo atual. Como singleton, é criado uma única vez perto do
 * boot do app e serve de referência para a recuperação conservadora de pagamentos
 * interrompidos: só tentativas iniciadas antes deste instante são promovidas a pendente.
 */
@Singleton
class ProcessClock @Inject constructor() {
    val startedAtEpochMillis: Long = System.currentTimeMillis()
}
