package com.jesstoselli.ticketflow.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PurchaseEntity::class, PaymentAttemptEntity::class, TicketEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TicketFlowDatabase : RoomDatabase() {
    abstract fun purchaseDao(): PurchaseDao
}
