package com.jesstoselli.ticketflow.di

import com.jesstoselli.ticketflow.events.data.LocalEventRepository
import com.jesstoselli.ticketflow.events.domain.EventRepository
import com.jesstoselli.ticketflow.ticket.data.ZxingQrCodeEncoder
import com.jesstoselli.ticketflow.ticket.domain.QrCodeEncoder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindEventRepository(repository: LocalEventRepository): EventRepository

    @Binds
    @Singleton
    abstract fun bindQrCodeEncoder(encoder: ZxingQrCodeEncoder): QrCodeEncoder
}
