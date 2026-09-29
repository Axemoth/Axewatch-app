package com.aistudio.axewatch.trader.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.axewatch.trader.data.local.AppDatabase
import com.aistudio.axewatch.trader.data.local.entity.AllotmentRecordEntity
import com.aistudio.axewatch.trader.data.repository.AxewatchRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AllotmentPersistenceTest {
    @Test fun manualCountsRoundTripWithoutInferringAppliedFromAllotted() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repo = AxewatchRepository(db)
            repo.recordManualAllotment("AB*****F", "FIRST", "ALLOTTED", 50, "one", 150)
            repo.recordManualAllotment("AB*****F", "SECOND", "ALLOTTED", 50, "two")
            val records = db.panVaultDao().getAllRecords().first()
            assertEquals(2, records.size)
            assertEquals(150, records.single { it.ipoSymbol == "FIRST" }.sharesApplied)
            assertEquals("USER", records.single { it.ipoSymbol == "FIRST" }.appliedSharesSource)
            assertEquals(0, records.single { it.ipoSymbol == "SECOND" }.sharesApplied)
            assertEquals("UNKNOWN", records.single { it.ipoSymbol == "SECOND" }.appliedSharesSource)
            assertTrue(records.all { it.sharesAllotted == 50 && it.status == "ALLOTTED" })
        } finally { db.close() }
    }

    @Test fun unavailableDetailRefreshKeepsOriginalRowWithoutAddingHistory() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val old = AllotmentRecordEntity(9, "AB*****F", "TEST", "Test Company", 150, 50, "ALLOTTED", "Bigshare")
            db.panVaultDao().insertRecord(old)
            assertFalse(AxewatchRepository(db).refreshAllotmentDetails(old, "ABCDE1234F"))
            assertEquals(listOf(old), db.panVaultDao().getAllRecords().first())
        } finally { db.close() }
    }
}
