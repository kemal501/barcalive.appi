package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = 1,
    val username: String = "BarcaFan_01",
    val level: Int = 1,
    val xp: Int = 0,
    val vipLevel: Int = 0,
    val coins: Double = 1200.0, // Starting balance to let user explore freely
    val followers: Int = 245,
    val following: Int = 68,
    val streamsCreated: Int = 2,
    val myReferralCode: String = "BARCA_99",
    val referredBy: String = ""
)

@Entity(tableName = "streams")
data class StreamEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // Music, Gaming, Chat, Open Mic, Fish Game
    val hostUsername: String,
    val isLive: Boolean = true,
    val initialViewers: Int = 0,
    val currentViewers: Int = 0,
    val likesCount: Int = 0,
    val giftCoinsReceived: Double = 0.0,
    val startTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "wallet_transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // WITHDRAW, DEPOSIT, GIFT_SENT, GIFT_RECEIVED, MISSION_REWARD, GAME_WIN, GAME_LOSS
    val amountCoins: Double,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_missions")
data class MissionEntity(
    @PrimaryKey val id: String, // e.g., "login", "stream", "gift", "fish"
    val title: String,
    val description: String,
    val rewardCoins: Double,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false,
    val currentProgress: Int = 0,
    val targetProgress: Int = 1
)
