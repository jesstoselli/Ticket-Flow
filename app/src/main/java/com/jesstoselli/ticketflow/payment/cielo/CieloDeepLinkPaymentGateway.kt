package com.jesstoselli.ticketflow.payment.cielo

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import com.jesstoselli.ticketflow.payment.domain.PaymentGateway
import com.jesstoselli.ticketflow.payment.domain.PaymentLaunchRequest
import com.jesstoselli.ticketflow.payment.domain.PaymentLaunchResult

class CieloDeepLinkPaymentGateway(
    private val context: Context,
    private val deepLinkFactory: CieloDeepLinkFactory,
    private val credentials: CieloCredentials,
) : PaymentGateway {
    override suspend fun launch(request: PaymentLaunchRequest): PaymentLaunchResult {
        if (credentials.missingKeys.isNotEmpty()) {
            return PaymentLaunchResult.ConfigurationError(credentials.missingKeys)
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkFactory.create(request, credentials)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) == null) {
            return PaymentLaunchResult.HandlerUnavailable
        }

        ContextCompat.startForegroundService(context, PaymentForegroundService.startIntent(context))
        context.startActivity(intent)
        return PaymentLaunchResult.Launched
    }
}
