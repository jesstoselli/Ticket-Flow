package com.jesstoselli.ticketflow.payment.cielo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.jesstoselli.ticketflow.MainActivity
import kotlinx.serialization.json.Json

class CieloCallbackActivity : ComponentActivity() {
    private val parser = CieloCallbackParser(Json { ignoreUnknownKeys = true })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(callbackIntent: Intent?) {
        val callback = callbackIntent?.dataString
        if (callback == null) {
            CallbackParseResult.Invalid("Callback sem URI")
        } else {
            parser.parse(callback)
        }
        PaymentForegroundService.stop(this)
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }
}
