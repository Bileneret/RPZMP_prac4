package ua.kpi.iot_lighting.prac4.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LightDeviceDao {
    @Query("SELECT * FROM light_devices ORDER BY id ASC")
    fun observeAll(): Flow<List<LightDeviceEntity>>

    @Query("SELECT * FROM light_devices ORDER BY id ASC")
    suspend fun getAll(): List<LightDeviceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(devices: List<LightDeviceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: LightDeviceEntity)

    @Query(
        "UPDATE light_devices SET isPoweredOn = :poweredOn, lastUpdatedAt = :lastUpdatedAt WHERE id = :id",
    )
    suspend fun setPower(id: Long, poweredOn: Boolean, lastUpdatedAt: Long)

    @Query("DELETE FROM light_devices WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM light_devices")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(devices: List<LightDeviceEntity>) {
        clearAll()
        upsertAll(devices)
    }
}
