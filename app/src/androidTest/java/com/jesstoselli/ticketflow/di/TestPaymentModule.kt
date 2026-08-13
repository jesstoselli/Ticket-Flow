package com.jesstoselli.ticketflow.di

import com.jesstoselli.ticketflow.FakePaymentGateway
import com.jesstoselli.ticketflow.payment.domain.PaymentGateway
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/** Substitui o gateway real da Cielo pelo [FakePaymentGateway] nos testes instrumentados. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [PaymentGatewayModule::class])
object TestPaymentModule {
    @Provides
    @Singleton
    fun providePaymentGateway(fake: FakePaymentGateway): PaymentGateway = fake
}
