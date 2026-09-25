package com.aistudio.axewatch.trader.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.axewatch.trader.data.local.AppDatabase
import com.aistudio.axewatch.trader.data.remote.DirectoryEntry
import com.aistudio.axewatch.trader.data.remote.IpoAllotmentService
import com.aistudio.axewatch.trader.data.remote.RegistrarDirectory
import com.aistudio.axewatch.trader.data.repository.AxewatchRepository
import com.aistudio.axewatch.trader.data.work.AllotNotifications
import com.aistudio.axewatch.trader.data.work.AllotWatcherWorker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AllotmentRegressionTest {
    @Test fun `kfin never combines two different partial company matches`() {
        val payload = """[
          {"Company":"Acme Industries Steel", "App_Shares":10,"All_Shares":10},
          {"Company":"Acme Industries Energy", "App_Shares":20,"All_Shares":20}
        ]"""
        val service = IpoAllotmentService()
        assertThrows(com.aistudio.axewatch.trader.data.remote.AllotmentTransportException::class.java) {
            service.parseKfinJson(payload, "Acme Industries")
        }
        assertEquals(20, service.parseKfinJson(payload, "Acme Industries Energy").sharesAllotted)
    }

    @Test fun `short and blank names cannot select a long company`() {
        val service = IpoAllotmentService()
        val companies = listOf(com.aistudio.axewatch.trader.data.remote.MufgCompany("1", "Arc Industries Limited"))
        assertNull(service.findBestCompanyMatch("Arc", "ARC", companies))
        assertNull(service.findBestCompanyMatch("", "", companies))
    }

    @Test fun `ambiguous directory match remains unknown while exact wins`() {
        val one = DirectoryEntry("mufg", "Acme Industries Steel")
        val two = DirectoryEntry("kfin", "Acme Industries Energy")
        val directory = mapOf("acmeindustriessteel" to one, "acmeindustriesenergy" to two)
        assertNull(RegistrarDirectory.lookup(directory, "Acme Industries"))
        assertEquals(two, RegistrarDirectory.lookup(directory, "Acme Industries Energy Ltd"))
    }

    @Test fun `blank allotment date does not shift registrar column`() {
        val rows = RegistrarDirectory.parseIpomarket("<h3>IPOs</h3><table><tr><td>Acme Ltd</td><td></td><td>KFintech</td></tr></table>")
        assertEquals("kfin", rows.single().registrar)
        assertEquals("", rows.single().allotmentDate)
    }

    @Test fun `authoritative entry replaces weak mapping`() {
        val directory = mutableMapOf("acme" to DirectoryEntry("kfin", "Acme"))
        RegistrarDirectory.mergeInto(directory, listOf(DirectoryEntry("mufg", "Acme", authoritative = true))) { IpoAllotmentService.canonIpoName(it) }
        assertEquals("mufg", directory.getValue("acme").registrar)
    }

    @Test fun `background declared override queries requested issue even without live list`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repo = AxewatchRepository(db)
            val record = repo.checkIpoAllotment("ABCDE1234F", "REQUESTED", knownName = "Requested Ltd", knownRegistrar = "Bigshare", knownDeclared = true)
            assertEquals("REQUESTED", record.ipoSymbol)
            assertEquals("Requested Ltd", record.ipoName)
            assertEquals("MANUAL_CHECK_REQUIRED", record.status)
            assertEquals("AB*****F", record.maskedPan)
        } finally { db.close() }
    }

    @Test fun `disabled notification is not acknowledged and can be delivered after enabling`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowOf(manager).setNotificationsEnabled(false)
        assertFalse(AllotNotifications.postNotification(context, 7, "Result", "Saved PAN"))
        shadowOf(manager).setNotificationsEnabled(true)
        assertTrue(AllotNotifications.postNotification(context, 7, "Result", "Saved PAN"))
        assertEquals(1, shadowOf(manager).allNotifications.size)
    }

    @Test fun `blocked channel is not acknowledged`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowOf(manager).setNotificationsEnabled(true)
        manager.createNotificationChannel(NotificationChannel(AllotWatcherWorker.CHANNEL_ID, "Results", NotificationManager.IMPORTANCE_NONE))
        assertFalse(AllotNotifications.postNotification(context, 8, "Result", "Saved PAN"))
    }
}
