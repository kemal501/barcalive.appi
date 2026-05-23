package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class AppRepository(private val appDao: AppDao) {

    val userStats: Flow<UserStatsEntity?> = appDao.getUserStats()
    val allStreams: Flow<List<StreamEntity>> = appDao.getAllStreams()
    val allTransactions: Flow<List<TransactionEntity>> = appDao.getAllTransactions()
    val allMissions: Flow<List<MissionEntity>> = appDao.getAllMissions()

    suspend fun initializeDatabaseIfEmpty() {
        val stats = appDao.getUserStatsSync()
        if (stats == null) {
            // First time launch: Initialize profile with generous balance
            val defaultStats = UserStatsEntity(
                id = 1,
                username = "BarcaFan_01",
                level = 1,
                xp = 0,
                vipLevel = 0,
                coins = 1200.0,
                followers = 245,
                following = 68,
                streamsCreated = 0,
                myReferralCode = "BARCA_99"
            )
            appDao.insertUserStats(defaultStats)

            // Populate high quality streams matching Tailwind "Featured Streams"
            val defaultStreams = listOf(
                StreamEntity(
                    title = "Evening Chill Vibes \uD83C\uDFA7 Music Session",
                    category = "Music",
                    hostUsername = "alex_stream",
                    initialViewers = 1200,
                    currentViewers = 1205,
                    likesCount = 824
                ),
                StreamEntity(
                    title = "Pro Solo Rank Push \uD83C\uDFAE Barca Tournament",
                    category = "Gaming",
                    hostUsername = "game_pro_x",
                    initialViewers = 840,
                    currentViewers = 842,
                    likesCount = 612
                ),
                StreamEntity(
                    title = "Podcast: Real Talk \uD83C\uDF99\uFE0F Weekly Football Gossip",
                    category = "Open Mic",
                    hostUsername = "maria_radio",
                    initialViewers = 540,
                    currentViewers = 545,
                    likesCount = 421
                ),
                StreamEntity(
                    title = "Open Mic Friday \uD83C\uDFA4 Share Your Talent!",
                    category = "Open Mic",
                    hostUsername = "vocal_soul",
                    initialViewers = 310,
                    currentViewers = 312,
                    likesCount = 203
                ),
                StreamEntity(
                    title = "Legend Ocean Hunter \uD83D\uDC1F Fish Game Stream",
                    category = "Gaming",
                    hostUsername = "fish_master",
                    initialViewers = 620,
                    currentViewers = 627,
                    likesCount = 478
                ),
                StreamEntity(
                    title = "Barca Fan Chat \uD83D\uDDE3\uFE0F Post-Match Analysis",
                    category = "Chat",
                    hostUsername = "mes_que_un_club",
                    initialViewers = 412,
                    currentViewers = 418,
                    likesCount = 356
                )
            )
            for (stream in defaultStreams) {
                appDao.insertStream(stream)
            }

            // Populate system daily missions
            val defaultMissions = listOf(
                MissionEntity(
                    id = "login",
                    title = "\uD83C\uDF82 Daily Attendance",
                    description = "Claim your daily login bonus coin reward",
                    rewardCoins = 50.0,
                    currentProgress = 1,
                    targetProgress = 1,
                    isCompleted = true,
                    isClaimed = false
                ),
                MissionEntity(
                    id = "stream",
                    title = "\uD83D\uDCF9 Host a Broadcaster Room",
                    description = "Go live for at least 15 seconds to interact",
                    rewardCoins = 150.0,
                    currentProgress = 0,
                    targetProgress = 1,
                    isCompleted = false,
                    isClaimed = false
                ),
                MissionEntity(
                    id = "gift",
                    title = "\uD83C\uDF81 Support Live Creator",
                    description = "Send any gift to are live streamer in any room",
                    rewardCoins = 100.0,
                    currentProgress = 0,
                    targetProgress = 1,
                    isCompleted = false,
                    isClaimed = false
                ),
                MissionEntity(
                    id = "fish",
                    title = "\uD83D\uDC20 Sea Hunter Mini-Game",
                    description = "Play and catch fish in the multiplayer game",
                    rewardCoins = 200.0,
                    currentProgress = 0,
                    targetProgress = 1,
                    isCompleted = false,
                    isClaimed = false
                )
            )
            appDao.insertMissions(defaultMissions)

            // Populate initial financial logs
            val defaultTransactions = listOf(
                TransactionEntity(
                    type = "DEPOSIT",
                    amountCoins = 1200.0,
                    description = "Welcome bonus credited on account creation"
                )
            )
            for (tx in defaultTransactions) {
                appDao.insertTransaction(tx)
            }
        }
    }

    suspend fun updateUsername(newName: String) {
        val stats = appDao.getUserStatsSync() ?: return
        appDao.insertUserStats(stats.copy(username = newName))
    }

    // Spend or earn coins
    suspend fun adjustCoins(amount: Double, type: String, reason: String): Boolean {
        val stats = appDao.getUserStatsSync() ?: return false
        val newBalance = stats.coins + amount
        if (newBalance < 0) {
            return false // Insufficient funds
        }

        // Add XP reward for transactions (e.g. sending a gift earns XP!)
        val xpBonus = if (amount < 0) kotlin.math.abs(amount).toInt() / 2 else 0
        var newXp = stats.xp + xpBonus
        var newLevel = stats.level
        var xpNeeded = newLevel * 100

        while (newXp >= xpNeeded) {
            newXp -= xpNeeded
            newLevel++
            xpNeeded = newLevel * 100
        }

        // Compute VIP Level based on lifetime earned/spent coins indirectly or level
        val newVip = when {
            newLevel >= 15 -> 5
            newLevel >= 10 -> 4
            newLevel >= 7 -> 3
            newLevel >= 4 -> 2
            newLevel >= 2 -> 1
            else -> 0
        }

        val updatedStats = stats.copy(
            coins = newBalance,
            xp = newXp,
            level = newLevel,
            vipLevel = newVip
        )
        appDao.insertUserStats(updatedStats)

        // Record Ledger history
        appDao.insertTransaction(
            TransactionEntity(
                type = type,
                amountCoins = amount,
                description = reason
            )
        )
        return true
    }

    suspend fun startOwnStream(title: String, category: String, username: String): Long {
        val stats = appDao.getUserStatsSync()
        if (stats != null) {
            appDao.insertUserStats(stats.copy(streamsCreated = stats.streamsCreated + 1))
        }
        val newStream = StreamEntity(
            title = title,
            category = category,
            hostUsername = username,
            isLive = true,
            initialViewers = 0,
            currentViewers = 0,
            likesCount = 0
        )
        return appDao.insertStream(newStream)
    }

    suspend fun endStream(streamId: Int, giftCoinsEarned: Double) {
        val stream = appDao.getStreamById(streamId) ?: return
        appDao.updateStream(stream.copy(isLive = false, currentViewers = 0, giftCoinsReceived = giftCoinsEarned))
        if (giftCoinsEarned > 0) {
            adjustCoins(giftCoinsEarned, "GIFT_RECEIVED", "Earned from viewers during live stream: ${stream.title}")
        }
    }

    suspend fun deleteStream(streamId: Int) {
        appDao.deleteStreamById(streamId)
    }

    suspend fun registerStreamAction(streamId: Int, likesToAdd: Int, viewersCount: Int) {
        val stream = appDao.getStreamById(streamId) ?: return
        appDao.updateStream(stream.copy(
            likesCount = stream.likesCount + likesToAdd,
            currentViewers = viewersCount
        ))
    }

    suspend fun claimMissionReward(missionId: String): Boolean {
        val mission = appDao.getMissionById(missionId) ?: return false
        if (mission.isCompleted && !mission.isClaimed) {
            appDao.updateMission(mission.copy(isClaimed = true))
            adjustCoins(mission.rewardCoins, "MISSION_REWARD", "Completed Mission reward: ${mission.title}")
            return true
        }
        return false
    }

    suspend fun triggerMissionProgress(missionId: String, increment: Int = 1) {
        val mission = appDao.getMissionById(missionId) ?: return
        if (mission.isCompleted) return

        val newProgress = mission.currentProgress + increment
        val isNowCompleted = newProgress >= mission.targetProgress
        appDao.updateMission(mission.copy(
            currentProgress = kotlin.math.min(newProgress, mission.targetProgress),
            isCompleted = isNowCompleted
        ))
    }

    suspend fun enterReferralCode(code: String): Boolean {
        val stats = appDao.getUserStatsSync() ?: return false
        if (stats.referredBy.isNotEmpty()) return false // Already claimed
        if (code.trim().uppercase() == stats.myReferralCode) return false // Cannot refer yourself

        // Code matches, grant instant 150 coins referral bonus
        appDao.insertUserStats(stats.copy(referredBy = code.trim().uppercase()))
        adjustCoins(150.0, "DEPOSIT", "Referral code applied: $code (+150 Coins)")
        return true
    }

    suspend fun resetMissions() {
        appDao.resetAllProgress()
    }
}
