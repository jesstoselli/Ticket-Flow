package com.jesstoselli.ticketflow.events.data

import com.jesstoselli.ticketflow.events.domain.EventRepository
import com.jesstoselli.ticketflow.model.Event
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Singleton
class LocalEventRepository @Inject constructor() : EventRepository {
    private val events = listOf(
        Event(
            id = "festival-aurora",
            name = "Festival Aurora",
            description = "Uma noite de música brasileira, luz e encontros ao ar livre.",
            dateTime = "12 SET · 20:00",
            location = "Parque Villa-Lobos · São Paulo",
            unitPriceInCents = 12_500,
            availableTickets = 84,
        ),
        Event(
            id = "samba-no-ceu",
            name = "Samba no Céu",
            description = "Roda de samba no terraço, com vista para o centro da cidade.",
            dateTime = "19 SET · 18:30",
            location = "Edifício Martinelli · São Paulo",
            unitPriceInCents = 8_000,
            availableTickets = 32,
        ),
        Event(
            id = "cinema-jardim",
            name = "Cinema no Jardim",
            description = "Sessão especial, trilha ao vivo e comida de rua sob as estrelas.",
            dateTime = "03 OUT · 19:00",
            location = "Jardim Botânico · São Paulo",
            unitPriceInCents = 6_500,
            availableTickets = 120,
        ),
    )

    override fun observeEvents(): Flow<List<Event>> = flowOf(events)
    override fun findById(id: String): Event? = events.find { it.id == id }
}
