package com.jesstoselli.ticketflow.payment.cielo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.jesstoselli.ticketflow.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Recebe o Deep Link de retorno da Cielo. Não renderiza UI própria: delega a URI ao
 * [HandleCieloCallbackUseCase], encerra o foreground service e reabre a [MainActivity]
 * apontando para o resultado da compra. Callback inválido nunca fabrica aprovação.
 */
@AndroidEntryPoint
class CieloCallbackActivity : ComponentActivity() {

    @Inject lateinit var handleCallback: HandleCieloCallbackUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    private fun handle(callbackIntent: Intent?) {
        val callbackUrl = callbackIntent?.dataString
        lifecycleScope.launch {
            val outcome = handleCallback(callbackUrl)
            PaymentForegroundService.stop(this@CieloCallbackActivity)
            val purchaseId = (outcome as? HandleCallbackOutcome.Handled)?.purchaseId
            startActivity(
                Intent(this@CieloCallbackActivity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .apply { if (purchaseId != null) putExtra(MainActivity.EXTRA_PURCHASE_ID, purchaseId) },
            )
            finish()
        }
    }
}
