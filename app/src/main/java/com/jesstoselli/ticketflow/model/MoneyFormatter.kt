package com.jesstoselli.ticketflow.model

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

fun formatBrl(
    valueInCents: Long,
    locale: Locale = Locale.forLanguageTag("pt-BR"),
): String {
    require(valueInCents >= 0)
    val amount = BigDecimal.valueOf(valueInCents, 2)
    return NumberFormat.getCurrencyInstance(locale).format(amount)
}
