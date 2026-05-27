package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.entity.Trip
import com.example.data.repository.ServiceItem
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

// Theme Color Constants (Vibrant Palette Theme)
val VibrantBg = Color(0xFFFDFBFF)
val VibrantTextDark = Color(0xFF1A1C1E)
val VibrantNavy = Color(0xFF001D36)
val VibrantBlue = Color(0xFF0061A4)
val VibrantIceBlue = Color(0xFFD1E4FF)
val VibrantGrayBg = Color(0xFFE1E2EC)
val VibrantLightGray = Color(0xFFF3F4F9)
val VibrantBorder = Color(0xFFC4C6D0)

data class AdaptiveColors(
    val bg: Color,
    val textDark: Color,
    val navy: Color,
    val blue: Color,
    val iceBlue: Color,
    val grayBg: Color,
    val lightGray: Color,
    val border: Color,
    val cardBg: Color
)

val LocalMileageTrackerColors = staticCompositionLocalOf {
    AdaptiveColors(
        bg = Color(0xFFFDFBFF),
        textDark = Color(0xFF1A1C1E),
        navy = Color(0xFF001D36),
        blue = Color(0xFF0061A4),
        iceBlue = Color(0xFFD1E4FF),
        grayBg = Color(0xFFE1E2EC),
        lightGray = Color(0xFFF3F4F9),
        border = Color(0xFFC4C6D0),
        cardBg = Color(0xFFFFFFFF)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MileageTrackerScreen(
    viewModel: MileageTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // State Collection
    val completedTrips by viewModel.completedTrips.collectAsState()
    val activeTrip by viewModel.activeTrip.collectAsState()
    val totalMileage by viewModel.totalMileage.collectAsState()
    val customTotalMileage by viewModel.customTotalMileage.collectAsState()
    val useMiles by viewModel.useMiles.collectAsState()
    val isAutostartEnabled by viewModel.isAutostartEnabled.collectAsState()
    val isAutoCalculationEnabled by viewModel.isAutoCalculationEnabled.collectAsState()
    
    // Live Service states
    val currentSpeedMs by viewModel.currentSpeed.collectAsState()
    val isGpsActive by viewModel.isGpsActive.collectAsState()
    val satellitesConnected by viewModel.satellitesConnected.collectAsState()
    val gpsSignalLevel by viewModel.gpsSignalLevel.collectAsState()
    val liveActiveDistance by viewModel.liveActiveDistance.collectAsState()

    val themeMode by viewModel.themeMode.collectAsState()
    val speedometerTheme by viewModel.speedometerTheme.collectAsState()
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        1 -> false
        2 -> true
        else -> systemInDark
    }

    val adaptiveColors = remember(isDark, isLandscape) {
        AdaptiveColors(
            bg = if (isDark) Color(0xFF121318) else Color(0xFFFDFBFF),
            textDark = if (isDark) Color(0xFFE1E2EC) else Color(0xFF1A1C1E),
            navy = if (isDark) Color(0xFFD1E4FF) else Color(0xFF001D36),
            blue = if (isDark) Color(0xFF4FAFFE) else Color(0xFF0061A4),
            iceBlue = if (isDark) Color(0xFF1B2230) else Color(0xFFD1E4FF),
            grayBg = if (isDark) (if (isLandscape) Color(0xFF1E2024) else Color(0xFF282A31)) else Color(0xFFE1E2EC),
            lightGray = if (isDark) Color(0xFF1E2024) else Color(0xFFF3F4F9),
            border = if (isDark) Color(0xFF383A41) else Color(0xFFC4C6D0),
            cardBg = if (isDark) Color(0xFF1E2024) else Color(0xFFFFFFFF)
        )
    }

    CompositionLocalProvider(LocalMileageTrackerColors provides adaptiveColors) {
        val colors = LocalMileageTrackerColors.current
        val VibrantBg = colors.bg
        val VibrantTextDark = colors.textDark
        val VibrantNavy = colors.navy
        val VibrantBlue = colors.blue
        val VibrantIceBlue = colors.iceBlue
        val VibrantGrayBg = colors.grayBg
        val VibrantLightGray = colors.lightGray
        val VibrantBorder = colors.border

        // Navigation selection state: 0 = Dashboard, 1 = History
        var selectedTab by remember { mutableStateOf(0) }

        // Dialog state
        var showEditOdometerDialog by remember { mutableStateOf(false) }
        var showSettingsDialog by remember { mutableStateOf(false) }
        var isEditingLayout by remember { mutableStateOf(false) }
        val cardOrder by viewModel.cardOrder.collectAsState()
        val landscapeFeaturedSide by viewModel.landscapeFeaturedSide.collectAsState()
        val landscapeFeaturedCard by viewModel.landscapeFeaturedCard.collectAsState()
        var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission Launchers
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        locationPermissionGranted = fineLocationGranted
        if (fineLocationGranted) {
            viewModel.startGpsMonitoringIfNotRunning(context)
        }
    }

    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            viewModel.startGpsMonitoringIfNotRunning(context)
        }
    }

    // Launch permission request if missing fine location
    LaunchedEffect(Unit) {
        if (!locationPermissionGranted) {
            val reqPermissions = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray()
            permissionLauncher.launch(reqPermissions)
        }
    }

    // Measurement conversion helpers
    val speedFactor = if (useMiles) 2.23694 else 3.6 // m/s to mph or km/h
    val currentSpeedNum = currentSpeedMs * speedFactor
    val speedLabel = if (useMiles) "mph" else "km/h"

    val distanceFactor = if (useMiles) 0.000621371 else 0.001 // meters to miles/km
    val displayActiveDistance = if (activeTrip != null) {
        liveActiveDistance.coerceAtLeast(activeTrip!!.distanceMeters) * distanceFactor
    } else {
        0.0
    }
    val distanceUnit = if (useMiles) "mi" else "km"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = VibrantBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(VibrantBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "App Icon",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "OdoFlow",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = VibrantNavy,
                            letterSpacing = (-0.5).sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VibrantBg
                ),
                actions = {
                    IconButton(
                        onClick = { isEditingLayout = !isEditingLayout },
                        modifier = Modifier.testTag("edit_layout_button")
                    ) {
                        Icon(
                            imageVector = if (isEditingLayout) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = "Edit Card Layout",
                            tint = if (isEditingLayout) VibrantBlue else VibrantNavy
                        )
                    }
                    IconButton(
                        onClick = { viewModel.resetAll(context) },
                        modifier = Modifier.testTag("reset_all_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Database",
                            tint = VibrantNavy
                        )
                    }
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open Settings",
                            tint = VibrantNavy
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Persistent Tab Switcher at the top below AppBar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(VibrantLightGray)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedTab == 0) VibrantBlue else Color.Transparent)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 10.dp)
                        .testTag("tab_dashboard"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Dashboard",
                            tint = if (selectedTab == 0) Color.White else VibrantTextDark.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Dashboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (selectedTab == 0) Color.White else VibrantTextDark.copy(alpha = 0.8f)
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedTab == 1) VibrantBlue else Color.Transparent)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 10.dp)
                        .testTag("tab_history"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "History",
                            tint = if (selectedTab == 1) Color.White else VibrantTextDark.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "History",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (selectedTab == 1) Color.White else VibrantTextDark.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                // TAB 0: DASHBOARD
                @Composable
                fun RenderCard(cardId: String, index: Int) {
                    when (cardId) {
                        "odometer" -> {
                            OdometerCard(
                                totalMileage = totalMileage,
                                distanceUnit = distanceUnit,
                                customTotalMileage = customTotalMileage,
                                completedTripsCount = completedTrips.size,
                                onEditClick = { showEditOdometerDialog = true },
                                index = index,
                                isEditingLayout = isEditingLayout,
                                cardOrder = cardOrder,
                                viewModel = viewModel
                            )
                        }
                        "gps" -> {
                            GpsStatusCard(
                                isGpsActive = isGpsActive,
                                satellites = satellitesConnected,
                                signalLevel = gpsSignalLevel,
                                onRefreshClick = { viewModel.refreshGps(context) },
                                index = index,
                                isEditingLayout = isEditingLayout,
                                cardOrder = cardOrder,
                                viewModel = viewModel
                            )
                        }
                        "speedometer" -> {
                            ActiveTripPanel(
                                activeTrip = activeTrip,
                                currentSpeedNum = currentSpeedNum,
                                speedLabel = speedLabel,
                                displayActiveDistance = displayActiveDistance,
                                distanceUnit = distanceUnit,
                                useMiles = useMiles,
                                speedometerTheme = speedometerTheme,
                                onActionClick = {
                                    if (activeTrip != null) {
                                        viewModel.stopTracking(context)
                                    } else {
                                        viewModel.startTracking(context)
                                    }
                                },
                                index = index,
                                isEditingLayout = isEditingLayout,
                                cardOrder = cardOrder,
                                viewModel = viewModel
                            )
                        }
                        "services" -> {
                            ServicesCard(
                                totalMileage = totalMileage,
                                distanceUnit = distanceUnit,
                                index = index,
                                isEditingLayout = isEditingLayout,
                                cardOrder = cardOrder,
                                viewModel = viewModel
                            )
                        }
                    }
                }

                if (isLandscape) {
                    val featuredColumn = @Composable {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (!locationPermissionGranted) {
                                PermissionWarningCard(onGrantClick = {
                                    val reqPermissions = mutableListOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ).apply {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            add(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }.toTypedArray()
                                    permissionLauncher.launch(reqPermissions)
                                })
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            val indexOfFeatured = cardOrder.indexOf(landscapeFeaturedCard).coerceAtLeast(0)
                            val fCardId = cardOrder.getOrNull(indexOfFeatured) ?: "speedometer"
                            RenderCard(cardId = fCardId, index = indexOfFeatured)
                        }
                    }

                    val scrollableColumn = @Composable {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            cardOrder.forEachIndexed { index, cardId ->
                                if (cardId != landscapeFeaturedCard) {
                                    RenderCard(cardId = cardId, index = index)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (landscapeFeaturedSide == "left") {
                            featuredColumn()
                            scrollableColumn()
                        } else {
                            scrollableColumn()
                            featuredColumn()
                        }
                    }
                } else {
                    // Portrait view
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Check permission warning
                        if (!locationPermissionGranted) {
                            PermissionWarningCard(onGrantClick = {
                                val reqPermissions = mutableListOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ).apply {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }.toTypedArray()
                                permissionLauncher.launch(reqPermissions)
                            })
                        }

                        // Loop through all cards in user's custom sequence
                        cardOrder.forEachIndexed { index, cardId ->
                            RenderCard(cardId = cardId, index = index)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            } else {
                // TAB 1: HISTORY/PREVIOUS TRIP LOGS TAB
                val totalCompletedDistance = completedTrips.sumOf { it.distanceMeters } * distanceFactor
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary Statistics Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = VibrantIceBlue.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "DRIVES SUMMARY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = VibrantNavy.copy(alpha = 0.6f),
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${completedTrips.size} total drives",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VibrantTextDark
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "TOTAL DISTANCE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = VibrantNavy.copy(alpha = 0.6f),
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format(Locale.getDefault(), "%.2f %s", totalCompletedDistance, distanceUnit),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = VibrantBlue
                                )
                            }
                        }
                    }

                    // Historical Drive logs list header
                    Text(
                        text = "HISTORICAL DRIVE LOGS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = VibrantNavy.copy(alpha = 0.6f),
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )

                    if (completedTrips.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(VibrantLightGray, RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "No drive logs",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No completed trips logged yet.",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("historical_logs_list"),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(completedTrips) { trip ->
                                TripLogItem(trip = trip, deleteTrip = { viewModel.deleteTrip(trip) }, useMiles = useMiles)
                            }
                        }
                    }
                }
            }
        }
    }

        // Settings Dialog Modal
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = VibrantBlue
                        )
                        Text(
                            text = "Preferences & Settings",
                            fontWeight = FontWeight.Bold,
                            color = VibrantNavy,
                            fontSize = 18.sp
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section A: Speedometer Custom Theme Selector
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "SPEEDOMETER THEME",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = VibrantNavy.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Select visual profile for the gauge sweep, divisions, and needle.",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )

                            val themesList = listOf(
                                Triple(0, "Analog Dial", "Classic dial arc with tick notches and racing needle"),
                                Triple(1, "Digital Glow", "Futuristic segment bars with neon cyan digits"),
                                Triple(2, "Retro 80s", "Warm orange-yellow neon stacks and retro gauge"),
                                Triple(3, "Minimalist", "Ultra line arc and subtle speed node indicator"),
                                Triple(4, "Sporty Redline", "Racing carbon gauge and orange warning threshold")
                            )

                            themesList.forEach { (id, title, desc) ->
                                val actsSelected = speedometerTheme == id
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (actsSelected) VibrantBlue.copy(alpha = 0.1f) else Color.Transparent)
                                        .border(
                                            width = if (actsSelected) 1.5.dp else 1.dp,
                                            color = if (actsSelected) VibrantBlue else VibrantBorder.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.setSpeedometerTheme(id) }
                                        .padding(8.dp)
                                        .testTag("speed_theme_option_$id")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        RadioButton(
                                            selected = actsSelected,
                                            onClick = { viewModel.setSpeedometerTheme(id) },
                                            colors = RadioButtonDefaults.colors(selectedColor = VibrantBlue)
                                        )
                                        Column {
                                            Text(
                                                text = title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (actsSelected) VibrantBlue else VibrantTextDark
                                            )
                                            Text(
                                                text = desc,
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Divider(color = VibrantBorder.copy(alpha = 0.3f))

                        // Section B: General preferences toggles (boots, metric, auto calculation, dark theme icon)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "GENERAL PREFERENCES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = VibrantNavy.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )

                            // 1. Autostart preference toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Allow Autostart inside boot", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VibrantTextDark)
                                    Text("Auto resume tracking on boot", fontSize = 10.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = isAutostartEnabled,
                                    onCheckedChange = { viewModel.toggleAutostart(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = VibrantBlue),
                                    modifier = Modifier.testTag("autostart_switch")
                                )
                            }

                            // 2. Auto Calculation preference toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Auto-Calculate Trips", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VibrantTextDark)
                                    Text("Start when speed > 5 km/h", fontSize = 10.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = isAutoCalculationEnabled,
                                    onCheckedChange = { viewModel.toggleAutoCalculation(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = VibrantBlue),
                                    modifier = Modifier.testTag("autocalc_switch")
                                )
                            }

                            // 3. Unit preference selection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Preferred Scale Unit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VibrantTextDark)
                                    Text("Imperial (Miles) vs Metric (Km)", fontSize = 10.sp, color = Color.Gray)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "Km", 
                                        fontSize = 12.sp, 
                                        fontWeight = if (!useMiles) FontWeight.Bold else FontWeight.Normal,
                                        color = if (!useMiles) VibrantBlue else Color.Gray,
                                        modifier = Modifier.clickable { viewModel.toggleUseMiles(false) }
                                    )
                                    Switch(
                                        checked = useMiles,
                                        onCheckedChange = { viewModel.toggleUseMiles(it) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = VibrantBlue),
                                        modifier = Modifier.testTag("unit_switch")
                                    )
                                    Text(
                                        "Mi", 
                                        fontSize = 12.sp, 
                                        fontWeight = if (useMiles) FontWeight.Bold else FontWeight.Normal,
                                        color = if (useMiles) VibrantBlue else Color.Gray,
                                        modifier = Modifier.clickable { viewModel.toggleUseMiles(true) }
                                    )
                                }
                            }

                            // 4. Dark Theme selection and toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("App Theme Mode", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VibrantTextDark)
                                    Text("Toggle Light, System or Dark visual styling", fontSize = 10.sp, color = Color.Gray)
                                }
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(VibrantBorder.copy(alpha = 0.2f))
                                        .padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    val themes = listOf("Sys", "Light", "Dark")
                                    themes.forEachIndexed { index, name ->
                                        val isSelected = themeMode == index
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) VibrantBlue else Color.Transparent)
                                                .clickable { viewModel.setThemeMode(index) }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                                .testTag("theme_button_$index"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = name,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else VibrantTextDark.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Divider(color = VibrantBorder.copy(alpha = 0.3f))

                        // Section C: Landscape Column Custom Preferences
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "LANDSCAPE COLUMN CONFIGURATION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = VibrantNavy.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )

                            // 1. Select which column side features the single card
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Featured Card Column", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VibrantTextDark)
                                    Text("Side to display the featured card (Left/Right)", fontSize = 10.sp, color = Color.Gray)
                                }
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(VibrantBorder.copy(alpha = 0.2f))
                                        .padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    val options = listOf("left" to "Left", "right" to "Right")
                                    options.forEach { (value, label) ->
                                        val isSelected = landscapeFeaturedSide == value
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) VibrantBlue else Color.Transparent)
                                                .clickable { viewModel.setLandscapeFeaturedSide(value) }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                                .testTag("featured_side_button_$value"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else VibrantTextDark.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Select which card occupies the single full-height column
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Featured Dashboard Card", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VibrantTextDark)
                                    Text("Card that occupies the single full column", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(VibrantBorder.copy(alpha = 0.15f))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val cards = listOf(
                                    "speedometer" to "Speedometer",
                                    "odometer" to "Odometer",
                                    "gps" to "GPS Status"
                                )
                                cards.forEach { (id, label) ->
                                    val isSelected = landscapeFeaturedCard == id
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) VibrantBlue else Color.Transparent)
                                            .clickable { viewModel.setLandscapeFeaturedCard(id) }
                                            .padding(vertical = 8.dp)
                                            .testTag("featured_card_button_$id"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else VibrantTextDark.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showSettingsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantBlue)
                    ) {
                        Text("Apply & Close")
                    }
                }
            )
        }

    // Custom Odometer Entry Dialog
    if (showEditOdometerDialog) {
        var mileageInput by remember { mutableStateOf(customTotalMileage.toString()) }
        
        AlertDialog(
            onDismissRequest = { showEditOdometerDialog = false },
            title = { Text("Set Custom Odometer", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Manually adjust the starting odometer base. Any driving distance will be added on top of this value.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = mileageInput,
                        onValueChange = { mileageInput = it },
                        label = { Text("Odometer baseline ($distanceUnit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_mileage_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val inputVal = mileageInput.toDoubleOrNull() ?: 0.0
                        viewModel.setCustomTotalMileage(inputVal)
                        showEditOdometerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantBlue)
                ) {
                    Text("Save baseline")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditOdometerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
}

@Composable
fun PermissionWarningCard(onGrantClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFDADA)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Location Permission Required",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF410002)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "This application uses real-time GPS coordinate logging in the background to calculate drive lengths. Please grant Location permission.",
                fontSize = 13.sp,
                color = Color(0xFF410002)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A))
            ) {
                Text("Grant Permission", color = Color.White)
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    icon: ImageVector,
    index: Int,
    isEditingLayout: Boolean,
    cardOrder: List<String>,
    viewModel: MileageTrackerViewModel,
    modifier: Modifier = Modifier,
    isGlowing: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalMileageTrackerColors.current
    var dragOffsetY by remember { mutableStateOf(0f) }
    val animatedDragOffset by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dragOffset"
    )

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val featuredCardVal by viewModel.landscapeFeaturedCard.collectAsState()
    val isFeaturedInLandscape = isLandscape && cardOrder.getOrNull(index) == featuredCardVal

    val infiniteTransition = rememberInfiniteTransition(label = "neon_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1200, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val glowColor = Color(0xFFFF3B30) // Vibrant premium neon red
    val animatedBorderModifier = if (isGlowing) {
        Modifier
            .shadow(
                elevation = (6 + (glowAlpha * 6)).dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = glowColor,
                spotColor = glowColor
            )
            .border(3.2.dp, glowColor.copy(alpha = glowAlpha * 0.4f), RoundedCornerShape(24.dp))
            .border(1.6.dp, glowColor.copy(alpha = glowAlpha), RoundedCornerShape(24.dp))
    } else {
        Modifier
            .shadow(elevation = if (dragOffsetY != 0f) 8.dp else 2.dp, shape = RoundedCornerShape(24.dp))
            .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.cardBg),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(0, animatedDragOffset.roundToInt()) }
            .then(animatedBorderModifier)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Card Icon",
                        tint = colors.blue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.navy,
                        letterSpacing = 1.2.sp
                    )
                }

                if (isEditingLayout) {
                    if (isFeaturedInLandscape) {
                        Text(
                            text = "FEATURED COLUMN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.blue,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DRAG TO REORDER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.blue
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.blue.copy(alpha = 0.1f))
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = {},
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount.y

                                                val threshold = 180f
                                                if (isLandscape) {
                                                    val isDragAction = (dragOffsetY < -threshold) || (dragOffsetY > threshold)
                                                    if (isDragAction) {
                                                        val remainingCards = cardOrder.filter { it != featuredCardVal }
                                                        if (remainingCards.size == 2) {
                                                            val idxA = cardOrder.indexOf(remainingCards[0])
                                                            val idxB = cardOrder.indexOf(remainingCards[1])
                                                            if (idxA >= 0 && idxB >= 0) {
                                                                val newOrder = cardOrder.toMutableList()
                                                                newOrder[idxA] = remainingCards[1]
                                                                newOrder[idxB] = remainingCards[0]
                                                                viewModel.setCardOrder(newOrder)
                                                                
                                                                dragOffsetY = if (dragOffsetY < 0) dragOffsetY + 320f else dragOffsetY - 320f
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    if (dragOffsetY < -threshold && index > 0) {
                                                        val newOrder = cardOrder.toMutableList()
                                                        val temp = newOrder[index]
                                                        newOrder[index] = newOrder[index - 1]
                                                        newOrder[index - 1] = temp
                                                        viewModel.setCardOrder(newOrder)
                                                        dragOffsetY += 320f
                                                    } else if (dragOffsetY > threshold && index < cardOrder.size - 1) {
                                                        val newOrder = cardOrder.toMutableList()
                                                        val temp = newOrder[index]
                                                        newOrder[index] = newOrder[index + 1]
                                                        newOrder[index + 1] = temp
                                                        viewModel.setCardOrder(newOrder)
                                                        dragOffsetY -= 320f
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                dragOffsetY = 0f
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Drag to reorder",
                                    tint = colors.blue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Divider(color = colors.border.copy(alpha = 0.15f), modifier = Modifier.padding(bottom = 12.dp))

            content()
        }
    }
}

@Composable
fun OdometerCard(
    totalMileage: Double,
    distanceUnit: String,
    customTotalMileage: Double,
    completedTripsCount: Int,
    onEditClick: () -> Unit,
    index: Int,
    isEditingLayout: Boolean,
    cardOrder: List<String>,
    viewModel: MileageTrackerViewModel
) {
    val colors = LocalMileageTrackerColors.current
    val VibrantNavy = colors.navy
    val VibrantBlue = colors.blue

    DashboardCard(
        title = "Main Odometer",
        icon = Icons.Default.LocationOn,
        index = index,
        isEditingLayout = isEditingLayout,
        cardOrder = cardOrder,
        viewModel = viewModel
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%,.1f", totalMileage),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = VibrantNavy,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = distanceUnit,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = VibrantNavy.copy(alpha = 0.8f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "BASELINE OFFSET",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = VibrantNavy.copy(alpha = 0.5f)
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f %s", customTotalMileage, distanceUnit),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = VibrantNavy
                        )
                    }
                    Column {
                        Text(
                            text = "LOGGED TRIPS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = VibrantNavy.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "$completedTripsCount total",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = VibrantNavy
                        )
                    }
                }
            }

            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .background(VibrantBlue, RoundedCornerShape(16.dp))
                    .size(48.dp)
                    .testTag("edit_odometer_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Odometer",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun GpsSignalBars(signalLevel: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 1..4) {
            val isActive = i <= signalLevel
            val barHeight = (i * 3 + 2).dp
            val barColor = if (isActive) {
                when (signalLevel) {
                    1 -> Color(0xFFEF5350) // Red
                    2 -> Color(0xFFFFB74D) // Orange
                    3 -> Color(0xFF81C784) // Green
                    else -> Color(0xFF4FC3F7) // Cyan/Blue
                }
            } else {
                Color.LightGray.copy(alpha = 0.4f)
            }
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(1.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
fun GpsStatusCard(
    isGpsActive: Boolean,
    satellites: Int,
    signalLevel: Int,
    onRefreshClick: () -> Unit,
    index: Int,
    isEditingLayout: Boolean,
    cardOrder: List<String>,
    viewModel: MileageTrackerViewModel
) {
    val colors = LocalMileageTrackerColors.current
    val VibrantTextDark = colors.textDark
    val VibrantBlue = colors.blue

    DashboardCard(
        title = "GPS Satellite Connection",
        icon = Icons.Default.LocationOn,
        index = index,
        isEditingLayout = isEditingLayout,
        cardOrder = cardOrder,
        viewModel = viewModel
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tracking Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(16.dp)) {
                    if (isGpsActive) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.Green.copy(alpha = 0.5f * pulseScale))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isGpsActive) Color(0xFF4CAF50) else Color.Red)
                    )
                }

                Text(
                    text = if (isGpsActive) "Auto-Monitoring Active" else "GPS Signal Off",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VibrantTextDark
                )
            }

            // GPS Sat Info & Refresh Action Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isGpsActive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(colors.lightGray, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        GpsSignalBars(signalLevel = signalLevel)
                        
                        Text(
                            text = "$satellites SATS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = VibrantTextDark
                        )
                    }
                }

                // Refresh Command trigger button
                IconButton(
                    onClick = onRefreshClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(VibrantBlue.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh GPS connection",
                        tint = VibrantBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveTripPanel(
    activeTrip: Trip?,
    currentSpeedNum: Double,
    speedLabel: String,
    displayActiveDistance: Double,
    distanceUnit: String,
    useMiles: Boolean,
    speedometerTheme: Int,
    modifier: Modifier = Modifier,
    onActionClick: () -> Unit,
    index: Int,
    isEditingLayout: Boolean,
    cardOrder: List<String>,
    viewModel: MileageTrackerViewModel
) {
    val colors = LocalMileageTrackerColors.current
    val VibrantTextDark = colors.textDark
    val VibrantNavy = colors.navy
    val VibrantBlue = colors.blue
    val VibrantBorder = colors.border

    DashboardCard(
        title = "Live Speedometer & Trip",
        icon = Icons.Default.PlayArrow,
        index = index,
        isEditingLayout = isEditingLayout,
        cardOrder = cardOrder,
        viewModel = viewModel,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dynamic monitoring status badge
            Box(
                modifier = Modifier
                    .background(
                        if (activeTrip != null) Color(0xFFDFF0D8) else Color(0xFFF2DEDE),
                        RoundedCornerShape(100.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (activeTrip != null) "ACTIVE RE-CALCULATED DRIVE" else "STANDBY SYSTEM",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = if (activeTrip != null) Color(0xFF3C763D) else Color(0xFFA94442),
                    letterSpacing = 1.2.sp
                )
            }

            // Massive speedometer centerstage
            Box(
                modifier = Modifier.size(210.dp), // ENLARGED: 210dp instead of 130dp! Utilize space completely!
                contentAlignment = Alignment.Center
            ) {
                SpeedometerGauge(speedVal = currentSpeedNum, useMiles = useMiles, theme = speedometerTheme)
            }

            // Clean, symmetrical metrics row beneath the massive speedometer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, VibrantBorder.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .background(colors.lightGray.copy(alpha = 0.5f))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Metric A: Trip distance
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Trip Dist icon",
                            tint = VibrantBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "TRIP DISTANCE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = VibrantNavy.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%.2f %s", displayActiveDistance, distanceUnit),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = VibrantBlue
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp)
                        .background(VibrantBorder.copy(alpha = 0.3f))
                )

                // Metric B: Speedometer digital copy
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Speedometer icon",
                            tint = VibrantTextDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "SPEED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = VibrantNavy.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f %s", currentSpeedNum, speedLabel),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = VibrantNavy
                    )
                }
            }

            // Action button filling width elegantly
            Button(
                onClick = onActionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("action_track_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeTrip != null) Color(0xFFBA1A1A) else VibrantBlue
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (activeTrip != null) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (activeTrip != null) "Stop Tracking" else "Start Tracking",
                        tint = Color.White
                    )
                    Text(
                        text = if (activeTrip != null) "STOP ACTIVE TRIP" else "START NEW TRIP",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// Dial Needle speedometer drawing component
@Composable
fun SpeedometerGauge(speedVal: Double, useMiles: Boolean, theme: Int) {
    val colors = LocalMileageTrackerColors.current
    val VibrantBg = colors.bg
    val VibrantTextDark = colors.textDark
    val VibrantNavy = colors.navy
    val VibrantBlue = colors.blue
    val VibrantIceBlue = colors.iceBlue
    val VibrantGrayBg = colors.grayBg
    val VibrantLightGray = colors.lightGray
    val VibrantBorder = colors.border

    val maxSpeed = if (useMiles) 120f else 180f
    val sweepAngle = 240f
    val startAngle = 150f
    
    // Smooth angle speed animation
    val animSpeedAngle by animateFloatAsState(
        targetValue = (speedVal.toFloat().coerceIn(0f, maxSpeed) / maxSpeed) * sweepAngle,
        animationSpec = tween(500, easing = LinearOutSlowInEasing),
        label = "speedGauge"
    )

    Box(
        modifier = Modifier.size(130.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2 - 12.dp.toPx()

            when (theme) {
                1 -> {
                    // DIGITAL GLOW THEME (Futuristic Segmented Arc)
                    val segments = 18
                    val segmentGap = 3f
                    val segmentAngleSize = (sweepAngle / segments) - segmentGap
                    for (i in 0 until segments) {
                        val segStartAngle = startAngle + i * (segmentAngleSize + segmentGap)
                        val isFilled = (i.toFloat() / segments) * sweepAngle <= animSpeedAngle
                        drawArc(
                            color = if (isFilled) Color(0xFF00E5FF) else VibrantBorder.copy(alpha = 0.15f),
                            startAngle = segStartAngle,
                            sweepAngle = segmentAngleSize,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Butt)
                        )
                    }
                }
                2 -> {
                    // RETRO 80S SYNTHWAVE (Step stack neon glow)
                    val stepsCount = 12
                    for (i in 0 until stepsCount) {
                        val stepStart = startAngle + i * (sweepAngle / stepsCount)
                        val isLit = (i.toFloat() / stepsCount) * sweepAngle <= animSpeedAngle
                        val stepColor = when {
                            !isLit -> VibrantBorder.copy(alpha = 0.15f)
                            i < stepsCount * 0.5f -> Color(0xFF00E676) // neon green
                            i < stepsCount * 0.8f -> Color(0xFFFFB300) // neon orange
                            else -> Color(0xFFFF3D00) // neon red
                        }
                        drawArc(
                            color = stepColor,
                            startAngle = stepStart + 1.5f,
                            sweepAngle = (sweepAngle / stepsCount) - 3f,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                            style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Butt)
                        )
                    }
                }
                3 -> {
                    // MINIMALIST (Ultra thin line with tiny lead dot)
                    drawArc(
                        color = VibrantBorder.copy(alpha = 0.15f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = VibrantBlue,
                        startAngle = startAngle,
                        sweepAngle = animSpeedAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    val dotAngleRad = Math.toRadians((startAngle + animSpeedAngle).toDouble())
                    val dotPoint = Offset(
                        (center.x + radius * cos(dotAngleRad)).toFloat(),
                        (center.y + radius * sin(dotAngleRad)).toFloat()
                    )
                    drawCircle(
                        color = VibrantBlue,
                        radius = 4.5.dp.toPx(),
                        center = dotPoint
                    )
                }
                4 -> {
                    // SPORTY PERFORMANCE SPEED (Tachometer carbon, warning rpm & orange sweep)
                    // Grey background dial
                    drawArc(
                        color = VibrantBorder.copy(alpha = 0.25f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Draw redline over 75% max speed
                    val redlineStartFraction = 0.75f
                    val redlineStartAngle = startAngle + redlineStartFraction * sweepAngle
                    drawArc(
                        color = Color(0xFFFF1744),
                        startAngle = redlineStartAngle,
                        sweepAngle = (1f - redlineStartFraction) * sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Square)
                    )
                    // Fill sweep line
                    val isRedzone = animSpeedAngle >= redlineStartFraction * sweepAngle
                    drawArc(
                        color = if (isRedzone) Color(0xFFFF1744) else Color(0xFFFF9100),
                        startAngle = startAngle,
                        sweepAngle = animSpeedAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Notch ticks
                    val sportyNotches = 14
                    for (i in 0..sportyNotches) {
                        val fraction = i.toFloat() / sportyNotches
                        val currentAngleDegrees = startAngle + fraction * sweepAngle
                        val angleRad = Math.toRadians(currentAngleDegrees.toDouble())
                        val isRedNotch = fraction >= redlineStartFraction
                        
                        val innerPoint = Offset(
                            (center.x + (radius - 12.dp.toPx()) * cos(angleRad)).toFloat(),
                            (center.y + (radius - 12.dp.toPx()) * sin(angleRad)).toFloat()
                        )
                        val outerPoint = Offset(
                            (center.x + (radius - 4.dp.toPx()) * cos(angleRad)).toFloat(),
                            (center.y + (radius - 4.dp.toPx()) * sin(angleRad)).toFloat()
                        )
                        drawLine(
                            color = if (isRedNotch) Color(0xFFFF1744) else VibrantNavy.copy(alpha = 0.4f),
                            start = innerPoint,
                            end = outerPoint,
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                    // Racing Needle Line pointer
                    val needleAngleRad = Math.toRadians((startAngle + animSpeedAngle).toDouble())
                    val needleEnd = Offset(
                        (center.x + (radius - 6.dp.toPx()) * cos(needleAngleRad)).toFloat(),
                        (center.y + (radius - 6.dp.toPx()) * sin(needleAngleRad)).toFloat()
                    )
                    drawLine(
                        color = Color(0xFFFF1744),
                        start = center,
                        end = needleEnd,
                        strokeWidth = 3.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = VibrantNavy, radius = 7.dp.toPx())
                    drawCircle(color = Color.White, radius = 2.5.dp.toPx())
                }
                else -> {
                    // ANALOG (Theme == 0) - Traditional fine ticks & red racing needle
                    drawArc(
                        color = VibrantBorder.copy(alpha = 0.3f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = VibrantBlue,
                        startAngle = startAngle,
                        sweepAngle = animSpeedAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                    val notchesCount = 10
                    for (i in 0..notchesCount) {
                        val fraction = i.toFloat() / notchesCount
                        val currentAngleDegrees = startAngle + fraction * sweepAngle
                        val angleRad = Math.toRadians(currentAngleDegrees.toDouble())
                        
                        val innerPoint = Offset(
                            (center.x + (radius - 12.dp.toPx()) * cos(angleRad)).toFloat(),
                            (center.y + (radius - 12.dp.toPx()) * sin(angleRad)).toFloat()
                        )
                        val outerPoint = Offset(
                            (center.x + (radius - 4.dp.toPx()) * cos(angleRad)).toFloat(),
                            (center.y + (radius - 4.dp.toPx()) * sin(angleRad)).toFloat()
                        )
                        drawLine(
                            color = VibrantNavy.copy(alpha = 0.3f),
                            start = innerPoint,
                            end = outerPoint,
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                    val needleAngleRad = Math.toRadians((startAngle + animSpeedAngle).toDouble())
                    val needleEnd = Offset(
                        (center.x + (radius - 6.dp.toPx()) * cos(needleAngleRad)).toFloat(),
                        (center.y + (radius - 6.dp.toPx()) * sin(needleAngleRad)).toFloat()
                    )
                    drawLine(
                        color = Color(0xFFBA1A1A), // Red racing needle
                        start = center,
                        end = needleEnd,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = VibrantNavy, radius = 7.dp.toPx())
                    drawCircle(color = Color.White, radius = 2.5.dp.toPx())
                }
            }
        }

        // Center typography speedometer readout setup
        val specTextColor = when (theme) {
            1 -> Color(0xFF00E5FF) // Digital Cyan
            2 -> Color(0xFFFF9100) // Retro synthwave amber
            3 -> VibrantTextDark.copy(alpha = 0.85f) // Minimalist Slate
            4 -> if (animSpeedAngle >= 0.75f * sweepAngle) Color(0xFFFF1744) else VibrantTextDark // Sporty high warning color
            else -> VibrantTextDark // Analog
        }

        val speedTextFontWeight = when (theme) {
            3 -> FontWeight.Light // Minimalist
            else -> FontWeight.Black // Others bold
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 10.dp)
        ) {
            Text(
                text = String.format(Locale.getDefault(), "%.0f", speedVal),
                fontSize = if (theme == 3) 34.sp else 32.sp,
                fontWeight = speedTextFontWeight,
                color = specTextColor,
                letterSpacing = if (theme == 3) 0.sp else (-1).sp
            )
            Text(
                text = if (useMiles) "MPH" else "KM/H",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = if (theme == 1 || theme == 2) specTextColor.copy(alpha = 0.7f) else Color.Gray,
                letterSpacing = 1.sp
            )
        }
    }
}

// Single trip item component in log
@Composable
fun TripLogItem(trip: Trip, deleteTrip: () -> Unit, useMiles: Boolean) {
    val colors = LocalMileageTrackerColors.current
    val VibrantBg = colors.bg
    val VibrantTextDark = colors.textDark
    val VibrantNavy = colors.navy
    val VibrantBlue = colors.blue
    val VibrantIceBlue = colors.iceBlue
    val VibrantGrayBg = colors.grayBg
    val VibrantLightGray = colors.lightGray
    val VibrantBorder = colors.border

    val distanceFactor = if (useMiles) 0.000621371 else 0.001
    val distanceUnit = if (useMiles) "mi" else "km"
    val displayDist = trip.distanceMeters * distanceFactor

    // Formatting date
    val sfd = remember { SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault()) }
    val formattedDate = sfd.format(Date(trip.startTime))

    // Format trip duration
    val durationText = if (trip.endTime != null) {
        val diffMs = trip.endTime - trip.startTime
        val mins = (diffMs / 60000) % 60
        val hrs = diffMs / 3600000
        if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
    } else {
        "In progress"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = VibrantLightGray),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trip_item_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (trip.isAutomatic) "🤖 Automatic Calculation" else "👤 Manual Logged Trip",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (trip.isAutomatic) VibrantBlue else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))
                
                Text(
                    text = String.format(Locale.getDefault(), "%,.2f %s", displayDist, distanceUnit),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VibrantNavy
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Duration: $durationText",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(
                onClick = deleteTrip,
                modifier = Modifier.testTag("delete_trip_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Trip Log",
                    tint = Color(0xFFBA1A1A)
                )
            }
        }
    }
}

@Composable
fun ServicesCard(
    totalMileage: Double,
    distanceUnit: String,
    index: Int,
    isEditingLayout: Boolean,
    cardOrder: List<String>,
    viewModel: MileageTrackerViewModel
) {
    val colors = LocalMileageTrackerColors.current
    val serviceItems by viewModel.serviceItems.collectAsState()
    val serviceGlowEnabled by viewModel.serviceGlowEnabled.collectAsState()

    // Estimate if any service is near (remaining is <= 500 units, or <= 10% of interval, or overdue)
    val isAnyServiceNear = serviceItems.any { item ->
        val dueOdo = item.lastServiceMileage + item.interval
        val remaining = dueOdo - totalMileage
        remaining <= 500.0 || remaining <= 0.0 || remaining <= (0.1 * item.interval)
    }

    val isGlowing = serviceGlowEnabled && isAnyServiceNear

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val selectedItemToEditState = remember { mutableStateOf<ServiceItem?>(null) }

    DashboardCard(
        title = "Services",
        icon = Icons.Default.Settings,
        index = index,
        isEditingLayout = isEditingLayout,
        cardOrder = cardOrder,
        viewModel = viewModel,
        isGlowing = isGlowing
    ) {
        // Switch for Enabling/Disabling Glowing Red Alarm Border
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.border.copy(alpha = 0.1f))
                .clickable { viewModel.setServiceGlowEnabled(!serviceGlowEnabled) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Neon Alert Glow",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.navy
                )
                Text(
                    text = "Glow border neon red when service is near/overdue",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
            Switch(
                checked = serviceGlowEnabled,
                onCheckedChange = { viewModel.setServiceGlowEnabled(it) },
                modifier = Modifier.testTag("service_glow_switch")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (serviceItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No service checkups configured.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            serviceItems.forEach { item ->
                val dueOdo = item.lastServiceMileage + item.interval
                val remaining = dueOdo - totalMileage
                val currentUsage = totalMileage - item.lastServiceMileage
                val percent = if (item.interval > 0) {
                    (currentUsage / item.interval).coerceIn(0.0, 1.0)
                } else {
                    0.0
                }
                val isOverdue = remaining <= 0.0
                val isNear = remaining <= 500.0 || remaining <= (0.1 * item.interval)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = item.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.navy
                                )
                                if (isOverdue) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Overdue",
                                        tint = Color(0xFFFF3B30),
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else if (isNear) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Near Checkup",
                                        tint = Color(0xFFFF9F0A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            val remainingStr = if (isOverdue) {
                                "Overdue by ${String.format(Locale.US, "%.1f", -remaining)} $distanceUnit"
                            } else {
                                "${String.format(Locale.US, "%.1f", remaining)} $distanceUnit remaining"
                            }
                            Text(
                                text = "Every ${String.format(Locale.US, "%.0f", item.interval)} $distanceUnit • $remainingStr",
                                fontSize = 11.sp,
                                color = if (isOverdue) Color(0xFFFF3B30) else if (isNear) Color(0xFFFF9F0A) else Color.Gray
                            )
                        }

                        // Actions
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Reset/Mark Service done
                            IconButton(
                                onClick = {
                                    val updated = serviceItems.map {
                                        if (it.id == item.id) it.copy(lastServiceMileage = totalMileage) else it
                                    }
                                    viewModel.updateServiceItems(updated)
                                },
                                modifier = Modifier.size(32.dp).testTag("perform_service_btn_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Mark service performed",
                                    tint = colors.blue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Edit service
                            IconButton(
                                onClick = {
                                    selectedItemToEditState.value = item
                                    showEditDialog = true
                                },
                                modifier = Modifier.size(32.dp).testTag("edit_service_btn_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Service",
                                    tint = colors.navy.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val progressBarColor = if (isOverdue) {
                        Color(0xFFFF3B30)
                    } else if (isNear) {
                        Color(0xFFFF9F0A)
                    } else {
                        colors.blue
                    }

                    LinearProgressIndicator(
                        progress = percent.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = progressBarColor,
                        trackColor = colors.border.copy(alpha = 0.15f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { showAddDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = colors.blue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_custom_service_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Custom Service",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Custom Service", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
        }
    }

    // dialogs
    if (showAddDialog) {
        var serviceName by remember { mutableStateOf("") }
        var serviceIntervalStr by remember { mutableStateOf("") }
        var startFromCurrentOdometer by remember { mutableStateOf(true) }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, colors.border.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "ADD SERVICE TYPE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.navy,
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = serviceName,
                        onValueChange = { serviceName = it },
                        label = { Text("Service Name (e.g., Spark Plugs)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_service_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.blue,
                            unfocusedBorderColor = colors.border
                        )
                    )

                    OutlinedTextField(
                        value = serviceIntervalStr,
                        onValueChange = { serviceIntervalStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Service Interval ($distanceUnit)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("add_service_interval_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.blue,
                            unfocusedBorderColor = colors.border
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { startFromCurrentOdometer = !startFromCurrentOdometer }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = startFromCurrentOdometer,
                            onCheckedChange = { startFromCurrentOdometer = it },
                            modifier = Modifier.testTag("add_service_offset_checkbox")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Mark as recently performed", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.navy)
                            Text("Count service mileage starting from current odometer (${String.format(Locale.US, "%.1f", totalMileage)} $distanceUnit)", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("CANCEL", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val intervalValue = serviceIntervalStr.toDoubleOrNull() ?: 0.0
                                if (serviceName.isNotBlank() && intervalValue > 0.0) {
                                    val lastOdo = if (startFromCurrentOdometer) totalMileage else 0.0
                                    val newService = ServiceItem(
                                        id = "custom_" + System.currentTimeMillis().toString(),
                                        name = serviceName,
                                        interval = intervalValue,
                                        lastServiceMileage = lastOdo
                                    )
                                    viewModel.updateServiceItems(serviceItems + newService)
                                    showAddDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.blue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("SAVE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    val editingItem = selectedItemToEditState.value
    if (showEditDialog && editingItem != null) {
        var serviceName by remember(editingItem) { mutableStateOf(editingItem.name) }
        var serviceIntervalStr by remember(editingItem) { mutableStateOf(editingItem.interval.toInt().toString()) }
        var lastServiceStr by remember(editingItem) { mutableStateOf(editingItem.lastServiceMileage.toInt().toString()) }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showEditDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, colors.border.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "EDIT SERVICE TYPE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.navy,
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = serviceName,
                        onValueChange = { serviceName = it },
                        label = { Text("Service Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_service_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.blue,
                            unfocusedBorderColor = colors.border
                        )
                    )

                    OutlinedTextField(
                        value = serviceIntervalStr,
                        onValueChange = { serviceIntervalStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Service Interval ($distanceUnit)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("edit_service_interval_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.blue,
                            unfocusedBorderColor = colors.border
                        )
                    )

                    OutlinedTextField(
                        value = lastServiceStr,
                        onValueChange = { lastServiceStr = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("Last Serviced Mileage ($distanceUnit)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("edit_service_last_odo_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.blue,
                            unfocusedBorderColor = colors.border
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.updateServiceItems(serviceItems.filter { it.id != editingItem.id })
                                showEditDialog = false
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF3B30))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DELETE", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { showEditDialog = false }) {
                                Text("CANCEL", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val intervalValue = serviceIntervalStr.toDoubleOrNull() ?: 0.0
                                    val lastServiceValue = lastServiceStr.toDoubleOrNull() ?: 0.0
                                    if (serviceName.isNotBlank() && intervalValue > 0.0) {
                                        val updated = serviceItems.map {
                                            if (it.id == editingItem.id) {
                                                it.copy(
                                                    name = serviceName,
                                                    interval = intervalValue,
                                                    lastServiceMileage = lastServiceValue
                                                )
                                            } else {
                                                it
                                            }
                                        }
                                        viewModel.updateServiceItems(updated)
                                        showEditDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.blue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("SAVE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
