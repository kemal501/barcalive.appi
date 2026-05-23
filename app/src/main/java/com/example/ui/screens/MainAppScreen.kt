package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BarcaViewModel
import com.example.data.MissionEntity
import com.example.data.StreamEntity
import com.example.data.TransactionEntity
import kotlinx.coroutines.delay

@Composable
fun MainAppScreen(viewModel: BarcaViewModel) {
    var showSplash by remember { mutableStateOf(true) }

    // Read reactive flows from Room via ViewModel
    val stats by viewModel.userStats.collectAsState()
    val streams by viewModel.filteredStreams.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val missions by viewModel.missions.collectAsState()

    val currentTab = remember { mutableStateOf("Home") } // Home, Explore, GoLive, Wallet, Profile
    val showSearchBox = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2600) // Splash holds for 2.6 seconds
        showSplash = false
        viewModel.postNotification("📢 Welcome back to Barca-live! Claim your daily attendance reward.")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (showSplash) {
            SplashScreen()
        } else {
            // Main Scaffold with edge-to-edge support
            Scaffold(
                topBar = {
                    TopAppBarWidget(
                        currentTab = currentTab.value,
                        userCoins = stats?.coins ?: 0.0,
                        username = stats?.username ?: "BarcaFan_01",
                        vipLevel = stats?.vipLevel ?: 0,
                        searchQuery = viewModel.searchQuery.collectAsState().value,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        showSearch = showSearchBox.value,
                        onToggleSearch = { showSearchBox.value = !showSearchBox.value }
                    )
                },
                bottomBar = {
                    BottomNavigationBarWidget(
                        selectedTab = currentTab.value,
                        onTabSelected = { 
                            currentTab.value = it 
                            if (it != "Home") showSearchBox.value = false // reset search box outside home
                        }
                    )
                },
                contentWindowInsets = WindowInsets.safeDrawing
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (currentTab.value) {
                        "Home" -> HomeScreenContent(
                            streams = streams,
                            activeCategory = viewModel.selectedCategory.collectAsState().value,
                            onCategorySelect = { viewModel.selectCategory(it) },
                            onStreamClick = { viewModel.viewStream(it) }
                        )
                        "Explore" -> ExploreScreenContent(
                            viewModel = viewModel,
                            onStreamClick = { viewModel.viewStream(it) }
                        )
                        "GoLive" -> GoLiveScreenContent(
                            userCoins = stats?.coins ?: 0.0,
                            username = stats?.username ?: "BarcaFan_01",
                            onStartStream = { title, category ->
                                viewModel.createAndStartOwnStream(title, category)
                            }
                        )
                        "Wallet" -> WalletScreenContent(
                            viewModel = viewModel,
                            userCoins = stats?.coins ?: 0.0,
                            transactions = transactions
                        )
                        "Profile" -> ProfileScreenContent(
                            viewModel = viewModel,
                            vipLevel = stats?.vipLevel ?: 0,
                            userXp = stats?.xp ?: 0,
                            userLevel = stats?.level ?: 1,
                            username = stats?.username ?: "BarcaFan_01",
                            followers = stats?.followers ?: 0,
                            following = stats?.following ?: 0,
                            streamsCreated = stats?.streamsCreated ?: 0,
                            refferCode = stats?.myReferralCode ?: "BARCA_99",
                            referredBy = stats?.referredBy ?: "",
                            missions = missions
                        )
                    }

                    // Simulated overlay notification notifications banner
                    AlertsBannerOverlay(viewModel.uiNotifications)
                }
            }

            // Fullscreen video streaming overlay ( TikTok/Instagram style player simulation )
            viewModel.currentStream.value?.let { stream ->
                StreamPlayerOverlay(
                    stream = stream,
                    isHost = viewModel.isUserTheHost.value,
                    comments = viewModel.liveComments,
                    viewersCount = viewModel.liveViewersSim.value,
                    likesCount = viewModel.liveLikesSim.value,
                    earnedCoins = viewModel.liveGiftsSimCoins.value,
                    isMicOpen = viewModel.isMicOpen.value,
                    vocalSpeakers = viewModel.simulatedVoiceSpeakers,
                    viewModel = viewModel,
                    onCloseClick = { viewModel.leaveActiveStream() }
                )
            }
        }
    }
}

// ---------------- SPLASH SCREEN ----------------
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6750A4),
                        Color(0xFF493875),
                        Color(0xFF1D192B)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Glowing stadium ring canvas
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .border(3.dp, Color(0xFFE8DEF8), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Logo Play",
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Barca-live",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Next-Gen Offline Live Platform",
                fontSize = 14.sp,
                color = Color(0xFFE8DEF8),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            CircularProgressIndicator(
                color = Color(0xFFE8DEF8),
                strokeWidth = 3.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ---------------- TOP APP BAR ----------------
@Composable
fun TopAppBarWidget(
    currentTab: String,
    userCoins: Double,
    username: String,
    vipLevel: Int,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    showSearch: Boolean,
    onToggleSearch: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 2.dp,
        modifier = Modifier.testTag("top_app_bar")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Brand Logo & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stream,
                            contentDescription = "Stream Icon",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Barca-live",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF2E7D32), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Secure Local Simulation",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Balance Box or profile name on top corner
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentTab == "Home") {
                        IconButton(
                            onClick = onToggleSearch,
                            modifier = Modifier.testTag("search_toggle")
                        ) {
                            Icon(
                                imageVector = if (showSearch) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search icon",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // VIP Status Token Card
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF6750A4).copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF6750A4).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🪙 ${userCoins.toInt()}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6750A4)
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFB3261E), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "VIP $vipLevel",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Expandable Search Textbox
            if (showSearch && currentTab == "Home") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search by stream title or host account...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                        .testTag("search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }
    }
}

// ---------------- BOTTOM NAVIGATION BAR ----------------
@Composable
fun BottomNavigationBarWidget(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        color = Color(0xFFF3EDF7),
        tonalElevation = 8.dp,
        modifier = Modifier
            .border(width = 1.dp, color = Color(0xFFE6E0E9))
            .navigationBarsPadding() // TECHNICAL REQUIREMENT: respect gesture pillars!
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val navItems = listOf(
                Triple("Home", Icons.Default.Home, "Home"),
                Triple("Explore", Icons.Default.Explore, "Explore"),
                Triple("GoLive", Icons.Default.Add, "Go Live"),
                Triple("Wallet", Icons.Default.AccountBalanceWallet, "Wallet"),
                Triple("Profile", Icons.Default.Person, "Profile")
            )

            for (item in navItems) {
                val isSelected = selectedTab == item.first
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(item.first) }
                        .testTag("nav_tab_${item.first.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (item.first == "GoLive") {
                        // Polished Floating visual center Go Live icon
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF6750A4), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.second,
                                contentDescription = item.third,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        // Standard Material 3 item representation
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Color(0xFFE8DEF8) else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.second,
                                contentDescription = item.third,
                                tint = if (isSelected) Color(0xFF1D192B) else Color(0xFF49454F),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Text(
                        text = item.third,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF1D192B) else Color(0xFF49454F)
                    )
                }
            }
        }
    }
}

// ---------------- TAB 1: HOME (STREAMS LISTING) ----------------
@Composable
fun HomeScreenContent(
    streams: List<StreamEntity>,
    activeCategory: String,
    onCategorySelect: (String) -> Unit,
    onStreamClick: (StreamEntity) -> Unit
) {
    val categories = listOf("All", "Music", "Gaming", "Open Mic", "Chat")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        // Category chips row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(categories) { cat ->
                val isSelected = cat == activeCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFFE8DEF8) else Color.White)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color(0xFF6750A4) else Color(0xFFCAC4D0),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onCategorySelect(cat) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("category_chip_$cat"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color(0xFF1D192B) else Color(0xFF49454F)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Sliding Promo Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(115.dp)
                .testTag("hero_banner"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF6750A4), Color(0xFFB3261E))
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "\uD83C\uDFC6 STADIUM TOURNAMENT",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Barca Cup Live Stream Earning Arena!",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Broadcast daily and secure double coin rewards up to 5000 🪙!",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid label title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Featured Live Rooms",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${streams.size} channels live",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (streams.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VideocamOff,
                        contentDescription = "No Streams icon",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No broadcasting streams found",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Search another category query or Go Live to start your own stream!",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            // Cards Grid (Professional aspect 3:4 styling from rules)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(streams) { stream ->
                    StreamCard(stream = stream, onClick = { onStreamClick(stream) })
                }
            }
        }
    }
}

@Composable
fun StreamCard(stream: StreamEntity, onClick: () -> Unit) {
    // Generate distinct pseudo-random background gradient for each card based on ID
    val gradientColors = when (stream.id % 5) {
        0 -> listOf(Color(0xFFD0BCFF), Color(0xFF6750A4))
        1 -> listOf(Color(0xFFEFB8C8), Color(0xFFB3261E))
        2 -> listOf(Color(0xFFA5D6A7), Color(0xFF2E7D32))
        3 -> listOf(Color(0xFFE1BEE7), Color(0xFF8E24AA))
        else -> listOf(Color(0xFFB3E5FC), Color(0xFF0288D1))
    }

    Card(
        modifier = Modifier
            .aspectRatio(0.82f) // Custom sleek aspect ratio
            .clickable(onClick = onClick)
            .testTag("stream_card_${stream.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Visual simulated camera placeholder video backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.radialGradient(colors = gradientColors))
            )

            // Dim outline shadow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.65f))
                        )
                    )
            )

            // TOP ROW badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "LIVE" status badge
                Box(
                    modifier = Modifier
                        .background(Color(0xFFB3261E), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "LIVE",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Viewer Badge
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(Color.Green, CircleShape)
                        )
                        Text(
                            text = if (stream.currentViewers > 1000) "${String.format("%.1fk", stream.currentViewers / 1000.0)}" else "${stream.currentViewers}",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // BOTTOM INFO Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                // Category Tag
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stream.category.uppercase(),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stream.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color.White.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    Text(
                        text = "@${stream.hostUsername}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ---------------- TAB 2: EXPLORE & LEADERBOARD ----------------
@Composable
fun ExploreScreenContent(
    viewModel: BarcaViewModel,
    onStreamClick: (StreamEntity) -> Unit
) {
    var exploreSection by remember { mutableStateOf("Leaderboard") } // Leaderboard or Agent & Referrals

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Toggle Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF6750A4).copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (exploreSection == "Leaderboard") Color(0xFF6750A4) else Color.Transparent)
                    .clickable { exploreSection = "Leaderboard" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🏆 Global Leaderboards",
                    color = if (exploreSection == "Leaderboard") Color.White else Color(0xFF6750A4),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (exploreSection == "Agents") Color(0xFF6750A4) else Color.Transparent)
                    .clickable { exploreSection = "Agents" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👥 Agents & Referrals",
                    color = if (exploreSection == "Agents") Color.White else Color(0xFF6750A4),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (exploreSection == "Leaderboard") {
            var selectedLeaderboardType by remember { mutableStateOf("Hosts") } // Hosts or Givers

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { selectedLeaderboardType = "Hosts" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedLeaderboardType == "Hosts") Color(0xFFE8DEF8) else Color.White,
                        contentColor = if (selectedLeaderboardType == "Hosts") Color(0xFF1D192B) else Color(0xFF49454F)
                    ),
                    modifier = Modifier.border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(20.dp)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Top Broadcasters 🎥", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { selectedLeaderboardType = "Givers" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedLeaderboardType == "Givers") Color(0xFFE8DEF8) else Color.White,
                        contentColor = if (selectedLeaderboardType == "Givers") Color(0xFF1D192B) else Color(0xFF49454F)
                    ),
                    modifier = Modifier.border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(20.dp)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Top VIP Givers 💎", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (selectedLeaderboardType == "Hosts") "Weekly Stream Earnings Leaderboard" else "Weekly Sponsorship Leaderboard",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val activeList = if (selectedLeaderboardType == "Hosts") viewModel.leaderboardsTopHosts else viewModel.leaderboardsTopGivers

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(activeList) { index, item ->
                            val placingSymbol = when (index) {
                                0 -> "🥇"
                                1 -> "🥈"
                                2 -> "🥉"
                                else -> "⭐"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = placingSymbol,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(end = 12.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "@${item.first}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D1B20)
                                    )
                                    Text(
                                        text = item.second,
                                        fontSize = 10.sp,
                                        color = Color(0xFF49454F)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MonetizationOn,
                                        contentDescription = "Coin",
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${item.third.toInt()} coins",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF6750A4)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // AGENTS REWARDS CARD EXCHANGERS
            var refCodeState by remember { mutableStateOf("") }
            var messageByRef by remember { mutableStateOf("") }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Apply Agent Invitation Code",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6750A4)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Enter any active agency or friend's referral promo code to immediately claim 150.0 🪙 startup bonus!",
                                fontSize = 11.sp,
                                color = Color(0xFF49454F)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = refCodeState,
                                onValueChange = { refCodeState = it },
                                placeholder = { Text("E.g., BARCA_55") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("referral_input"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6750A4)
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (refCodeState.trim().isEmpty()) {
                                        messageByRef = "Please enter an agency code."
                                        return@Button
                                    }
                                    viewModel.submitReferralCode(refCodeState) { success, msg ->
                                        messageByRef = msg
                                        if (success) refCodeState = ""
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("apply_referral_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Claim Earning Reward Bonus \uD83C\uDF81", fontWeight = FontWeight.Bold)
                            }

                            if (messageByRef.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = messageByRef,
                                    fontSize = 11.sp,
                                    color = if (messageByRef.contains("Success", ignoreCase = true)) Color(0xFF2E7D32) else Color(0xFFB3261E),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                item {
                    // Agency rewards calculator explanation
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                        border = BorderStroke(1.dp, Color(0xFFE6E0E9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "How Earning Opportunities Work",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1B20)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val rules = listOf(
                                "🎁 Host Streams: Broadcast and receive high value dynamic gifts from simulated viewers. Gift tokens enter your wallet immediately.",
                                "🏁 Daily Missions: Complete the live missions listed in your profile to redeem free coins.",
                                "👫 Invite Givers: Share your unique referral promo code and earn 10% lifetime commission on simulated agency events.",
                                "🛡️ 100% Offline: Secure ledger transactions stored locally with zero internet security vulnerability."
                            )

                            for (rule in rules) {
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("⚽", fontSize = 10.sp)
                                    Text(text = rule, fontSize = 11.sp, color = Color(0xFF49454F))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- TAB 3: GO LIVE (STREAM STUDIO) ----------------
@Composable
fun GoLiveScreenContent(
    userCoins: Double,
    username: String,
    onStartStream: (String, String) -> Unit
) {
    var streamTitle by remember { mutableStateOf("Evening football discussion!") }
    var selectedCategory by remember { mutableStateOf("Music") }
    var safetyFilterEnabled by remember { mutableStateOf(true) }

    val categoriesList = listOf("Music", "Gaming", "Open Mic", "Chat")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFB3261E), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Camera",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Broadcasting Studio",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1B20)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Customize Broadcaster Title:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF49454F)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = streamTitle,
                        onValueChange = { streamTitle = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stream_title_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4)
                        ),
                        placeholder = { Text("What are you streaming about?") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Select Stream Category Tag:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF49454F)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (cat in categoriesList) {
                            val active = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) Color(0xFF6750A4) else Color.White)
                                    .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(8.dp))
                                    .clickable { selectedCategory = cat }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat,
                                    color = if (active) Color.White else Color(0xFF49454F),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🛡️ Real-Time AI Safety & Moderation",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enable automatic fake content moderation, word filters, and anti-fraud protocols.",
                                fontSize = 9.sp,
                                color = Color(0xFF49454F)
                            )
                        }
                        Switch(
                            checked = safetyFilterEnabled,
                            onCheckedChange = { safetyFilterEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF6750A4),
                                checkedTrackColor = Color(0xFFE8DEF8)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (streamTitle.trim().isNotEmpty()) {
                                onStartStream(streamTitle.trim(), selectedCategory)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("go_live_submit_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.White, CircleShape)
                            )
                            Text("GO LIVE AS @$username \uD83C\uDFA5", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE6E0E9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💡 Advice for New Broadcasters",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Barca-live simulates streaming behaviors offline. When you start broadcasting, virtual spectators will automatically join, send chat messages, and sponsor your channel with coin gifts. Make sure safety filter remains active to auto-mute spam accounts!",
                        fontSize = 11.sp,
                        color = Color(0xFF49454F),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// ---------------- TAB 4: WALLET & WITHDRAW ----------------
@Composable
fun WalletScreenContent(
    viewModel: BarcaViewModel,
    userCoins: Double,
    transactions: List<TransactionEntity>
) {
    var withdrawAmountStr by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Mobile Money") } // Mobile Money, PayPal, Bank Account
    var targetAccountAddress by remember { mutableStateOf("") }
    var logText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // VIP Purple Visa-styled Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .testTag("wallet_credit_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF6750A4), Color(0xFF1D192B))
                            )
                        )
                        .padding(20.dp)
                ) {
                    // Coin balance representation
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Barca-live Secure Wallet",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security lock symbol",
                                tint = Color.Green,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Cash balance indicator
                        Column {
                            Text(
                                text = "${userCoins.toInt()} 🪙 Coins",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Equivalent balance: \$${String.format("%.2f", userCoins / 100.0)} USD",
                                color = Color(0xFFE8DEF8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WALLET HOLDER: OFFLINE USER",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "100 🪙 = \$1.00 USD",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Conversion and withdrawal submission deck
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Secure Funds Withdrawal",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Convert coin packages to real withdraws immediately. Simulated updates record to the offline ledger list below.",
                        fontSize = 10.sp,
                        color = Color(0xFF49454F)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Specify Coin Quantity:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = withdrawAmountStr,
                        onValueChange = { withdrawAmountStr = it },
                        placeholder = { Text("E.g., 500") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("withdraw_coins_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Payment Transfer Platform:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val routes = listOf("Mobile Money", "PayPal", "Bank Card")
                        for (route in routes) {
                            val active = paymentMethod == route
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) Color(0xFF6750A4) else Color.White)
                                    .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(8.dp))
                                    .clickable { paymentMethod = route }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = route,
                                    color = if (active) Color.White else Color(0xFF49454F),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Recipient Card No/Address:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = targetAccountAddress,
                        onValueChange = { targetAccountAddress = it },
                        placeholder = { Text("E.g., +251 9... / paypal@mail / IBAN") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("withdraw_address_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val amt = withdrawAmountStr.toDoubleOrNull()
                            if (amt == null || amt <= 0) {
                                logText = "❌ Please specify a valid postive coin number."
                                return@Button
                            }
                            if (targetAccountAddress.trim().isEmpty()) {
                                logText = "❌ Please provide your payout address."
                                return@Button
                            }

                            viewModel.requestWithdraw(amt, paymentMethod, targetAccountAddress) { success, msg ->
                                logText = if (success) "✅ $msg" else "❌ $msg"
                                if (success) {
                                    withdrawAmountStr = ""
                                    targetAccountAddress = ""
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("withdraw_submit_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Initiate Secure Payout 💸", fontWeight = FontWeight.Bold)
                    }

                    if (logText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = logText,
                            fontSize = 11.sp,
                            color = if (logText.contains("✅")) Color(0xFF2E7D32) else Color(0xFFB3261E),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Ledger History label
        item {
            Text(
                text = "Wallet Ledger History",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recorded transactions on your account.",
                        fontSize = 12.sp,
                        color = Color(0xFF49454F)
                    )
                }
            }
        } else {
            items(transactions) { tx ->
                TransactionListItem(tx = tx)
            }
        }
    }
}

@Composable
fun TransactionListItem(tx: TransactionEntity) {
    val isDeduction = tx.amountCoins < 0
    val prefix = if (isDeduction) "" else "+"
    val accentColor = if (isDeduction) Color(0xFFB3261E) else Color(0xFF2E7D32)
    val circleBg = if (isDeduction) Color(0xFFFEEBEE) else Color(0xFFE8F5E9)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFE6E0E9), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(circleBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDeduction) Icons.Default.ArrowOutward else Icons.Default.ArrowDownward,
                contentDescription = "Tx symbol",
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.description,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D1B20)
            )
            Text(
                text = tx.type,
                fontSize = 8.sp,
                color = Color(0xFF49454F),
                fontWeight = FontWeight.ExtraBold
            )
        }

        Text(
            text = "$prefix${tx.amountCoins.toInt()} 🪙",
            color = accentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ---------------- TAB 5: PROFILE & MISSIONS ----------------
@Composable
fun ProfileScreenContent(
    viewModel: BarcaViewModel,
    vipLevel: Int,
    userXp: Int,
    userLevel: Int,
    username: String,
    followers: Int,
    following: Int,
    streamsCreated: Int,
    refferCode: String,
    referredBy: String,
    missions: List<MissionEntity>
) {
    var isEditingName by remember { mutableStateOf(false) }
    var inputNameState by remember { mutableStateOf(username) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User primary profile info Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // User Avatar
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFF6750A4), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = username.firstOrNull()?.uppercase() ?: "B",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Followers and Stats Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "$followers", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(text = "Followers", fontSize = 10.sp, color = Color(0xFF49454F))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "$following", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(text = "Following", fontSize = 10.sp, color = Color(0xFF49454F))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "$streamsCreated", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(text = "Broadcasts", fontSize = 10.sp, color = Color(0xFF49454F))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isEditingName) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = inputNameState,
                                onValueChange = { inputNameState = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("username_edit_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    if (inputNameState.trim().isNotEmpty()) {
                                        viewModel.updateUsername(inputNameState.trim())
                                        isEditingName = false
                                    }
                                },
                                modifier = Modifier.testTag("username_save_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Check, tint = Color(0xFF2E7D32), contentDescription = "Save")
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "@$username",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1B20)
                            )
                            IconButton(onClick = { isEditingName = true }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit name", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Level Milestones tracker
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Level Progress ($userXp / ${userLevel * 100} XP)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF6750A4), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(text = "Lv $userLevel", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = userXp.toFloat() / (userLevel * 100f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF6750A4),
                        trackColor = Color(0xFFE8DEF8)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Agency referral indicators
                    HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Your Invitation Code:", fontSize = 11.sp, color = Color(0xFF49454F))
                            Text(text = refferCode, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB3261E))
                        }
                        if (referredBy.isNotEmpty()) {
                            Text(
                                text = "Referred by Agency: $referredBy",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        } else {
                            Text(
                                text = "No Agency Linked Yet",
                                fontSize = 10.sp,
                                color = Color(0xFF49454F)
                            )
                        }
                    }
                }
            }
        }

        // Attendance / Missions Row Label
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Earning Missions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Resets 24h",
                    fontSize = 10.sp,
                    color = Color(0xFF6750A4),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Missions mapping deck
        if (missions.isEmpty()) {
            item {
                Text(text = "Loading Daily Missions...", fontSize = 12.sp, color = Color(0xFF49454F))
            }
        } else {
            items(missions) { mission ->
                MissionListItem(mission = mission, onClaim = { viewModel.claimDailyMissionReward(mission.id) })
            }
        }
    }
}

@Composable
fun MissionListItem(mission: MissionEntity, onClaim: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE6E0E9)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mission.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1B20)
                )
                Text(
                    text = mission.description,
                    fontSize = 10.sp,
                    color = Color(0xFF49454F),
                    lineHeight = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Progress: ${mission.currentProgress}/${mission.targetProgress}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE8DEF8), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "+${mission.rewardCoins.toInt()} 🪙",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D192B)
                        )
                    }
                }
            }

            // Claim action button
            if (mission.isClaimed) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF2E7D32).copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = "Claimed ✔", color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else if (mission.isCompleted) {
                Button(
                    onClick = onClaim,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("claim_btn_${mission.id}")
                ) {
                    Text("CLAIM RESULT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF49454F).copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = "Active", color = Color(0xFF49454F), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ---------------- ALERTS POP-UP TRAY OVERLAY ----------------
@Composable
fun AlertsBannerOverlay(notifications: List<String>) {
    val safeNotifications = remember(notifications.size) { notifications.toList() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 12.dp, end = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (notif in safeNotifications) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1D192B)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Alert logo", tint = Color(0xFFE8DEF8))
                        Text(
                            text = notif,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ---------------- FULLSCREEN STREAM PLAYER OVERLAY ----------------
// TikTok / Instagram inspired overlay, completely simulating HD Live streaming of Barca-live!
@Composable
fun StreamPlayerOverlay(
    stream: StreamEntity,
    isHost: Boolean,
    comments: List<Pair<String, String>>,
    viewersCount: Int,
    likesCount: Int,
    earnedCoins: Double,
    isMicOpen: Boolean,
    vocalSpeakers: List<Pair<String, Boolean>>,
    viewModel: BarcaViewModel,
    onCloseClick: () -> Unit
) {
    var chatInputValue by remember { mutableStateOf("") }
    var showGiftDrawer by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // VIDEO BACKGROUND CANVAS: Renders dynamic stadium particles/flowing vectors offline
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Simulated stadium light gradients
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1D192B), Color(0xFF110E1A)),
                    center = Offset(canvasWidth / 2f, canvasHeight / 3f),
                    radius = canvasWidth
                )
            )

            // Draw spinning soccer lines
            drawCircle(
                color = Color(0xFF6750A4).copy(alpha = 0.25f),
                center = Offset(canvasWidth / 2f, canvasHeight / 2f),
                radius = canvasWidth * 0.35f,
                style = Stroke(width = 4f)
            )

            drawLine(
                color = Color(0xFFB3261E).copy(alpha = 0.15f),
                start = Offset(0f, canvasHeight / 2f),
                end = Offset(canvasWidth, canvasHeight / 2f),
                strokeWidth = 3f
            )
        }

        // PRIMARY OVERLAY COLUMN
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .safeDrawingPadding() // Notch awareness
        ) {
            // 1. TOP STATUS HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Channel profile badge info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFF6750A4), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stream.hostUsername.first().uppercase(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            text = "@${stream.hostUsername}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isHost) "Hosting (Live Studio)" else "Watching Stream",
                            color = Color(0xFFE8DEF8),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Close & simulated coin metrics
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.Green, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$viewersCount viewers",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Leave broadcast clicker
                    IconButton(
                        onClick = onCloseClick,
                        modifier = Modifier
                            .background(Color(0xFFB3261E), CircleShape)
                            .size(34.dp)
                            .testTag("leave_stream_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Exit stream player", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. LIVE SHOW TAGS AND STATS OVERVIEW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFB3261E), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "HD 1080P", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "Category: ${stream.category}", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (isHost) {
                    Text(
                        text = "Received Coins: ${earnedCoins.toInt()} 🪙",
                        color = Color.Green,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 3. SPECIAL AUDIO/MIC RIPPLES FOR OPEN MC/VOICE CHATS
            val isVoiceRoom = stream.category.lowercase().contains("mic") || stream.category.lowercase().contains("voice")
            val safeVocalSpeakers = remember(vocalSpeakers.size) { vocalSpeakers.toList() }
            if (isVoiceRoom && safeVocalSpeakers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                // Grid of simulated mic speakers
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎙️ Global Open-Mic Voice Cabin",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Simulated Speaking...",
                                color = Color.LightGray,
                                fontSize = 8.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (speaker in safeVocalSpeakers.take(4)) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            width = if (speaker.second) 1.5.dp else 1.dp,
                                            color = if (speaker.second) Color.Green else Color.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(if (speaker.second) Color.Green else Color.Gray, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = "Mic Indicator",
                                                tint = Color.White,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = speaker.first,
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. MULTIPLAYER FISH MINI-GAME EMBED PANEL
            // Playable Fish game right inside the player for maximum recreation!
            val isGamingRoom = stream.category.lowercase().contains("game") || stream.category.lowercase().contains("gaming")
            if (isGamingRoom) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.72f)),
                    border = BorderStroke(1.dp, Color(0xFF6750A4).copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🐟 Multiplayer Sea Hunter Arena",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.VideogameAsset,
                                tint = Color(0xFFE8DEF8),
                                contentDescription = "game logo",
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        if (viewModel.isFishGameRunning.value) {
                            Text(
                                text = viewModel.fishStatusMessage.value,
                                color = Color.Green,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            MovingFishIndicator(fishPositionProvider = { viewModel.fishPositionX.value })

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.catchFish() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .testTag("catch_fish_btn")
                                ) {
                                    Text("🎯 CATCH!", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = { viewModel.exitFishGame() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("exit_fish_btn")
                                ) {
                                    Text("Quit", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Participate instantly. Entry Fee: 0 coins. Earn up to 15.0 🪙 per catch!",
                                    color = Color.LightGray,
                                    fontSize = 9.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { viewModel.startFishGame() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("start_fish_game_btn")
                                ) {
                                    Text("PLAY NOW", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 5. FLOATING SCROLLING COMMENTS WINDOW
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val safeReversedComments = remember(comments.size) { comments.toList().reversed() }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    reverseLayout = true // scroll from bottom up!
                ) {
                    items(safeReversedComments) { comment ->
                        Row(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${comment.first}:",
                                color = if (comment.first == "You") Color(0xFFD0BCFF) else Color(0xFFE8DEF8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = comment.second,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 6. BOTTOM CHAT INPUT BAR & ACTIONS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("stream_actions_layer"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Outlined Comment Field
                OutlinedTextField(
                    value = chatInputValue,
                    onValueChange = { chatInputValue = it },
                    placeholder = { Text("Comment...", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("comment_field_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFE8DEF8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (chatInputValue.trim().isNotEmpty()) {
                                    viewModel.submitDirectComment(chatInputValue)
                                    chatInputValue = ""
                                }
                            },
                            modifier = Modifier.testTag("send_comment_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "send chat message", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                )

                // High visual Like button
                IconButton(
                    onClick = { viewModel.likeCurrentStream() },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .size(42.dp)
                        .testTag("like_stream_btn")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Favorite, contentDescription = "heart like action", tint = Color(0xFFB3261E), modifier = Modifier.size(18.dp))
                        Text(text = "$likesCount", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // High Visual Gift Drawer button (Viewer Only)
                if (!isHost) {
                    IconButton(
                        onClick = { showGiftDrawer = !showGiftDrawer },
                        modifier = Modifier
                            .background(Color(0xFF6750A4), CircleShape)
                            .size(42.dp)
                            .testTag("gift_drawer_btn")
                    ) {
                        Icon(imageVector = Icons.Default.CardGiftcard, contentDescription = "Sponsor Gift Icon", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // 7. EXPANDABLE GIFT SPONSORSHIP CARD DRAWER (TikTok inspired sliding panel)
        if (showGiftDrawer && !isHost) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showGiftDrawer = false } // Dismiss when click outside
            )

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(210.dp)
                    .clickable(enabled = false, onClick = {}), // Prevent dismiss clicks
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1D192B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎁 Support Streamer - Send Gift",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showGiftDrawer = false }) {
                            Icon(imageVector = Icons.Default.Close, tint = Color.White.copy(alpha = 0.6f), contentDescription = "close")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val giftItems = listOf(
                        Triple("🌹 Rose", 5.0, "rose"),
                        Triple("⚽ Barca Jersey", 50.0, "jersey"),
                        Triple("⚡ Ballon d'Or", 250.0, "ballon"),
                        Triple("🏟️ Super Camp Nou", 500.0, "campnou")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (gift in giftItems) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.sendGiftToStreamer(gift.first, gift.second)
                                        showGiftDrawer = false // Auto-close drawer
                                    }
                                    .testTag("gift_item_btn_${gift.third}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    val emoji = when (gift.third) {
                                        "rose" -> "🌹"
                                        "jersey" -> "👕"
                                        "ballon" -> "🏆"
                                        else -> "🏟️"
                                    }
                                    Text(text = emoji, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = gift.first.replace(emoji, "").trim(),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${gift.second.toInt()} 🪙",
                                        color = Color(0xFFFFB300),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- PERFORMANCE-OPTIMIZED ANIMATED COMPONENT ----------------
// Isolates state evaluations to individual frames avoiding full layout invalidations
@Composable
fun MovingFishIndicator(fishPositionProvider: () -> Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Green alignment center ring (target)
        Box(
            modifier = Modifier
                .size(width = 32.dp, height = 24.dp)
                .background(Color.Green.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                .border(1.dp, Color.Green, RoundedCornerShape(6.dp))
        )

        // Current fish position slider positioned using cheap BiasAlignment
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.BiasAlignment(
                horizontalBias = fishPositionProvider(),
                verticalBias = 0.0f
            )
        ) {
            // Fish visual element
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(0xFFFFB300), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🐠", fontSize = 11.sp)
            }
        }
    }
}
