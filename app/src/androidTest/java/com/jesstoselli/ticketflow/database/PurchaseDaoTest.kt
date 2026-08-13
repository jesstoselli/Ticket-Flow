package com.jesstoselli.ticketflow.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jesstoselli.ticketflow.model.PaymentResult
import com.jesstoselli.ticketflow.purchase.data.IdGenerator
import com.jesstoselli.ticketflow.purchase.data.OfflinePurchaseRepository
import com.jesstoselli.ticketflow.purchase.data.TimeProvider
import com.jesstoselli.ticketflow.purchase.domain.ApplyResult
import com.jesstoselli.ticketflow.purchase.domain.PurchaseSelection
import com.jesstoselli.ticketflow.purchase.domain.StartAttemptResult
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PurchaseDaoTest {
    private lateinit var database: TicketFlowDatabase
    private lateinit var dao: PurchaseDao
    private lateinit var repository: OfflinePurchaseRepository
    private val idCounter = AtomicInteger()

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            TicketFlowDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.purchaseDao()
        repository = OfflinePurchaseRepository(
            database = database,
            idGenerator = IdGenerator { "id-${idCounter.incrementAndGet()}" },
            timeProvider = TimeProvider { 1_000L + idCounter.get() },
        )
    }

    @After fun close() = database.close()

    @Test fun startingAttemptAtomicallyLocksPurchase() = runTest {
        val purchase = repository.createDraft(selection())

        assertTrue(repository.startAttempt(purchase.id) is StartAttemptResult.Started)
        assertTrue(repository.startAttempt(purchase.id) is StartAttemptResult.Rejected)
        assertEquals(1, dao.countAttemptsForPurchase(purchase.id))
    }

    @Test fun duplicateApprovalCreatesOneTicket() = runTest {
        val purchase = repository.createDraft(selection())
        val attempt = repository.startAttempt(purchase.id) as StartAttemptResult.Started
        val approved = PaymentResult.Approved("transaction", "auth")

        assertEquals(ApplyResult.Applied, repository.applyPaymentResult(attempt.reference, approved))
        assertEquals(ApplyResult.AlreadyApplied, repository.applyPaymentResult(attempt.reference, approved))
        assertEquals(1, dao.countTicketsForPurchase(purchase.id))
    }

    @Test fun explicitRetryIsAllowedAfterCancellation() = runTest {
        val purchase = repository.createDraft(selection())
        val first = repository.startAttempt(purchase.id) as StartAttemptResult.Started
        repository.applyPaymentResult(first.reference, PaymentResult.Cancelled("cancelled"))

        assertTrue(repository.startAttempt(purchase.id) is StartAttemptResult.Started)
        assertEquals(2, dao.countAttemptsForPurchase(purchase.id))
    }

    @Test fun divergentTerminalCallbackIsIgnored() = runTest {
        val purchase = repository.createDraft(selection())
        val attempt = repository.startAttempt(purchase.id) as StartAttemptResult.Started
        repository.applyPaymentResult(attempt.reference, PaymentResult.Approved("transaction", "auth"))

        assertEquals(
            ApplyResult.ConflictIgnored,
            repository.applyPaymentResult(attempt.reference, PaymentResult.Failed("2", "error")),
        )
    }

    private fun selection() = PurchaseSelection(
        eventId = "event-1",
        eventName = "Festival Aurora",
        eventDateTime = "2026-09-12T20:00:00-03:00",
        eventLocation = "São Paulo",
        quantity = 2,
        unitPriceInCents = 5_000,
    )
}
