package com.jesstoselli.ticketflow.di

import android.content.Context
import com.jesstoselli.ticketflow.payment.cielo.CieloCredentials
import com.jesstoselli.ticketflow.payment.cielo.CieloDeepLinkFactory
import com.jesstoselli.ticketflow.payment.cielo.CieloDeepLinkPaymentGateway
import com.jesstoselli.ticketflow.payment.domain.PaymentGateway
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo isolado do gateway de pagamento para que os testes instrumentados possam
 * substituí-lo por um fake determinístico via `@TestInstallIn`, sem depender de outro APK.
 */
@Module
@InstallIn(SingletonComponent::class)
object PaymentGatewayModule {
    @Provides
    @Singleton
    fun providePaymentGateway(
        @ApplicationContext context: Context,
        factory: CieloDeepLinkFactory,
        credentials: CieloCredentials,
    ): PaymentGateway = CieloDeepLinkPaymentGateway(context, factory, credentials)
}
