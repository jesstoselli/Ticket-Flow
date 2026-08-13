package com.jesstoselli.ticketflow.events.domain

import com.jesstoselli.ticketflow.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun observeEvents(): Flow<List<Event>>
    fun findById(id: String): Event?
}
