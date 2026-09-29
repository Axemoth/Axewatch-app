package com.aistudio.axewatch.trader.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.axewatch.trader.data.local.AppDatabase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DatabaseMigrationTest {
    @Test fun versionOneUpgradePreservesEveryUserTable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test.db"
        context.deleteDatabase(name)
        val before = linkedMapOf<String, List<String?>>()
        context.openOrCreateDatabase(name, 0, null).use { db ->
            javaClass.classLoader!!.getResourceAsStream("schema-v1.sql")!!.bufferedReader().useLines { lines ->
                lines.filter { it.isNotBlank() }.forEach { db.execSQL(it) }
            }
            db.execSQL("INSERT INTO holdings VALUES(4,'ACME','Acme','EQUITY',25,120,123,'Industry','saved')")
            db.execSQL("INSERT INTO paper_account VALUES(1,900000,1000000,1500,3,2)")
            db.execSQL("INSERT INTO paper_positions VALUES('ACME','Acme',20,100,125,90,150,123)")
            db.execSQL("INSERT INTO paper_orders VALUES(8,'ACME','BUY',20,100,'MARKET','FILLED',0,123,'saved')")
            db.execSQL("INSERT INTO pan_vault VALUES('ABCDE1234F','Test holder','Self',123)")
            db.execSQL("INSERT INTO allotment_records VALUES(7,'AB*****F','ACME','Acme',150,50,'ALLOTTED','MUFG',123,'application')")
            db.execSQL("INSERT INTO watchlist VALUES('ACME','Acme','EQUITY',100,150,123)")
            for (table in listOf("holdings", "paper_account", "paper_positions", "paper_orders", "pan_vault", "allotment_records", "watchlist")) {
                db.rawQuery("SELECT * FROM $table", null).use { c ->
                    assertTrue(c.moveToFirst()); before[table] = (0 until c.columnCount).map { c.getString(it) }
                }
            }
            db.version = 1
        }
        val room = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2).allowMainThreadQueries().build()
        try {
            val db = room.openHelper.writableDatabase // runs Room schema validation after migration
            assertEquals(2, db.version)
            for ((table, values) in before) db.query("SELECT * FROM $table").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(values, values.indices.map { c.getString(it) })
                if (table == "allotment_records") assertEquals("LEGACY", c.getString(c.getColumnIndexOrThrow("appliedSharesSource")))
                assertFalse(c.moveToNext())
            }
        } finally { room.close(); context.deleteDatabase(name) }
    }
}
