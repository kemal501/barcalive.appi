package com.example

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.MissionEntity
import com.example.data.StreamEntity
import com.example.data.TransactionEntity
import com.example.data.UserStatsEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class BarcaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AppRepository(db.appDao())

    // UI exposed StateFlows
    val userStats: StateFlow<UserStatsEntity?> = repository.userStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val missions: StateFlow<List<MissionEntity>> = repository.allMissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Raw list of streams from db
    private val _dbStreams = repository.allStreams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Local filter states
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Combined filtered streams for home
    val filteredStreams: StateFlow<List<StreamEntity>> = combine(
        _dbStreams,
        _selectedCategory,
        _searchQuery
    ) { list, cat, query ->
        list.filter { stream ->
            val matchesCategory = (cat == "All" || stream.category.lowercase() == cat.lowercase())
            val matchesQuery = (query.isEmpty() || 
                    stream.title.contains(query, ignoreCase = true) || 
                    stream.hostUsername.contains(query, ignoreCase = true))
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Simulator Session Variables
    val currentStream = mutableStateOf<StreamEntity?>(null)
    val isUserTheHost = mutableStateOf(false)
    val liveComments = mutableStateListOf<Pair<String, String>>() // Pair of (username, comment_text)
    val liveViewersSim = mutableStateOf(100)
    val liveLikesSim = mutableStateOf(120)
    val liveGiftsSimCoins = mutableStateOf(0.0)
    val isMicOpen = mutableStateOf(true)

    // Voice room grid active speaking list (simulated)
    val simulatedVoiceSpeakers = mutableStateListOf<Pair<String, Boolean>>() // Pair of (Name, IsSpeaking)

    // Fish mini-game values (Multiplayer Fish Arena)
    val fishPositionX = mutableStateOf(0.0f) // -1.0 to 1.0 relative
    val fishTargetZone = mutableStateOf(0.1f) // target width center offset
    val fishSpeed = mutableStateOf(0.04f)
    val fishMovingRight = mutableStateOf(true)
    val fishGameScore = mutableStateOf(0)
    val isFishGameRunning = mutableStateOf(false)
    val fishStatusMessage = mutableStateOf("Tap Catch when the fish enters the green ring!")

    // Pop-up Notification stack
    val uiNotifications = mutableStateListOf<String>()

    // Leaderboards Data Mock (Static for UI explore depth)
    val leaderboardsTopHosts = listOf(
        Triple("messi_fan_club", "Level 48", 125000.0),
        Triple("alex_stream", "Level 42", 98400.0),
        Triple("barca_queen", "Level 38", 78200.0),
        Triple("camp_nou_live", "Level 35", 64000.0),
        Triple("game_pro_x", "Level 31", 52300.0)
    )

    val leaderboardsTopGivers = listOf(
        Triple("barcaboy_vip", "VIP 5", 250000.0),
        Triple("suarez_el_pistolero", "VIP 4", 180000.0),
        Triple("griezmann_champ", "VIP 3", 120000.0),
        Triple("fcb_patron", "VIP 3", 95000.0),
        Triple("iniesta_magician", "VIP 2", 72200.0)
    )

    private var sessionSimulationJob: Job? = null
    private var gameLoopJob: Job? = null

    init {
        // Initialize sample databases on startup asynchronously
        viewModelScope.launch {
            repository.initializeDatabaseIfEmpty()
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateUsername(newName: String) {
        viewModelScope.launch {
            repository.updateUsername(newName)
            postNotification("Username updated!")
        }
    }

    // Main interaction: Register an in-app system notification banner
    fun postNotification(message: String) {
        viewModelScope.launch {
            uiNotifications.add(message)
            delay(4000)
            if (uiNotifications.isNotEmpty()) {
                uiNotifications.removeAt(0)
            }
        }
    }

    // Wallet Withdraw simulation
    fun requestWithdraw(amountCoins: Double, paymentMethod: String, address: String, onFinished: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val stats = repository.userStats.firstOrNull() ?: userStats.value
            if (stats == null) {
                onFinished(false, "System error. Please restart.")
                return@launch
            }
            if (stats.coins < amountCoins) {
                onFinished(false, "Insufficient balance. Max withdrawable: ${stats.coins.toInt()} coins.")
                return@launch
            }
            if (amountCoins < 100) {
                onFinished(false, "Minimum withdrawal limit is 100 coins.")
                return@launch
            }

            // Perform withdrawal transaction
            val success = repository.adjustCoins(
                -amountCoins,
                "WITHDRAW",
                "Withdrawal of $${String.format("%.2f", amountCoins / 100.0)} to $paymentMethod ($address)"
            )
            if (success) {
                postNotification("💸 Withdrawal requested successfully!")
                onFinished(true, "Successfully withdrew ${amountCoins.toInt()} coins ($${String.format("%.2f", amountCoins / 100.0)} USD)")
            } else {
                onFinished(false, "Transaction failed.")
            }
        }
    }

    // Go Live (as host)
    fun createAndStartOwnStream(title: String, category: String) {
        viewModelScope.launch {
            val stats = repository.userStats.firstOrNull() ?: userStats.value
            val hostName = stats?.username ?: "BarcaFan_01"
            val newId = repository.startOwnStream(title, category, hostName)
            val freshStream = StreamEntity(
                id = newId.toInt(),
                title = title,
                category = category,
                hostUsername = hostName,
                isLive = true
            )
            isUserTheHost.value = true
            startStreamSimulation(freshStream)

            // Trigger mission progress for creating stream
            repository.triggerMissionProgress("stream", 1)
        }
    }

    // Join Stream (as viewer)
    fun viewStream(stream: StreamEntity) {
        isUserTheHost.value = false
        startStreamSimulation(stream)
    }

    // Shared stream simulator loop
    private fun startStreamSimulation(stream: StreamEntity) {
        currentStream.value = stream
        liveComments.clear()
        simulatedVoiceSpeakers.clear()
        liveLikesSim.value = stream.likesCount
        liveViewersSim.value = if (stream.initialViewers > 0) stream.initialViewers else 15
        liveGiftsSimCoins.value = 0.0

        // Populate mock voice hosts if category is Voice/Open Mic
        if (stream.category.lowercase().contains("mic") || stream.category.lowercase().contains("voice")) {
            simulatedVoiceSpeakers.addAll(listOf(
                Pair("Leo_Goat_10", false),
                Pair("Pique_HQ", false),
                Pair("Xavi_tactics", false),
                Pair("Neymar_junior", false),
                Pair("Barca_Queen", false)
            ))
        }

        liveComments.add(Pair("System", "🚨 Barca-live AI Safety guard applied. This room is filtered for spam and toxic behavior."))

        // Cancel previous flows if alive
        sessionSimulationJob?.cancel()
        sessionSimulationJob = viewModelScope.launch {
            val names = listOf(
                "ViscaBarca99", "LionelFans", "Gavi_CampNou", "Lewandowski_goal",
                "Pedri_Magic", "CampNou_Hero", "Raphinha⚡", "XaviStyle",
                "Busquets_tactics", "CruyffSpirit", "BlaugranaFever", "AraujoWall"
            )
            val footballTexts = listOf(
                "Incredible gameplay!", "Visca el Barça! ❤️💙", "Amazing music choice",
                "Best Open Mic ever! 🎤", "Let's play and win!", "Is Gavi injured?",
                "This app is so fast and smooth!", "Love from Barcelona!", "Can I request a song?",
                "Sent you a hearts! ❤️", "FC Barcelona is the best club in the world!",
                "Messi forever!", "Checking my wallet balance now", "Play some esports game!"
            )
            val gifts = listOf(
                Pair("🎁 Rose", 5.0),
                Pair("⚡ Ballon d'Or", 250.0),
                Pair("⚽ Barca Jersey", 50.0)
            )

            var secondsElapsed = 0

            while (currentStream.value != null) {
                delay(2500)
                secondsElapsed += 2

                // 1. Viewer fluctuate
                val valShift = (-8..12).random()
                liveViewersSim.value = kotlin.math.max(1, liveViewersSim.value + valShift)

                // 2. Add likes randomly
                if ((0..10).random() > 4) {
                    liveLikesSim.value += (1..5).random()
                }

                // 3. Simulated Comments
                if ((0..10).random() > 3) {
                    val user = names.random()
                    val text = footballTexts.random()
                    liveComments.add(Pair(user, text))
                    if (liveComments.size > 50) {
                        liveComments.removeAt(0)
                    }
                }

                // 4. Simulated Speaking indicator ripple for voice rooms
                if (simulatedVoiceSpeakers.isNotEmpty()) {
                    val speakerIndex = (0 until simulatedVoiceSpeakers.size).random()
                    val currentVal = simulatedVoiceSpeakers[speakerIndex]
                    simulatedVoiceSpeakers[speakerIndex] = Pair(currentVal.first, !currentVal.second)
                }

                // 5. Simulated incoming gifts (ONLY IF USER IS THE HOST)
                if (isUserTheHost.value && (0..100).random() > 80) {
                    val gift = gifts.random()
                    val donor = names.random()
                    liveComments.add(Pair("🎁 $donor", "Sent a ${gift.first}!"))
                    liveGiftsSimCoins.value += gift.second
                    postNotification("✨ Recieved ${gift.first} (+${gift.second.toInt()} coins) from $donor!")
                    // Give coins directly in the wallet! Excellent for earnings flow
                    repository.adjustCoins(gift.second, "GIFT_RECEIVED", "Received ${gift.first} during your stream")
                }

                // Post updates to DB periodically for streams if active
                if (secondsElapsed % 10 == 0) {
                    repository.registerStreamAction(stream.id, (2..8).random(), liveViewersSim.value)
                }
            }
        }
    }

    // Interaction elements within stream
    fun sendGiftToStreamer(giftType: String, cost: Double) {
        viewModelScope.launch {
            val stream = currentStream.value ?: return@launch
            if (isUserTheHost.value) {
                postNotification("❌ You cannot send gifts to your own live stream!")
                return@launch
            }

            // Deduct coins from user
            val success = repository.adjustCoins(-cost, "GIFT_SENT", "Sent $giftType to host @${stream.hostUsername}")
            if (success) {
                postNotification("🎁 Sent $giftType! (-${cost.toInt()} coins)")
                liveComments.add(Pair("You", "Sent $giftType! 💖"))
                liveLikesSim.value += 15

                // Trigger support creator daily mission
                repository.triggerMissionProgress("gift", 1)
            } else {
                postNotification("❌ Insufficient coin balance!")
            }
        }
    }

    fun submitDirectComment(text: String) {
        if (text.trim().isEmpty()) return
        liveComments.add(Pair("You", text.trim()))
        postNotification("Message broadcasted!")
    }

    fun likeCurrentStream() {
        liveLikesSim.value += 1
        liveComments.add(Pair("You", "Liked the stream! ❤️"))
    }

    // Start/Stop Fish Game mini entertainment screen inside stream
    fun startFishGame() {
        isFishGameRunning.value = true
        fishGameScore.value = 0
        fishStatusMessage.value = "Catch the fish when it reaches the center ring!"

        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (isFishGameRunning.value) {
                delay(40)
                // Move fish back and forth
                if (fishMovingRight.value) {
                    fishPositionX.value += fishSpeed.value
                    if (fishPositionX.value >= 1.0f) {
                        fishMovingRight.value = false
                    }
                } else {
                    fishPositionX.value -= fishSpeed.value
                    if (fishPositionX.value <= -1.0f) {
                        fishMovingRight.value = true
                    }
                }
            }
        }
    }

    fun catchFish() {
        if (!isFishGameRunning.value) return
        val currentX = fishPositionX.value
        // Target is around center (0.0). Let's see how close we are.
        val deviation = kotlin.math.abs(currentX)

        if (deviation < 0.22f) {
            // SUCCESS!
            fishGameScore.value += 1
            fishSpeed.value += 0.015f // Make it faster & harder!
            fishStatusMessage.value = "🎯 GREAT CATCH! Combo score: ${fishGameScore.value}"

            viewModelScope.launch {
                repository.adjustCoins(15.0, "GAME_WIN", "Caught a rare Red Guppy in Fish Game")
                repository.triggerMissionProgress("fish", 1)
            }
            postNotification("🐟 Logged a fish catch! (+15.0 coins)")
        } else {
            // MISS!
            fishStatusMessage.value = "💨 MISSED! Too early or too late! Game reset."
            fishGameScore.value = 0
            fishSpeed.value = 0.04f
        }
    }

    fun exitFishGame() {
        isFishGameRunning.value = false
        gameLoopJob?.cancel()
    }

    // Stop own stream or exit viewer stream
    fun leaveActiveStream() {
        viewModelScope.launch {
            val active = currentStream.value
            if (active != null) {
                if (isUserTheHost.value) {
                    repository.endStream(active.id, liveGiftsSimCoins.value)
                    postNotification("🎥 Broadcast saved. Check your wallet ledger!")
                }
                currentStream.value = null
                sessionSimulationJob?.cancel()
                sessionSimulationJob = null
                exitFishGame()
            }
        }
    }

    fun claimDailyMissionReward(missionId: String) {
        viewModelScope.launch {
            val rewardClaimed = repository.claimMissionReward(missionId)
            if (rewardClaimed) {
                postNotification("🏆 Claimed mission rewards successfully!")
            }
        }
    }

    fun submitReferralCode(code: String, onCompleted: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val success = repository.enterReferralCode(code)
            if (success) {
                postNotification("🎉 Referral applied successfully!")
                onCompleted(true, "Referral Code applied! Received +150 Coins bonus.")
            } else {
                onCompleted(false, "Invalid code or already applied. You cannot apply your own code.")
            }
        }
    }

    fun startDailyLoginAttendance() {
        viewModelScope.launch {
            val success = repository.claimMissionReward("login")
            if (success) {
                postNotification("📆 Daily login attendance reward claimed!")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionSimulationJob?.cancel()
        gameLoopJob?.cancel()
    }
}
