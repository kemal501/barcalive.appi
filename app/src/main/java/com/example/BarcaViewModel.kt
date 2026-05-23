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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BarcaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AppRepository(db.appDao())

    private val sharedPrefs = application.getSharedPreferences("barca_live_coin_system_prefs", android.content.Context.MODE_PRIVATE)
    private val moshi = com.squareup.moshi.Moshi.Builder().build()
    private val userMapType = com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, com.example.data.CoinSystemUser::class.java)
    @Suppress("UNCHECKED_CAST")
    private val userMapAdapter = moshi.adapter<Map<String, com.example.data.CoinSystemUser>>(userMapType)
    
    private val transactionListType = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.CoinSystemTransaction::class.java)
    @Suppress("UNCHECKED_CAST")
    private val transactionListAdapter = moshi.adapter<List<com.example.data.CoinSystemTransaction>>(transactionListType)

    // --- COIN SYSTEM STATES (HTML SIMULATION) ---
    private val _coinSystemCurrentUser = MutableStateFlow<String?>(null)
    val coinSystemCurrentUser = _coinSystemCurrentUser.asStateFlow()

    private val _coinSystemUsers = MutableStateFlow<Map<String, com.example.data.CoinSystemUser>>(emptyMap())
    val coinSystemUsers = _coinSystemUsers.asStateFlow()

    private val _coinSystemTransactions = MutableStateFlow<List<com.example.data.CoinSystemTransaction>>(emptyList())
    val coinSystemTransactions = _coinSystemTransactions.asStateFlow()

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

    // --- AGENCY CENTER STATE (HTML REPLICA) ---
    private val _agencyIsAgent = MutableStateFlow(sharedPrefs.getBoolean("agency_is_agent", false))
    val agencyIsAgent = _agencyIsAgent.asStateFlow()

    private val _agencyAgentCode = MutableStateFlow(sharedPrefs.getString("agency_agent_code", "") ?: "")
    val agencyAgentCode = _agencyAgentCode.asStateFlow()

    private val _agencyJoinedCode = MutableStateFlow(sharedPrefs.getString("agency_joined_code", "") ?: "")
    val agencyJoinedCode = _agencyJoinedCode.asStateFlow()

    private val _agencyRank = MutableStateFlow(sharedPrefs.getString("agency_rank", "Silver") ?: "Silver")
    val agencyRank = _agencyRank.asStateFlow()

    private val _agencyIncome = MutableStateFlow(sharedPrefs.getFloat("agency_income", 0.0f).toDouble())
    val agencyIncome = _agencyIncome.asStateFlow()

    private val _agencyCommission = MutableStateFlow(sharedPrefs.getInt("agency_commission", 4))
    val agencyCommission = _agencyCommission.asStateFlow()

    private val _agencySubAgentsCount = MutableStateFlow(sharedPrefs.getInt("agency_sub_agents_count", 0))
    val agencySubAgentsCount = _agencySubAgentsCount.asStateFlow()

    data class AgencyHost(val uid: String, val name: String, val userId: String, val incomeUSD: Double)
    val agencyHosts = mutableStateListOf<AgencyHost>()

    init {
        // Initialize sample databases on startup asynchronously
        viewModelScope.launch {
            repository.initializeDatabaseIfEmpty()
        }
        loadCoinSystemState()
        loadAgencyHosts()
        startAgencyIncomeSimulation()
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

    // --- COIN SYSTEM BACKING METHODS ---
    private fun loadCoinSystemState() {
        try {
            val usersJson = sharedPrefs.getString("barcaUsers", "")
            if (!usersJson.isNullOrEmpty()) {
                val loadedUsers = userMapAdapter.fromJson(usersJson)
                if (loadedUsers != null) {
                    _coinSystemUsers.value = loadedUsers
                }
            }
            val txJson = sharedPrefs.getString("barcaTransactions", "")
            if (!txJson.isNullOrEmpty()) {
                val loadedTx = transactionListAdapter.fromJson(txJson)
                if (loadedTx != null) {
                    _coinSystemTransactions.value = loadedTx
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveCoinSystemState() {
        try {
            sharedPrefs.edit().apply {
                putString("barcaUsers", userMapAdapter.toJson(_coinSystemUsers.value))
                putString("barcaTransactions", transactionListAdapter.toJson(_coinSystemTransactions.value))
                apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getCurrentFormattedDate(): String {
        return try {
            val sdf = SimpleDateFormat("M/d/yyyy, h:mm:ss a", Locale.getDefault())
            sdf.format(Date())
        } catch (e: Exception) {
            "5/23/2026, 4:26:00 PM"
        }
    }

    fun loadCoinSystemUser(userId: String): Boolean {
        if (userId.trim().isEmpty()) return false
        val cleanId = userId.trim()
        val currentUsers = _coinSystemUsers.value.toMutableMap()
        if (!currentUsers.containsKey(cleanId)) {
            currentUsers[cleanId] = com.example.data.CoinSystemUser(userId = cleanId, coins = 0, diamonds = 0)
            _coinSystemUsers.value = currentUsers
        }
        _coinSystemCurrentUser.value = cleanId
        saveCoinSystemState()
        return true
    }

    fun coinSystemBuyCoins(amount: Int): Boolean {
        val currentUser = _coinSystemCurrentUser.value ?: return false
        val currentUsers = _coinSystemUsers.value.toMutableMap()
        val userObj = currentUsers[currentUser] ?: com.example.data.CoinSystemUser(userId = currentUser, coins = 0, diamonds = 0)
        currentUsers[currentUser] = userObj.copy(coins = userObj.coins + amount)
        _coinSystemUsers.value = currentUsers

        val newTx = com.example.data.CoinSystemTransaction(
            type = "PURCHASE",
            user = currentUser,
            amount = amount,
            date = getCurrentFormattedDate()
        )
        val currentTx = _coinSystemTransactions.value.toMutableList()
        currentTx.add(0, newTx)
        _coinSystemTransactions.value = currentTx

        saveCoinSystemState()
        return true
    }

    fun coinSystemAdminGenerateCoins(password: String, targetUser: String, amount: Int): Pair<Boolean, String> {
        val ADMIN_PASSWORD = "BARCA_ADMIN_2026"
        if (password != ADMIN_PASSWORD) {
            return Pair(false, "Invalid admin password")
        }
        val cleanTarget = targetUser.trim()
        if (cleanTarget.isEmpty() || amount <= 0) {
            return Pair(false, "Invalid input")
        }

        val currentUsers = _coinSystemUsers.value.toMutableMap()
        val userObj = currentUsers[cleanTarget] ?: com.example.data.CoinSystemUser(userId = cleanTarget, coins = 0, diamonds = 0)
        currentUsers[cleanTarget] = userObj.copy(coins = userObj.coins + amount)
        _coinSystemUsers.value = currentUsers

        val newTx = com.example.data.CoinSystemTransaction(
            type = "ADMIN_GENERATION",
            user = cleanTarget,
            amount = amount,
            date = getCurrentFormattedDate()
        )
        val currentTx = _coinSystemTransactions.value.toMutableList()
        currentTx.add(0, newTx)
        _coinSystemTransactions.value = currentTx

        saveCoinSystemState()
        return Pair(true, "$amount coins generated for $cleanTarget")
    }

    fun coinSystemSendGift(receiverId: String, amount: Int): Pair<Boolean, String> {
        val currentUser = _coinSystemCurrentUser.value ?: return Pair(false, "Load user first")
        val cleanReceiver = receiverId.trim()
        if (cleanReceiver.isEmpty() || amount <= 0) {
            return Pair(false, "Invalid gift")
        }

        val currentUsers = _coinSystemUsers.value.toMutableMap()
        val senderObj = currentUsers[currentUser] ?: com.example.data.CoinSystemUser(userId = currentUser, coins = 0, diamonds = 0)
        if (senderObj.coins < amount) {
            return Pair(false, "Not enough coins")
        }

        val receiverObj = currentUsers[cleanReceiver] ?: com.example.data.CoinSystemUser(userId = cleanReceiver, coins = 0, diamonds = 0)

        // Deduct from sender
        currentUsers[currentUser] = senderObj.copy(coins = senderObj.coins - amount)

        // Convert to diamonds for host
        val diamondsEarned = kotlin.math.floor(amount * 0.6).toInt()
        currentUsers[cleanReceiver] = receiverObj.copy(diamonds = receiverObj.diamonds + diamondsEarned)

        _coinSystemUsers.value = currentUsers

        val newTx = com.example.data.CoinSystemTransaction(
            type = "GIFT",
            user = currentUser,
            receiver = cleanReceiver,
            amount = amount,
            diamonds = diamondsEarned,
            date = getCurrentFormattedDate()
        )
        val currentTx = _coinSystemTransactions.value.toMutableList()
        currentTx.add(0, newTx)
        _coinSystemTransactions.value = currentTx

        saveCoinSystemState()
        return Pair(true, "Gift sent successfully")
    }

    private fun loadAgencyHosts() {
        if (_agencyIsAgent.value) {
            val hostsStr = sharedPrefs.getString("agency_hosts_data", "")
            if (!hostsStr.isNullOrEmpty()) {
                try {
                    val elements = hostsStr.split("||")
                    for (el in elements) {
                        if (el.isNotEmpty()) {
                            val parts = el.split(",")
                            if (parts.size == 4) {
                                agencyHosts.add(AgencyHost(parts[0], parts[1], parts[2], parts[3].toDouble()))
                            }
                        }
                    }
                } catch (e: Exception) {
                    populateDefaultAgencyHosts()
                }
            } else {
                populateDefaultAgencyHosts()
            }
        }
    }

    private fun populateDefaultAgencyHosts() {
        agencyHosts.clear()
        agencyHosts.addAll(
            listOf(
                AgencyHost("h1", "CampNou_Live", "102456", 15.0),
                AgencyHost("h2", "Ansu_Fcb_Fan", "589311", 28.0),
                AgencyHost("h3", "Barca_Queen", "482012", 42.0)
            )
        )
        saveAgencyHosts()
    }

    private fun saveAgencyHosts() {
        val str = agencyHosts.joinToString("||") { "${it.uid},${it.name},${it.userId},${it.incomeUSD}" }
        sharedPrefs.edit().putString("agency_hosts_data", str).apply()
    }

    fun becomeAgencyAgent(): String {
        if (_agencyIsAgent.value) {
            return _agencyAgentCode.value
        }
        val allowedChars = ('A'..'Z') + ('0'..'9')
        val code = (1..4).map { allowedChars.random() }.joinToString("")
        
        _agencyIsAgent.value = true
        _agencyAgentCode.value = code
        _agencyRank.value = "Silver"
        _agencyCommission.value = 4
        
        sharedPrefs.edit().apply {
            putBoolean("agency_is_agent", true)
            putString("agency_agent_code", code)
            putString("agency_rank", "Silver")
            putInt("agency_commission", 4)
            apply()
        }
        
        populateDefaultAgencyHosts()
        postNotification("🎉 You are now a Barca-live Certified Agent! Code: $code")
        return code
    }

    fun joinAgencyByCode(code: String): Boolean {
        if (code.trim().isEmpty()) return false
        _agencyJoinedCode.value = code.trim().uppercase()
        sharedPrefs.edit().putString("agency_joined_code", code.trim().uppercase()).apply()
        postNotification("🏢 Successfully joined Agency: ${code.trim().uppercase()}!")
        return true
    }

    fun withdrawAgencyIncome(amount: Double): Pair<Boolean, String> {
        val currentIncome = _agencyIncome.value
        if (amount <= 0) {
            return Pair(false, "Please enter a valid amount")
        }
        if (currentIncome < amount) {
            return Pair(false, "Insufficient agency income balance. Current: $$currentIncome")
        }
        val updated = currentIncome - amount
        _agencyIncome.value = updated
        sharedPrefs.edit().putFloat("agency_income", updated.toFloat()).apply()
        postNotification("💸 Withdraw request of $$amount submitted successfully!")
        return Pair(true, "Withdraw request of $$amount submitted. Status: Pending.")
    }

    fun removeAgencyHost(uid: String) {
        agencyHosts.removeAll { it.uid == uid }
        saveAgencyHosts()
        recomputeAgencyRankAndCommission()
        postNotification("❌ Host removed from your agency team.")
    }

    fun addAgencyHostInteractively(name: String, userId: String) {
        val uid = "h_" + System.currentTimeMillis()
        agencyHosts.add(AgencyHost(uid, name, userId, 0.0))
        saveAgencyHosts()
        recomputeAgencyRankAndCommission()
        postNotification("➕ Added host $name to your agency!")
    }

    private fun recomputeAgencyRankAndCommission() {
        val count = agencyHosts.size
        val nextRank = when {
            count >= 10 -> "Diamond"
            count >= 5 -> "Gold"
            else -> "Silver"
        }
        val nextComm = when (nextRank) {
            "Diamond" -> 12
            "Gold" -> 8
            else -> 4
        }
        _agencyRank.value = nextRank
        _agencyCommission.value = nextComm
        sharedPrefs.edit().apply {
            putString("agency_rank", nextRank)
            putInt("agency_commission", nextComm)
            apply()
        }
    }

    private var agencySimulationJob: Job? = null
    
    private fun startAgencyIncomeSimulation() {
        agencySimulationJob?.cancel()
        agencySimulationJob = viewModelScope.launch {
            while (true) {
                delay(12000) // update every 12 seconds
                if (_agencyIsAgent.value) {
                    val bonus = (1..5).random().toDouble()
                    val updatedIncome = _agencyIncome.value + bonus
                    _agencyIncome.value = updatedIncome
                    sharedPrefs.edit().putFloat("agency_income", updatedIncome.toFloat()).apply()
                    
                    // Increment each host's accumulated income randomly
                    for (i in agencyHosts.indices) {
                        val current = agencyHosts[i]
                        val addedHostInc = (2..8).random().toDouble()
                        agencyHosts[i] = current.copy(incomeUSD = current.incomeUSD + addedHostInc)
                    }
                    saveAgencyHosts()
                    
                    // Random suspicious check notification (simulates fakeDetection)
                    if (Math.random() > 0.85) {
                        postNotification("🛡️ Agency Security: System checked. All hosts running legally.")
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionSimulationJob?.cancel()
        gameLoopJob?.cancel()
        agencySimulationJob?.cancel()
    }
}
