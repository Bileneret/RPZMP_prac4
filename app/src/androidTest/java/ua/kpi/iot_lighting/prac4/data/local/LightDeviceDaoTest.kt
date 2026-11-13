package ua.kpi.iot_lighting.prac4.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LightDeviceDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: LightDeviceDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.lightDeviceDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `setPower updates powered flag and lastUpdatedAt atomically`() = runTest {
        val initial = listOf(
            LightDeviceEntity(1L, "Lamp #1", "Вулиця", 120, true, 47.9108, 33.3924, 100L),
            LightDeviceEntity(2L, "Lamp #2", "Парк", 60, false, 47.9071, 33.3880, 200L),
        )
        dao.replaceAll(initial)
        assertEquals(2, dao.observeAll().first().size)

        dao.setPower(id = 1L, poweredOn = false, lastUpdatedAt = 555_555L)
        val afterUpdate = dao.getAll().first { it.id == 1L }
        assertFalse(afterUpdate.isPoweredOn)
        assertEquals(555_555L, afterUpdate.lastUpdatedAt)
    }

    @Test
    fun `deleteById and clearAll remove entities as expected`() = runTest {
        dao.upsertAll(
            listOf(
                LightDeviceEntity(1L, "A", "Z1", 100, true),
                LightDeviceEntity(2L, "B", "Z2", 200, false),
            ),
        )
        dao.deleteById(1L)
        assertEquals(listOf(2L), dao.getAll().map { it.id })

        dao.clearAll()
        assertTrue(dao.getAll().isEmpty())
    }
}
