package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.TextStyle
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
    var activeSubTab by remember { mutableStateOf("Payouts") } // Payouts vs Coins
    var withdrawAmountStr by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Mobile Money") } // Mobile Money, PayPal, Bank Account
    var targetAccountAddress by remember { mutableStateOf("") }
    var logText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // TAB CAPTION HEADER ROUGH SELECTOR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(Color(0xFFF3EDF7), RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            val tabs = listOf(
                "Secure Payouts 💸" to "Payouts", 
                "Coin System 🎮" to "Coins",
                "Agency Center 🏢" to "Agency"
            )
            for ((label, tab) in tabs) {
                val active = activeSubTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) Color(0xFF6750A4) else Color.Transparent)
                        .clickable { activeSubTab = tab }
                        .padding(vertical = 10.dp)
                        .testTag("wallet_sub_tab_$tab"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (active) Color.White else Color(0xFF49454F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (activeSubTab == "Coins") {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                CoinSystemDashboard(viewModel = viewModel)
            }
        } else if (activeSubTab == "Agency") {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AgencyCenterDashboard(viewModel = viewModel)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
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
                                        text = "Equivalent balance: $${String.format("%.2f", userCoins / 100.0)} USD",
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
                                        text = "100 🪙 = $1.00 USD",
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
    val safeNotifications = notifications.toList()
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
    val context = LocalContext.current
    var chatInputValue by remember { mutableStateOf("") }
    var showGiftDrawer by remember { mutableStateOf(false) }
    var showNoticeBoard by remember { mutableStateOf(true) }
    var isBackgroundMusicPlaying by remember { mutableStateOf(false) }
    var showBagDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAdminDialog by remember { mutableStateOf(false) }
    var triggerFireworksEffect by remember { mutableStateOf(false) }
    var selectedBagItemMsg by remember { mutableStateOf<String?>(null) }
    
    // Interactive Room Seats state mapping: occupantName, avatarInitial, avatarColor
    var seatOccupants by remember {
        mutableStateOf(
            listOf(
                Triple("Barca-live Host", "H", Color(0xFFEF4444)),
                Triple("Ansu_10", "A", Color(0xFF3B82F6)),
                Triple("Matias_FC", "M", Color(0xFF10B981)),
                Triple<String?, String, Color>(null, "", Color.Transparent),
                Triple<String?, String, Color>(null, "", Color.Transparent),
                Triple<String?, String, Color>(null, "", Color.Transparent),
                Triple<String?, String, Color>(null, "", Color.Transparent),
                Triple<String?, String, Color>(null, "", Color.Transparent),
                Triple<String?, String, Color>(null, "", Color.Transparent)
            )
        )
    }

    // Set of indicators currently speaking
    var speakingSeats by remember { mutableStateOf(setOf(1)) }

    // Pulsate animation for active speakers (Translates HTML speaking class keyframe animation)
    val infiniteTransition = rememberInfiniteTransition(label = "pulsating_mic")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Firework sparkles/effects lines configuration state
    var fireworksParticles by remember { mutableStateOf<List<Offset>>(emptyList()) }
    LaunchedEffect(triggerFireworksEffect) {
        if (triggerFireworksEffect) {
            // Populate animated sparks
            fireworksParticles = List(30) {
                Offset(
                    x = (100..900).random().toFloat(),
                    y = (200..1200).random().toFloat()
                )
            }
            delay(3000)
            triggerFireworksEffect = false
            fireworksParticles = emptyList()
        }
    }

    // Periodic simulation of audience activities to mimic a real dynamic room!
    LaunchedEffect(Unit) {
        while (true) {
            delay(15000)
            // Randomly toggle speaking seats to simulate interactive discussion
            val nextSpeakers = mutableSetOf(1)
            if ((0..1).random() == 1) nextSpeakers.add(2)
            if ((0..1).random() == 1) nextSpeakers.add(3)
            speakingSeats = nextSpeakers
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0A12))
    ) {
        // --- 1. PREMIUM COZY STADIUM NIGHT GRADIENT BACKGROUND CANVAS ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            if (canvasWidth > 0f && canvasHeight > 0f) {
                // Radial camp-nou twilight dark blue / bordeaux red gradient aura
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF3A0D15), Color(0xFF0F0E1C), Color(0xFF030305)),
                        center = Offset(canvasWidth / 2f, canvasHeight / 3f),
                        radius = canvasWidth * 1.2f
                    )
                )

                // Render glowing stage spotlights reflecting on active mic sessions
                drawCircle(
                    color = Color(0xFF00FFD0).copy(alpha = 0.05f),
                    center = Offset(canvasWidth / 2f, canvasHeight * 0.35f),
                    radius = canvasWidth * 0.45f
                )

                // Animated equalizers backdrop helper
                if (isBackgroundMusicPlaying) {
                    for (i in 0..12) {
                        val barHeight = (40..160).random().toFloat()
                        val barX = (canvasWidth / 14f) * i + 30f
                        drawRect(
                            color = Color(0xFF00FFD0).copy(alpha = 0.12f),
                            topLeft = Offset(barX, canvasHeight - barHeight - 120f),
                            size = androidx.compose.ui.geometry.Size(12f, barHeight)
                        )
                    }
                }

                // Render active colorful fireworks if triggered from bottom actions list
                if (fireworksParticles.isNotEmpty()) {
                    fireworksParticles.forEach { particle ->
                        drawCircle(
                            color = listOf(Color(0xFFFF00CC), Color(0xFF3333FF), Color(0xFF00FFD0), Color(0xFFFFCC00)).random(),
                            center = particle,
                            radius = (4..12).random().toFloat()
                        )
                    }
                }
            }
        }

        // --- 2. LIVE RED CENTER BADGE ---
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp)
                .background(Color(0xFFEF4444), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .testTag("room_live_badge"),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color.White, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "🔴 LIVE",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- PRIMARY CONTENT COLUMN ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .safeDrawingPadding() // Ensures no notch overlapping
        ) {
            // --- TOP STATUS BAR (Host Info, Exit) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Host badge cards
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF6750A4), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "H",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            text = "Barca-live Host",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ID: 1086462",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 10.sp
                        )
                    }
                }

                // Top Actions Header list
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Plus Add
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .clickable {
                                android.widget.Toast.makeText(context, "Added room to shortcuts!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("➕", fontSize = 16.sp, color = Color.White)
                    }

                    // Share Link
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .clickable {
                                android.widget.Toast.makeText(context, "Room link copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔗", fontSize = 16.sp, color = Color.White)
                    }

                    // Leave/Close Stream Button
                    IconButton(
                        onClick = onCloseClick,
                        modifier = Modifier
                            .background(Color(0xFFB3261E), CircleShape)
                            .size(38.dp)
                            .testTag("leave_stream_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit streaming room",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- COIN & VIRTUAL GAME TOKEN STATS BOX ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Coins Counter
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFFFFCC00), Color(0xFFFF9900))),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🪙 25,000",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Gaming Tokens Counter
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🎮 320",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 3. THE 9-SEATS INTERACTIVE AUDIENCE GRID ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                for (row in 0..2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (col in 0..2) {
                            val seatIndex = row * 3 + col
                            val actualSeatNum = seatIndex + 1
                            val state = seatOccupants[seatIndex]
                            val isOccupied = state.first != null
                            val isSpeaking = speakingSeats.contains(actualSeatNum)

                            // Base interactive item container
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .then(
                                            if (isSpeaking) Modifier.scale(pulseScale) else Modifier
                                        )
                                        .border(
                                            width = if (isSpeaking) 3.dp else 2.dp,
                                            color = if (isSpeaking) Color(0xFF00FFD0) else Color.White.copy(alpha = 0.15f),
                                            shape = CircleShape
                                        )
                                        .background(
                                            if (isOccupied) Color.Black.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
                                            CircleShape
                                        )
                                        .clip(CircleShape)
                                        .clickable {
                                            if (isOccupied) {
                                                // Toggle Speaking status
                                                speakingSeats = if (speakingSeats.contains(actualSeatNum)) {
                                                    speakingSeats - actualSeatNum
                                                } else {
                                                    speakingSeats + actualSeatNum
                                                }
                                            } else {
                                                // Vacant mic seat click -> user occupies the mic seat!
                                                val updatedList = seatOccupants.toMutableList()
                                                updatedList[seatIndex] = Triple("You", "Y", Color(0xFF8B5CF6))
                                                seatOccupants = updatedList
                                                android.widget.Toast.makeText(context, "You stepped onto Mic seat $actualSeatNum!", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isOccupied) {
                                        // Occupant badge representation
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(state.third),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = state.second,
                                                color = Color.White,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        // Empty/Open seat indicator icon
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Empty Mic Slot",
                                            tint = Color.White.copy(alpha = 0.4f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    // Crown emoji positioning for index 1, 2, 3
                                    if (actualSeatNum <= 3) {
                                        Text(
                                            text = "👑",
                                            fontSize = 18.sp,
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .offset(y = (-4).dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Seat number label
                                Text(
                                    text = state.first ?: "$actualSeatNum",
                                    color = if (isOccupied) Color.White else Color.White.copy(alpha = 0.5f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(76.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- MULTIPLAYER FISH ARENA MINI-GAME TOGGLE ---
            if (viewModel.isFishGameRunning.value) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .testTag("fish_game_cabin"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.72f)),
                    border = BorderStroke(1.dp, Color(0xFF00FFD0).copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
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
                            IconButton(onClick = { viewModel.exitFishGame() }, modifier = Modifier.size(20.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Exit game", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = viewModel.fishStatusMessage.value,
                            color = Color(0xFF00FFD0),
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        MovingFishIndicator(fishPositionProvider = { viewModel.fishPositionX.value })
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.catchFish() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFD0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                        ) {
                            Text("🎯 CATCH FISH NOW!", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- 4. FLOATING ROOM RULES/NOTICE BOARD ---
            if (showNoticeBoard) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.65f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📋 Room Rules & Notices",
                                color = Color(0xFF00FFD0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { showNoticeBoard = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss notice board",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "1. Respect everyone in the room.\n2. No abusive language.\n3. Room admin can mute users.\n4. Enjoy Barca-live voice chatting together.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // --- 5. CHAT MESSAGES SCROLL PANEL ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                val combinedLogs = remember(comments) {
                    listOf(
                        "System" to "🔥 Welcome to Barca-live room",
                        "System" to "🎤 User joined the room"
                    ) + comments
                }.reversed()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    reverseLayout = true
                ) {
                    items(combinedLogs) { log ->
                        Row(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (log.first == "System") "📢" else "🗨️ ${log.first}:",
                                color = Color(0xFF00FFD0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = log.second,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- 6. CHAT INPUT BAR & FLOAT GIFT DISPATCH ACTION ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Outlined message typing bar
                OutlinedTextField(
                    value = chatInputValue,
                    onValueChange = { chatInputValue = it },
                    placeholder = { Text("Say hello...", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("comment_field_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00FFD0),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.3f)
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
                            Text(text = "➤", color = Color(0xFF00D5B5), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )

                // Large Premium Floating Support Gift Button (🎁)
                IconButton(
                    onClick = { showGiftDrawer = !showGiftDrawer },
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFFF00CC), Color(0xFF3333FF))),
                            CircleShape
                        )
                        .testTag("gift_drawer_btn")
                ) {
                    Text("🎁", fontSize = 22.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- 7. BOTTOM MENU NAVIGATION GRID ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(vertical = 10.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Feature 1: Mute/Unmute Microphone
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            viewModel.isMicOpen.value = !viewModel.isMicOpen.value
                            android.widget.Toast.makeText(
                                context,
                                if (viewModel.isMicOpen.value) "Microphone Open!" else "Microphone Muted!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (viewModel.isMicOpen.value) "🎙️" else "🔇",
                        fontSize = 20.sp
                    )
                    Text("Mic", color = Color.White, fontSize = 9.sp)
                }

                // Feature 2: Background Music Sound Track Simulation
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            isBackgroundMusicPlaying = !isBackgroundMusicPlaying
                            android.widget.Toast.makeText(
                                context,
                                if (isBackgroundMusicPlaying) "Playing Barca Stadium Hymn!" else "Sound Effects Stopped",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isBackgroundMusicPlaying) "🎵" else "🔇",
                        fontSize = 20.sp
                    )
                    Text("Music", color = Color.White, fontSize = 9.sp)
                }

                // Feature 3: Room Settings Configuration
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showSettingsDialog = true },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⚙️", fontSize = 20.sp)
                    Text("Settings", color = Color.White, fontSize = 9.sp)
                }

                // Feature 4: Virtual Gift Bag
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showBagDialog = true },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎒", fontSize = 20.sp)
                    Text("Bag", color = Color.White, fontSize = 9.sp)
                }

                // Feature 5: Recharge Quick Coins Top-up
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            // Instantly increment virtual balance by 1000 simulated bonus coins
                            viewModel.coinSystemBuyCoins(500)
                            android.widget.Toast.makeText(context, "Quick Recharge added 1000 Coins! 🪙", android.widget.Toast.LENGTH_SHORT).show()
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("💰", fontSize = 20.sp)
                    Text("Recharge", color = Color.White, fontSize = 9.sp)
                }

                // Feature 6: Multiplayer Sea Hunter Fish Arena activation
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (!viewModel.isFishGameRunning.value) {
                                viewModel.startFishGame()
                            } else {
                                viewModel.exitFishGame()
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎮", fontSize = 20.sp)
                    Text("Games", color = Color.White, fontSize = 9.sp)
                }

                // Feature 7: Firework Effects spark triggers
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            triggerFireworksEffect = true
                            android.widget.Toast.makeText(context, "Rising spark effects triggered! 🎉", android.widget.Toast.LENGTH_SHORT).show()
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎉", fontSize = 20.sp)
                    Text("Effects", color = Color.White, fontSize = 9.sp)
                }

                // Feature 8: Shield / Room Admin Panel
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showAdminDialog = true },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🛡️", fontSize = 20.sp)
                    Text("Admin", color = Color.White, fontSize = 9.sp)
                }
            }
        }

        // --- EXPANDABLE SUPPORT GIFT PANEL DRAWERS ---
        if (showGiftDrawer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showGiftDrawer = false } // Dismiss clicking backdrop overlay
            )

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(220.dp)
                    .clickable(enabled = false, onClick = {}),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13101B)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎁 Support Room Star - Send Gifts",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showGiftDrawer = false }) {
                            Icon(imageVector = Icons.Default.Close, tint = Color.White.copy(alpha = 0.6f), contentDescription = "dismiss")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val options = listOf(
                        Triple("🌹 Rose", 5.0, "rose"),
                        Triple("⚽ Barca Jersey", 50.0, "jersey"),
                        Triple("🏆 Ballon d'Or", 250.0, "ballon"),
                        Triple("🏟️ Super Camp Nou", 500.0, "campnou")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (item in options) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.sendGiftToStreamer(item.first, item.second)
                                        showGiftDrawer = false
                                        android.widget.Toast.makeText(context, "Sponser gift ${item.first} dispatched!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    val iconRep = when (item.third) {
                                        "rose" -> "🌹"
                                        "jersey" -> "👕"
                                        "ballon" -> "🏆"
                                        else -> "🏟️"
                                    }
                                    Text(text = iconRep, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.first.replace(iconRep, "").trim(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.second.toInt()} 🪙",
                                        color = Color(0xFFFFB300),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- INTERACTIVE DIALOGS SIMULATION ---

        // Bag / Inventory dialog
        if (showBagDialog) {
            AlertDialog(
                onDismissRequest = { showBagDialog = false },
                title = { Text("🎒 Virtuelle Bag List") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Check the live active inventory stored in your inventory bag:",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        HorizontalDivider()
                        
                        val activeInventory = listOf(
                            "🔑 Gold VIP Cabin Ticket" to "Unlocks VIP streaming levels",
                            "🎤 Premium Golden Condenser Mic" to "Vocal feedback upgrade icon",
                            "🛡️ Camp Nou Mod Badge" to "Authentic moderator tag"
                        )
                        
                        activeInventory.forEach { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.submitDirectComment("used ${item.first} in the voice cabin!")
                                        showBagDialog = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(item.first, color = Color(0xFF00FFD0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(item.second, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBagDialog = false }) {
                        Text("Close", color = Color(0xFF00FFD0))
                    }
                },
                containerColor = Color(0xFF13101B)
            )
        }

        // Settings config dialog
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("⚙️ Voice Room Settings") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Show Noticeboard Rules", color = Color.White, fontSize = 13.sp)
                            Switch(
                                checked = showNoticeBoard,
                                onCheckedChange = { showNoticeBoard = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FFD0))
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Disable Mic Entry", color = Color.White, fontSize = 13.sp)
                            var disableMicCheck by remember { mutableStateOf(false) }
                            Switch(
                                checked = disableMicCheck,
                                onCheckedChange = { disableMicCheck = it }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Done", color = Color(0xFF00FFD0))
                    }
                },
                containerColor = Color(0xFF13101B)
            )
        }

        // Admin action panel
        if (showAdminDialog) {
            AlertDialog(
                onDismissRequest = { showAdminDialog = false },
                title = { Text("🛡️ Room Admin Dashboard") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Manage current speaking limits and voice slot parameters safely:", color = Color.LightGray, fontSize = 11.sp)
                        
                        Button(
                            onClick = {
                                seatOccupants = listOf(
                                    Triple("Barca-live Host", "H", Color(0xFFEF4444)),
                                    Triple<String?, String, Color>(null, "", Color.Transparent),
                                    Triple<String?, String, Color>(null, "", Color.Transparent),
                                    Triple<String?, String, Color>(null, "", Color.Transparent),
                                    Triple<String?, String, Color>(null, "", Color.Transparent),
                                    Triple<String?, String, Color>(null, "", Color.Transparent),
                                    Triple<String?, String, Color>(null, "", Color.Transparent),
                                    Triple<String?, String, Color>(null, "", Color.Transparent),
                                    Triple<String?, String, Color>(null, "", Color.Transparent)
                                )
                                speakingSeats = setOf(1)
                                android.widget.Toast.makeText(context, "Slashes empty slot, reset cabinets!", android.widget.Toast.LENGTH_SHORT).show()
                                showAdminDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Mute & Empty All Guest Slots 🎤", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.liveViewersSim.value += 125
                                android.widget.Toast.makeText(context, "Simulated viewer base incremented by +125 listeners!", android.widget.Toast.LENGTH_SHORT).show()
                                showAdminDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFD0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Inject +125 Simulated Viewers 👥", color = Color.Black, fontSize = 11.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAdminDialog = false }) {
                        Text("Dismiss", color = Color(0xFF00FFD0))
                    }
                },
                containerColor = Color(0xFF13101B)
            )
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

// ==========================================
// 🎮 COIN SYSTEM SIMULATOR DASHBOARD 
// ==========================================
@Composable
fun CoinSystemDashboard(viewModel: BarcaViewModel) {
    val context = LocalContext.current
    
    // Read state from ViewModel
    val currentUserId by viewModel.coinSystemCurrentUser.collectAsState()
    val usersMap by viewModel.coinSystemUsers.collectAsState()
    val coinTransactions by viewModel.coinSystemTransactions.collectAsState()
    
    // Local Inputs State
    var userIdInput by remember { mutableStateOf("") }
    var selectedPackageAmount by remember { mutableStateOf(100) }
    
    // Admin Generator State
    var adminPasswordInput by remember { mutableStateOf("") }
    var adminTargetUserIdInput by remember { mutableStateOf("") }
    var adminCoinAmountInput by remember { mutableStateOf("") }
    var adminMessageState by remember { mutableStateOf<Pair<Boolean, String>?>(null) } // Pair(IsSuccess, Message)
    
    // Purchase Status State
    var purchaseMessageState by remember { mutableStateOf<String?>(null) }
    
    // Send Gift State
    var giftReceiverIdInput by remember { mutableStateOf("") }
    var giftCoinsInput by remember { mutableStateOf("") }
    var giftMessageState by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val loadedUserObj = currentUserId?.let { usersMap[it] }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFF0F172A))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. TITLE HEADER ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎮", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Barca-live Coin System",
                        color = Color(0xFF38BDF8),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Virtual coin purchase and management system.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }

        // --- 2. USER WALLET CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "👤 User Wallet",
                    color = Color(0xFF38BDF8),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("User ID", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                
                OutlinedTextField(
                    value = userIdInput,
                    onValueChange = { userIdInput = it },
                    placeholder = { Text("Enter user ID", color = Color.White.copy(alpha = 0.4f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("coin_system_user_id_input"),
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A)
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                Button(
                    onClick = {
                        if (userIdInput.trim().isEmpty()) {
                            android.widget.Toast.makeText(context, "Enter user ID", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.loadCoinSystemUser(userIdInput)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("coin_system_load_user_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Load User", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Coins Balance",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${loadedUserObj?.coins ?: 0} 🪙",
                            color = Color(0xFF22C55E),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("coin_system_balance_text")
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Diamonds Balance",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${loadedUserObj?.diamonds ?: 0} 💎",
                            color = Color(0xFFE91E63),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("coin_system_diamonds_text")
                        )
                    }
                }
                
                if (currentUserId != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Current User: $currentUserId",
                        color = Color(0xFF38BDF8).copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // --- 3. BUY COINS CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🛒 Buy Coins",
                    color = Color(0xFF38BDF8),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                val packages = listOf(
                    100 to "$1",
                    500 to "$5",
                    1000 to "$10",
                    5000 to "$50",
                    10000 to "$100"
                )
                
                var showDropdown by remember { mutableStateOf(false) }
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showDropdown = true },
                        modifier = Modifier.fillMaxWidth().testTag("coin_system_package_dropdown"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF0F172A),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$selectedPackageAmount Coins - ${packages.firstOrNull { it.first == selectedPackageAmount }?.second ?: ""}",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown indicator",
                                tint = Color(0xFF38BDF8)
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false },
                        modifier = Modifier.background(Color(0xFF1E293B)).width(280.dp)
                    ) {
                        packages.forEach { pkg ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = "${pkg.first} Coins - ${pkg.second}",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    ) 
                                },
                                onClick = {
                                    selectedPackageAmount = pkg.first
                                    showDropdown = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = {
                        if (currentUserId == null) {
                            android.widget.Toast.makeText(context, "Load user first", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            val success = viewModel.coinSystemBuyCoins(selectedPackageAmount)
                            if (success) {
                                purchaseMessageState = "Successfully purchased $selectedPackageAmount coins"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("coin_system_buy_coins_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Purchase Coins", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                purchaseMessageState?.let { msg ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = msg,
                        color = Color(0xFF22C55E),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("coin_system_purchase_msg")
                    )
                }
            }
        }

        // --- 4. ADMIN COIN GENERATOR ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(2.dp, Color(0xFFF59E0B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "👑 Admin Coin Generator",
                    color = Color(0xFFF59E0B),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                Text("Admin Password", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = adminPasswordInput,
                    onValueChange = { adminPasswordInput = it },
                    placeholder = { Text("Enter admin password", color = Color.White.copy(alpha = 0.4f)) },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("coin_system_admin_password_input"),
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF59E0B),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A)
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Target User ID", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = adminTargetUserIdInput,
                    onValueChange = { adminTargetUserIdInput = it },
                    placeholder = { Text("User ID", color = Color.White.copy(alpha = 0.4f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("coin_system_admin_target_input"),
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF59E0B),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A)
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Coin Amount", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = adminCoinAmountInput,
                    onValueChange = { adminCoinAmountInput = it },
                    placeholder = { Text("Enter coin amount", color = Color.White.copy(alpha = 0.4f)) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("coin_system_admin_amount_input"),
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF59E0B),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A)
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = {
                        val amt = adminCoinAmountInput.toIntOrNull() ?: 0
                        val res = viewModel.coinSystemAdminGenerateCoins(
                            adminPasswordInput,
                            adminTargetUserIdInput,
                            amt
                        )
                        adminMessageState = res
                        if (res.first) {
                            adminPasswordInput = ""
                            adminTargetUserIdInput = ""
                            adminCoinAmountInput = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("coin_system_admin_generate_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Generate Coins", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                adminMessageState?.let { (success, msg) ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = msg,
                        color = if (success) Color(0xFF22C55E) else Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("coin_system_admin_msg")
                    )
                }
            }
        }

        // --- 5. SEND GIFT CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🎁 Send Gift",
                    color = Color(0xFF38BDF8),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                Text("Receiver User ID", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = giftReceiverIdInput,
                    onValueChange = { giftReceiverIdInput = it },
                    placeholder = { Text("Receiver ID", color = Color.White.copy(alpha = 0.4f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("coin_system_gift_receiver_input"),
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A)
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Gift Cost", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = giftCoinsInput,
                    onValueChange = { giftCoinsInput = it },
                    placeholder = { Text("Gift coin cost", color = Color.White.copy(alpha = 0.4f)) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("coin_system_gift_amount_input"),
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A)
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = {
                        if (currentUserId == null) {
                            android.widget.Toast.makeText(context, "Load user first", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            val amt = giftCoinsInput.toIntOrNull() ?: 0
                            val res = viewModel.coinSystemSendGift(giftReceiverIdInput, amt)
                            giftMessageState = res
                            if (res.first) {
                                giftReceiverIdInput = ""
                                giftCoinsInput = ""
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("coin_system_send_gift_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Send Gift", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                giftMessageState?.let { (success, msg) ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = msg,
                        color = if (success) Color(0xFF22C55E) else Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("coin_system_gift_msg")
                    )
                }
            }
        }

        // --- 6. TRANSACTION HISTORY CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📜 Transaction History",
                    color = Color(0xFF38BDF8),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                if (coinTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No history available.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        coinTransactions.forEach { tx ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF334155))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tx.type,
                                            color = Color(0xFF38BDF8),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = tx.date,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 10.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "User: ${tx.user}",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Amount: ${tx.amount}",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                    if (!tx.receiver.isNullOrEmpty()) {
                                        Text(
                                            text = "Receiver: ${tx.receiver}",
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }
                                    if (tx.diamonds != null) {
                                        Text(
                                            text = "Diamonds: ${tx.diamonds}",
                                            color = Color(0xFFE91E63),
                                            fontSize = 12.sp,
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
}

@Composable
fun AgencyCenterDashboard(viewModel: BarcaViewModel) {
    val context = LocalContext.current
    
    // Read reactive flows from BarcaViewModel
    val isAgent by viewModel.agencyIsAgent.collectAsState()
    val agentCode by viewModel.agencyAgentCode.collectAsState()
    val joinedCode by viewModel.agencyJoinedCode.collectAsState()
    val rank by viewModel.agencyRank.collectAsState()
    val income by viewModel.agencyIncome.collectAsState()
    val commission by viewModel.agencyCommission.collectAsState()
    val subAgentsCount by viewModel.agencySubAgentsCount.collectAsState()
    val hosts = viewModel.agencyHosts
    val stats by viewModel.userStats.collectAsState()
    
    // Local text inputs
    var joinCodeInput by remember { mutableStateOf("") }
    var withdrawAmountValue by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFD8FFF4)) // Mint Green background of Agency Center!
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. HEADER TITLE ---
        Text(
            text = "Barca-live Agency Center",
            color = Color(0xFF0F172A),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            textAlign = TextAlign.Center
        )

        // --- 2. THE TOP VIP METRIC GRADIENT CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFB9FFF7), Color(0xFF9FDCFF))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Profile details
                    Text(
                        text = stats?.username ?: "Barca User",
                        color = Color(0xFF0F172A),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ID: ${stats?.myReferralCode ?: "1086462"}",
                        color = Color(0xFF1E293B),
                        fontSize = 14.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Agent Code: ${if (isAgent) agentCode else "Not Agent"}",
                            color = Color(0xFF1E293B),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFF9800), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = rank,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Buttons of copy invite actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!isAgent) {
                                    android.widget.Toast.makeText(context, "Please become an agent first!", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    val link = "https://barca-live.github.io/?host=$agentCode"
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Host Invite", link)
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "Host invite copied: $link", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D7C6)),
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier.weight(1f).testTag("agency_invite_host_btn")
                        ) {
                            Text("Invite Host", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (!isAgent) {
                                    android.widget.Toast.makeText(context, "Please become an agent first!", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    val link = "https://barca-live.github.io/?agent=$agentCode"
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Agent Invite", link)
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "Agent invite copied: $link", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF32A9FF)),
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier.weight(1f).testTag("agency_invite_agent_btn")
                        ) {
                            Text("Invite Agent", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Simulated QR Code Frame exactly representing '#qr' in style
                    if (isAgent) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(110.dp)
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Design an beautiful scan pattern / QR barcode offline visual matrix
                                val w = size.width
                                val h = size.height
                                drawRect(color = Color.Black, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.3f))
                                drawRect(color = Color.Black, topLeft = Offset(w * 0.7f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.3f))
                                drawRect(color = Color.Black, topLeft = Offset(0f, h * 0.7f), size = androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.3f))
                                
                                // Random qr squares
                                for (i in 2..8) {
                                    for (j in 2..8) {
                                        if ((i + j) % 2 == 0) {
                                            drawRect(
                                                color = Color.Black,
                                                topLeft = Offset(w * 0.1f * i, h * 0.1f * j),
                                                size = androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.08f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Text(
                            text = "Scan to Link Agency Team",
                            color = Color(0xFF1E293B),
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Grid layout representing the 4 stats boxes
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Hosts Count Box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFFC8FFF4), RoundedCornerShape(15.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text("Hosts", color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = "${hosts.size}",
                                        color = Color(0xFF0D9488),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Sub Agents Box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFFC8FFF4), RoundedCornerShape(15.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text("Sub Agents", color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$subAgentsCount",
                                            color = Color(0xFF0D9488),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        // Plus clickable adder to make it visual and high-fidelity
                                        IconButton(
                                            onClick = {
                                                if (isAgent) {
                                                    val randomSubAgentNames = listOf("Matias_FCB", "Ronaldo_Fan", "Pedri_Vibe", "Gavi_Camp")
                                                    val added = randomSubAgentNames.random()
                                                    android.widget.Toast.makeText(context, "Added subagent $added to your tree!", android.widget.Toast.LENGTH_SHORT).show()
                                                } else {
                                                    android.widget.Toast.makeText(context, "Become agent first!", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text("➕", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Income Box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFFC8FFF4), RoundedCornerShape(15.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text("Income", color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = "$${String.format("%.2f", income)}",
                                        color = Color(0xFF0D9488),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Commission Box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFFC8FFF4), RoundedCornerShape(15.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text("Commission", color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = "$commission%",
                                        color = Color(0xFF0D9488),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 3. BECOME AGENT CARD PANEL ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Become Agent",
                    color = Color(0xFF0F172A),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Open a certified agency node, invite hosts, manage users and start collecting massive lifetime commission rewards up to 12%.",
                    color = Color.DarkGray,
                    fontSize = 12.sp
                )
                Button(
                    onClick = {
                        val generatedCode = viewModel.becomeAgencyAgent()
                        android.widget.Toast.makeText(context, "Approved! Your Agent Code: $generatedCode", android.widget.Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("agency_become_agent_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D7C6)),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = if (isAgent) "AGENT ACTIVE (Code: $agentCode)" else "Generate Agent Code",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // --- 4. JOIN AGENCY CARD PANEL ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Join Agency",
                    color = Color(0xFF0F172A),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                if (joinedCode.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFCCFFEA), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Linked with Agency: $joinedCode ✅",
                            color = Color(0xFF1B5E20),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = joinCodeInput,
                        onValueChange = { joinCodeInput = it },
                        placeholder = { Text("Enter Agent Code", color = Color.Gray.copy(alpha = 0.6f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("agency_join_input"),
                        textStyle = TextStyle(color = Color(0xFF1E293B), fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                    
                    Button(
                        onClick = {
                            if (joinCodeInput.trim().isEmpty()) {
                                android.widget.Toast.makeText(context, "Please enter an agent code prefix!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                val success = viewModel.joinAgencyByCode(joinCodeInput)
                                if (success) {
                                    joinCodeInput = ""
                                    android.widget.Toast.makeText(context, "Joined successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("agency_join_submit_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF32A9FF)),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text("Join", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // --- 5. WITHDRAW CARD PANEL ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Withdraw",
                    color = Color(0xFF0F172A),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Deduct from your simulated agency balance. Commission is instantly translated to payments.",
                    color = Color.DarkGray,
                    fontSize = 12.sp
                )
                
                OutlinedTextField(
                    value = withdrawAmountValue,
                    onValueChange = { withdrawAmountValue = it },
                    placeholder = { Text("Amount ($)", color = Color.Gray.copy(alpha = 0.6f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("agency_withdraw_input"),
                    textStyle = TextStyle(color = Color(0xFF1E293B), fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                
                Button(
                    onClick = {
                        val amt = withdrawAmountValue.toDoubleOrNull()
                        if (amt == null || amt <= 0.0) {
                            android.widget.Toast.makeText(context, "Please enter a valid amount!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            val res = viewModel.withdrawAgencyIncome(amt)
                            if (res.first) {
                                withdrawAmountValue = ""
                            }
                            android.widget.Toast.makeText(context, res.second, android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("agency_withdraw_submit_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D7C6)),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text("Request Withdraw", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // --- 6. MY HOSTS PANEL ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Hosts",
                        color = Color(0xFF0F172A),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            if (isAgent) {
                                val hostNames = listOf("Lionel_M10", "Gavi_CampNou", "Pedri_Magic", "DeJong_FC")
                                val hId = (100000..999999).random().toString()
                                val selectedName = hostNames.random()
                                viewModel.addAgencyHostInteractively(selectedName, hId)
                            } else {
                                android.widget.Toast.makeText(context, "Become agent first!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(28.dp).testTag("agency_add_host_btn_plus")
                    ) {
                        Text("➕", fontSize = 14.sp)
                    }
                }
                Text(
                    text = "Linked broadcasters earning coins currently. Click '+' above to sign up new hosts to your team!",
                    color = Color.DarkGray,
                    fontSize = 11.sp
                )
                
                if (!isAgent) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Please become an agent first to view hosts list.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (hosts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No active hosts registered. Click 'Invite Host' or '+' to add one.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (host in hosts) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF5F5F5), RoundedCornerShape(15.dp))
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = host.name,
                                        color = Color(0xFF0F172A),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "User ID: ${host.userId}",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Host Income: $${String.format("%.2f", host.incomeUSD)}",
                                        color = Color(0xFF0F172A),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.removeAgencyHost(host.uid)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4F4F)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().height(32.dp).testTag("agency_remove_host_${host.uid}"),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Remove Host", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
