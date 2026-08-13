package com.jesstoselli.ticketflow.di

import android.content.Context
import androidx.room.Room
import com.jesstoselli.ticketflow.BuildConfig
import com.jesstoselli.ticketflow.database.TicketFlowDatabase
import com.jesstoselli.ticketflow.payment.cielo.CieloCredentials
import com.jesstoselli.ticketflow.payment.cielo.CieloDeepLinkFactory
import com.jesstoselli.ticketflow.payment.cielo.CieloDeepLinkPaymentGateway
import com.jesstoselli.ticketflow.payment.domain.PaymentGateway
import com.jesstoselli.ticketflow.purchase.data.OfflinePurchaseRepository
import com.jesstoselli.ticketflow.purchase.domain.PurchaseRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object InfrastructureModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TicketFlowDatabase =
        Room.databaseBuilder(context, TicketFlowDatabase::class.java, "ticketflow.db").build()

    @Provides
    @Singleton
    fun providePurchaseRepository(database: TicketFlowDatabase): PurchaseRepository =
        OfflinePurchaseRepository(database)

    @Provides
    @Singleton
    fun provideCieloCredentials(): CieloCredentials =
        CieloCredentials(
            clientId = BuildConfig.CIELO_CLIENT_ID,
            accessToken = BuildConfig.CIELO_ACCESS_TOKEN,
        )

    @Provides
    @Singleton
    fun provideCieloDeepLinkFactory(json: Json): CieloDeepLinkFactory = CieloDeepLinkFactory(json)

    @Provides
    @Singleton
    fun providePaymentGateway(
        @ApplicationContext context: Context,
        factory: CieloDeepLinkFactory,
        credentials: CieloCredentials,
    ): PaymentGateway = CieloDeepLinkPaymentGateway(context, factory, credentials)
}
