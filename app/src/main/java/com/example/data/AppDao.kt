package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // User Profile Stats
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun getUserStats(): Flow<UserStatsEntity?>

    @Query("SELECT * FROM user_stats WHERE id = 1")
    suspend fun getUserStatsSync(): UserStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserStats(stats: UserStatsEntity)

    // Streams Listing
    @Query("SELECT * FROM streams ORDER BY isLive DESC, id DESC")
    fun getAllStreams(): Flow<List<StreamEntity>>

    @Query("SELECT * FROM streams WHERE id = :id")
    suspend fun getStreamById(id: Int): StreamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStream(stream: StreamEntity): Long

    @Update
    suspend fun updateStream(stream: StreamEntity)

    @Query("DELETE FROM streams WHERE id = :id")
    suspend fun deleteStreamById(id: Int)

    // Wallet Ledger Logs
    @Query("SELECT * FROM wallet_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    // Daily Achievements & Missions
    @Query("SELECT * FROM daily_missions")
    fun getAllMissions(): Flow<List<MissionEntity>>

    @Query("SELECT * FROM daily_missions WHERE id = :id")
    suspend fun getMissionById(id: String): MissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMissions(missions: List<MissionEntity>)

    @Update
    suspend fun updateMission(mission: MissionEntity)

    @Query("UPDATE daily_missions SET isCompleted = 0, isClaimed = 0, currentProgress = 0")
    suspend fun resetAllProgress()
}
