package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.MeasurementDao
import com.example.data.MeasurementRecord
import com.example.data.MeasurementType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: MeasurementDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.measurementDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ARMeasure Pro", appName)
    }

    @Test
    fun `insert and retrieve measurement record in database`() = runBlocking {
        val record = MeasurementRecord(
            type = MeasurementType.VOLUME.name,
            label = "Storage Box",
            formattedValue = "0.450 m³",
            primaryValue = 0.45,
            unit = "m³",
            details = "L: 1.00m x W: 0.60m x H: 0.45m",
            pointCount = 4
        )
        val id = dao.insert(record)
        val retrieved = dao.getMeasurementById(id)
        assertEquals("Storage Box", retrieved?.label)
        assertEquals("0.450 m³", retrieved?.formattedValue)
        assertEquals(MeasurementType.VOLUME.name, retrieved?.type)

        val all = dao.getAllMeasurements().first()
        assertEquals(1, all.size)
    }
}
