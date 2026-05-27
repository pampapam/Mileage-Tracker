package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.entity.Trip
import com.example.data.repository.TripRepository
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import java.util.Locale

class MileageTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: TripRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    // Satellite and Signal Strength properties
    private var locationManager: LocationManager? = null
    private var gnssStatusCallback: GnssStatus.Callback? = null
    private var currentSatellitesConnected: Int = 0
    private var currentGpsSignalLevel: Int = 0 // 0 to 4 bars

    // Tracking State
    private var activeTripId: Int? = null
    private var accumulatedDistanceMeters: Double = 0.0
    private var lastLocation: Location? = null
    private var lastMovementTime: Long = System.currentTimeMillis()
    private var currentSpeedMs: Double = 0.0

    // Sync Preferences cache
    private var totalCompletedDistanceMeters: Double = 0.0
    private var customTotalMileageOffset: Double = 0.0
    private var useMilesSystem: Boolean = false

    // Notification IDs and Channels
    private val NOTIFICATION_ID = 8871
    private val CHANNEL_ID = "mileage_tracking_channel"

    companion object {
        const val ACTION_START_TRACKING = "com.example.service.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.example.service.STOP_TRACKING"
        const val ACTION_REFRESH_STATE = "com.example.service.REFRESH_STATE"
        const val ACTION_REFRESH_GPS_CONNECTION = "com.example.service.REFRESH_GPS_CONNECTION"

        // Status broadcast for visual UI syncing if needed
        const val BROADCAST_ACTION_STATUS = "com.example.service.STATUS_UPDATE"
        const val EXTRA_TRIP_ID = "extra_trip_id"
        const val EXTRA_DISTANCE = "extra_distance"
        const val EXTRA_IS_MONITORING = "extra_is_monitoring"
        const val EXTRA_SPEED = "extra_speed"
        const val EXTRA_SATELLITES = "extra_satellites"
        const val EXTRA_SIGNAL_LEVEL = "extra_signal_level"
    }

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(this)
        repository = TripRepository(database.tripDao(), this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        createNotificationChannel()

        // Monitor flow updates to synchronously update our notification
        serviceScope.launch {
            repository.totalCompletedDistance.collect { completed ->
                totalCompletedDistanceMeters = completed ?: 0.0
                updateForegroundNotification()
            }
        }
        serviceScope.launch {
            repository.customTotalMileage.collect { offset ->
                customTotalMileageOffset = offset
                updateForegroundNotification()
            }
        }
        serviceScope.launch {
            repository.useMiles.collect { useMiles ->
                useMilesSystem = useMiles
                updateForegroundNotification()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_TRACKING
        
        when (action) {
            ACTION_START_TRACKING -> {
                startForegroundServiceCompat()
                serviceScope.launch {
                    // Load active trip if it exists in DB, otherwise auto resume/start
                    val activeTrip = databaseGetActiveTrip()
                    if (activeTrip != null) {
                        activeTripId = activeTrip.id
                        accumulatedDistanceMeters = activeTrip.distanceMeters
                    }
                    startGpsTracking()
                }
            }
            ACTION_STOP_TRACKING -> {
                stopGpsTracking()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
            ACTION_REFRESH_GPS_CONNECTION -> {
                startForegroundServiceCompat()
                reconnectGps()
            }
            ACTION_REFRESH_STATE -> {
                broadcastStatus()
                if (locationCallback == null && activeTripId == null) {
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundServiceCompat() {
        // Safe check for location permission before starting foreground of type location
        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            // Cannot start as a foreground service with type location now because permissions are missing.
            // Start as standard foreground without type to prevent "did not then call Service.startForeground()" crash, then immediately stop.
            val notification = buildNotification("OdoFlow Standby", "Location permissions are required to track mileage.")
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            stopSelf()
            return
        }

        // Use dynamic formatted values from the beginning
        val distanceFactor = if (useMilesSystem) 0.000621371 else 0.001
        val unitLabel = if (useMilesSystem) "mi" else "km"
        val tripDist = accumulatedDistanceMeters * distanceFactor
        val speedFactor = if (useMilesSystem) 2.23694 else 3.6
        val speedVal = currentSpeedMs * speedFactor
        val speedUnit = if (useMilesSystem) "mph" else "km/h"
        val totalMileage = customTotalMileageOffset + (totalCompletedDistanceMeters * distanceFactor) + tripDist

        val title = if (activeTripId != null) "Active Trip Tracking" else "OdoFlow Standby"
        val content = String.format(
            Locale.getDefault(),
            "Trip: %.2f %s   |   Speed: %.1f %s   |   Odo: %.1f %s",
            tripDist, unitLabel,
            speedVal, speedUnit,
            totalMileage, unitLabel
        )

        val notification = buildNotification(title, content)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID, 
                    notification, 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (ignored: Exception) {}
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Trip Tracking Service"
            val descriptionText = "Monitors GPS coordinates to track driver mileage"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MileageTrackingService::class.java).apply {
            action = ACTION_STOP_TRACKING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_compass) // Standard robust Android system icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Tracking", stopPendingIntent)
            .build()
    }

    private fun updateNotification(ignoredContent: String = "") {
        updateForegroundNotification()
    }

    private fun updateForegroundNotification() {
        val distanceFactor = if (useMilesSystem) 0.000621371 else 0.001
        val unitLabel = if (useMilesSystem) "mi" else "km"

        val tripDist = accumulatedDistanceMeters * distanceFactor

        val speedFactor = if (useMilesSystem) 2.23694 else 3.6
        val speedVal = currentSpeedMs * speedFactor
        val speedUnit = if (useMilesSystem) "mph" else "km/h"

        val totalMileage = customTotalMileageOffset + (totalCompletedDistanceMeters * distanceFactor) + tripDist

        val title = if (activeTripId != null) "Active Trip Tracking" else "OdoFlow Standby"
        
        val content = String.format(
            Locale.getDefault(),
            "Trip: %.2f %s   |   Speed: %.1f %s   |   Odo: %.1f %s",
            tripDist, unitLabel,
            speedVal, speedUnit,
            totalMileage, unitLabel
        )

        val notification = buildNotification(title, content)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun startGpsTracking() {
        if (locationCallback != null) return // Already running

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(2000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                handleNewLocation(location)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                mainLooper
            )
            registerGnssStatus()
            updateNotification()
        } catch (unlikely: SecurityException) {
            updateNotification("Location permissions missing!")
        }
    }

    private fun registerGnssStatus() {
        if (locationManager == null) {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && gnssStatusCallback == null) {
            gnssStatusCallback = object : GnssStatus.Callback() {
                override fun onSatelliteStatusChanged(status: GnssStatus) {
                    val totalSats = status.satelliteCount
                    var inFixCount = 0
                    var totalSnr = 0.0
                    
                    for (i in 0 until totalSats) {
                        if (status.usedInFix(i)) {
                            inFixCount++
                            totalSnr += status.getCn0DbHz(i)
                        }
                    }
                    
                    currentSatellitesConnected = inFixCount
                    val averageCn0 = if (inFixCount > 0) totalSnr / inFixCount else 0.0
                    
                    // Wifi-like signal strength (0 to 4 bars)
                    currentGpsSignalLevel = when {
                        inFixCount == 0 -> 0
                        averageCn0 >= 32.0 -> 4
                        averageCn0 >= 26.0 -> 3
                        averageCn0 >= 18.0 -> 2
                        else -> 1
                    }
                    
                    broadcastStatus()
                    updateNotification()
                }
            }
            
            try {
                locationManager?.registerGnssStatusCallback(gnssStatusCallback!!, null)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    private fun unregisterGnssStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && gnssStatusCallback != null) {
            try {
                locationManager?.unregisterGnssStatusCallback(gnssStatusCallback!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            gnssStatusCallback = null
        }
        currentSatellitesConnected = 0
        currentGpsSignalLevel = 0
    }

    private fun reconnectGps() {
        stopGpsTrackingOnly()
        startGpsTracking()
    }

    private fun stopGpsTrackingOnly() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
        lastLocation = null
        currentSpeedMs = 0.0
        unregisterGnssStatus()
    }

    private fun stopGpsTracking() {
        stopGpsTrackingOnly()
        activeTripId = null
        accumulatedDistanceMeters = 0.0
    }

    private fun handleNewLocation(location: Location) {
        // We only use points with reasonable accuracy to filter drift
        if (location.accuracy > 40.0) return

        currentSpeedMs = if (location.hasSpeed()) location.speed.toDouble() else 0.0

        serviceScope.launch {
            val autoCalcEnabled = repository.isAutoCalculationEnabled.value

            if (activeTripId == null) {
                if (autoCalcEnabled) {
                    // If not tracking, check speed for AUTO-START if enabled
                    val speed = if (location.hasSpeed()) location.speed else 0.0f
                    if (speed > 1.39f) { // ~5.0 km/h is standard movement speed threshold
                        // Auto-start active trip!
                        val newId = repository.startNewTrip(isAutomatic = true)
                        activeTripId = newId.toInt()
                        accumulatedDistanceMeters = 0.0
                        lastLocation = location
                        lastMovementTime = System.currentTimeMillis()
                        
                        updateNotification("Started calculated trip automatically: 0.00 km")
                        broadcastStatus()
                    }
                }
            } else {
                // We have an active trip
                val distanceRatio = if (repository.useMiles.value) 0.000621371 else 0.001
                val unitLabel = if (repository.useMiles.value) "mi" else "km"

                if (lastLocation != null) {
                    val deltaMeters = location.distanceTo(lastLocation!!).toDouble()
                    
                    // Filter outlier jumps and small drift additions
                    if (deltaMeters > 2.0 && deltaMeters < 500.0) {
                        accumulatedDistanceMeters += deltaMeters
                        repository.updateActiveTripDistance(accumulatedDistanceMeters)
                        lastLocation = location
                        lastMovementTime = System.currentTimeMillis()
                    }
                } else {
                    lastLocation = location
                    lastMovementTime = System.currentTimeMillis()
                }

                // Check for AUTO-STOP if speed is very low for a while
                val speed = if (location.hasSpeed()) location.speed else 0.0f
                if (speed > 0.5f) {
                    lastMovementTime = System.currentTimeMillis()
                }

                // If no movement for 3 minutes (180,000 ms), auto stop completed trip!
                if (autoCalcEnabled && (System.currentTimeMillis() - lastMovementTime > 180000L)) {
                    repository.stopActiveTrip()
                    activeTripId = null
                    lastLocation = null
                    accumulatedDistanceMeters = 0.0
                    updateNotification("Trip completed automatically. Standby.")
                    broadcastStatus()
                } else {
                    val displayDist = accumulatedDistanceMeters * distanceRatio
                    updateNotification(String.format(Locale.getDefault(), "Driving: %.2f %s", displayDist, unitLabel))
                    broadcastStatus()
                }
            }
        }
    }

    private suspend fun databaseGetActiveTrip(): Trip? {
        val database = AppDatabase.getDatabase(this)
        return database.tripDao().getActiveTripSync()
    }

    private fun broadcastStatus() {
        val intent = Intent(BROADCAST_ACTION_STATUS).apply {
            putExtra(EXTRA_TRIP_ID, activeTripId)
            putExtra(EXTRA_DISTANCE, accumulatedDistanceMeters)
            putExtra(EXTRA_IS_MONITORING, locationCallback != null)
            putExtra(EXTRA_SPEED, currentSpeedMs)
            putExtra(EXTRA_SATELLITES, currentSatellitesConnected)
            putExtra(EXTRA_SIGNAL_LEVEL, currentGpsSignalLevel)
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        stopGpsTracking()
        serviceScope.cancel()
        super.onDestroy()
    }
}
