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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
    val liveActiveDistance by viewModel.liveActiveDistance.collectAsState()

    // Dialog state
    var showEditOdometerDialog by remember { mutableStateOf(false) }
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
                        onClick = { viewModel.resetAll(context) },
                        modifier = Modifier.testTag("reset_all_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Database",
                            tint = VibrantNavy
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Navigation / Active Service Control Card (Speedometer + Trip distance info)
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActiveTripPanel(
                        activeTrip = activeTrip,
                        currentSpeedNum = currentSpeedNum,
                        speedLabel = speedLabel,
                        displayActiveDistance = displayActiveDistance,
                        distanceUnit = distanceUnit,
                        useMiles = useMiles,
                        onActionClick = {
                            if (activeTrip != null) {
                                viewModel.stopTracking(context)
                            } else {
                                viewModel.startTracking(context)
                            }
                        }
                    )
                }

                // Right Column: Settings preferences, main odometer, historical logs
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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

                    // Total mileage custom odometer
                    OdometerCard(
                        totalMileage = totalMileage,
                        distanceUnit = distanceUnit,
                        customTotalMileage = customTotalMileage,
                        completedTripsCount = completedTrips.size,
                        onEditClick = { showEditOdometerDialog = true }
                    )

                    // GPS Status ribbon indicator
                    GpsStatusRibbon(isGpsActive = isGpsActive)

                    // Settings preferences card
                    PreferencesCard(
                        isAutostartEnabled = isAutostartEnabled,
                        isAutoCalculationEnabled = isAutoCalculationEnabled,
                        useMiles = useMiles,
                        viewModel = viewModel
                    )

                    // Historical Drive logs list
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "HISTORICAL DRIVE LOGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VibrantNavy.copy(alpha = 0.6f),
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        if (completedTrips.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(VibrantLightGray, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No completed trips logged yet.",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.testTag("historical_logs_list")
                            ) {
                                completedTrips.forEach { trip ->
                                    TripLogItem(trip = trip, deleteTrip = { viewModel.deleteTrip(trip) }, useMiles = useMiles)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else {
            // Portrait view
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
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

                // Odometer card
                OdometerCard(
                    totalMileage = totalMileage,
                    distanceUnit = distanceUnit,
                    customTotalMileage = customTotalMileage,
                    completedTripsCount = completedTrips.size,
                    onEditClick = { showEditOdometerDialog = true }
                )

                // GPS Status Ribbon
                GpsStatusRibbon(isGpsActive = isGpsActive)

                // Current Trip Log Card with Speedometer Gauge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    ActiveTripPanel(
                        activeTrip = activeTrip,
                        currentSpeedNum = currentSpeedNum,
                        speedLabel = speedLabel,
                        displayActiveDistance = displayActiveDistance,
                        distanceUnit = distanceUnit,
                        useMiles = useMiles,
                        onActionClick = {
                            if (activeTrip != null) {
                                viewModel.stopTracking(context)
                            } else {
                                viewModel.startTracking(context)
                            }
                        }
                    )
                }

                // Quick Auto Settings bar
                PreferencesCard(
                    isAutostartEnabled = isAutostartEnabled,
                    isAutoCalculationEnabled = isAutoCalculationEnabled,
                    useMiles = useMiles,
                    viewModel = viewModel
                )

                // Historical Logs segment
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.7f)
                ) {
                    Text(
                        text = "HISTORICAL DRIVE LOGS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = VibrantNavy.copy(alpha = 0.6f),
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (completedTrips.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(VibrantLightGray, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No completed trips logged yet.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("historical_logs_list"),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
fun OdometerCard(
    totalMileage: Double,
    distanceUnit: String,
    customTotalMileage: Double,
    completedTripsCount: Int,
    onEditClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(VibrantIceBlue)
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MAIN ODOMETER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = VibrantNavy.copy(alpha = 0.6f),
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
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
fun GpsStatusRibbon(isGpsActive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VibrantGrayBg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
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

        // GPS Sat Info Tag
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "GPS icon",
                tint = VibrantTextDark,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "100% SIGNAL",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = VibrantTextDark
            )
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
    onActionClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White)
            .border(1.dp, VibrantBorder, RoundedCornerShape(32.dp))
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(32.dp))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Title Label
            Box(
                modifier = Modifier
                    .background(Color(0xFFD3E4F6), RoundedCornerShape(100.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (activeTrip != null) "ACTIVE RE-CALCULATED DRIVE" else "STANDBY SYSTEM",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = VibrantNavy,
                    letterSpacing = 1.5.sp
                )
            }

            // Speedometer and metrics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vector Interactive Custom Gauge Speedometer
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    SpeedometerGauge(speedVal = currentSpeedNum, useMiles = useMiles)
                }

                // Trip mileage details next to Speedometer
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Trip Dist icon",
                            tint = VibrantBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "TRIP DISTANCE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = VibrantNavy.copy(alpha = 0.5f)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%.2f %s", displayActiveDistance, distanceUnit),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = VibrantBlue
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Current Speed icon",
                            tint = VibrantTextDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "SPEEDOMETER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = VibrantNavy.copy(alpha = 0.5f)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f %s", currentSpeedNum, speedLabel),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = VibrantNavy
                    )
                }
            }

            // Main Start/Stop button
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

@Composable
fun PreferencesCard(
    isAutostartEnabled: Boolean,
    isAutoCalculationEnabled: Boolean,
    useMiles: Boolean,
    viewModel: MileageTrackerViewModel
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VibrantLightGray),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Text(
                text = "INTELLIGENT RUNNING PREFERENCES",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = VibrantNavy.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )

            // Autostart toggle row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Allow Autostart each boot", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VibrantTextDark)
                    Text("Automatically open app and start tracking on device boot", fontSize = 10.sp, color = Color.Gray)
                }
                Switch(
                    checked = isAutostartEnabled,
                    onCheckedChange = { viewModel.toggleAutostart(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = VibrantBlue),
                    modifier = Modifier.testTag("autostart_switch")
                )
            }

            Divider(color = VibrantBorder.copy(alpha = 0.3f))

            // Auto Calculation toggle row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Auto-Calculate Trips", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VibrantTextDark)
                    Text("Auto start when moving > 5 km/h", fontSize = 10.sp, color = Color.Gray)
                }
                Switch(
                    checked = isAutoCalculationEnabled,
                    onCheckedChange = { viewModel.toggleAutoCalculation(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = VibrantBlue),
                    modifier = Modifier.testTag("autocalc_switch")
                )
            }

            Divider(color = VibrantBorder.copy(alpha = 0.3f))

            // Distance unit toggle row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Preferred Unit System", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VibrantTextDark)
                    Text("Switch between imperial or metric scale", fontSize = 10.sp, color = Color.Gray)
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
        }
    }
}

// Dial Needle speedometer drawing component
@Composable
fun SpeedometerGauge(speedVal: Double, useMiles: Boolean) {
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

            // 1. Draw static background dial grey ring arc
            drawArc(
                color = VibrantBorder.copy(alpha = 0.3f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )

            // 2. Draw active dynamic speed arc indicator in Blue
            drawArc(
                color = VibrantBlue,
                startAngle = startAngle,
                sweepAngle = animSpeedAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. Draw notch indicators inside the speedometer
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
        }

        // Center typography speedometer readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = String.format(Locale.getDefault(), "%.0f", speedVal),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = VibrantNavy,
                letterSpacing = (-1).sp
            )
            Text(
                text = if (useMiles) "MPH" else "KM/H",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color.Gray,
                letterSpacing = 1.sp
            )
        }
    }
}

// Single trip item component in log
@Composable
fun TripLogItem(trip: Trip, deleteTrip: () -> Unit, useMiles: Boolean) {
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
