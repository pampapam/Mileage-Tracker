package com.example.data.dao

import androidx.room.*
import com.example.data.entity.Trip
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun getAllTripsFlow(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE isActive = 0 ORDER BY startTime DESC")
    fun getCompletedTripsFlow(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE isActive = 1 LIMIT 1")
    fun getActiveTripFlow(): Flow<Trip?>

    @Query("SELECT * FROM trips WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTripSync(): Trip?

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getTripById(id: Int): Trip?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: Trip): Long

    @Update
    suspend fun update(trip: Trip)

    @Delete
    suspend fun delete(trip: Trip)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM trips")
    suspend fun deleteAll()

    @Query("SELECT SUM(distanceMeters) FROM trips WHERE isActive = 0")
    fun getTotalCompletedDistanceFlow(): Flow<Double?>
}
