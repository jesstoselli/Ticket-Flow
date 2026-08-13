package com.jesstoselli.ticketflow.payment.cielo

import org.junit.Assert.assertEquals
import org.junit.Test

class CieloCredentialsTest {
    @Test
    fun blankCredentialsReportBothConfigurationKeys() {
        assertEquals(
            setOf("CIELO_CLIENT_ID", "CIELO_ACCESS_TOKEN"),
            CieloCredentials("", "").missingKeys,
        )
    }

    @Test
    fun configuredCredentialsHaveNoMissingKeys() {
        assertEquals(emptySet<String>(), CieloCredentials("client", "token").missingKeys)
    }
}
