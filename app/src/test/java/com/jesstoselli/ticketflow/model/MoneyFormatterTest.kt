package com.jesstoselli.ticketflow.model

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {
    @Test
    fun brlFormattingUsesCentsWithoutFloatingPointMath() {
        assertEquals("R$ 123,45", formatBrl(12_345, Locale.forLanguageTag("pt-BR")))
    }

    @Test
    fun brlFormattingSupportsZero() {
        assertEquals("R$ 0,00", formatBrl(0, Locale.forLanguageTag("pt-BR")))
    }
}
