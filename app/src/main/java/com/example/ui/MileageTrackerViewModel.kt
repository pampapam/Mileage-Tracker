package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.Trip
import com.example.data.repository.TripRepository
import com.example.service.MileageTrackingService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MileageTrackerViewModel(
    private val repository: TripRepository
) : ViewModel() {

    // Trips and stats
    val completedTrips: StateFlow<List<Trip>> = repository.completedTrips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTrip: StateFlow<Trip?> = repository.activeTrip
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val totalCompletedDistance: StateFlow<Double?> = repository.totalCompletedDistance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // General preferences
    val customTotalMileage: StateFlow<Double> = repository.customTotalMileage
    val useMiles: StateFlow<Boolean> = repository.useMiles
    val isAutostartEnabled: StateFlow<Boolean> = repository.isAutostartEnabled
    val isAutoCalculationEnabled: StateFlow<Boolean> = repository.isAutoCalculationEnabled

    // Live update fields (received via service broadcasts)
    private val _currentSpeed = MutableStateFlow(0.0) // in m/s
    val currentSpeed: StateFlow<Double> = _currentSpeed.asStateFlow()

    private val _isGpsActive = MutableStateFlow(false)
    val isGpsActive: StateFlow<Boolean> = _isGpsActive.asStateFlow()

    private val _liveActiveDistance = MutableStateFlow(0.0) // in meters
    val liveActiveDistance: StateFlow<Double> = _liveActiveDistance.asStateFlow()

    // Helper: Total combined mileage
    val totalMileage: StateFlow<Double> = combine(
        totalCompletedDistance,
        activeTrip,
        liveActiveDistance,
        customTotalMileage,
        useMiles
    ) { completed, active, liveDist, customOffset, miles ->
        val completedVal = completed ?: 0.0
        val activeVal = if (active != null) liveDist.coerceAtLeast(active.distanceMeters) else 0.0
        // Convert to display units
        val totalMeters = completedVal + activeVal
        val factor = if (miles) 0.000621371 else 0.001
        customOffset + (totalMeters * factor)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun updateLiveStatus(speedMs: Double, distanceMeters: Double, isGpsActive: Boolean) {
        _currentSpeed.value = speedMs
        _liveActiveDistance.value = distanceMeters
        _isGpsActive.value = isGpsActive
    }

    // User actions
    fun startTracking(context: Context) {
        viewModelScope.launch {
            // Write database starting record
            repository.startNewTrip(isAutomatic = false)
            
            // Start the GPS background service
            val intent = Intent(context, MileageTrackingService::class.java).apply {
                action = MileageTrackingService.ACTION_START_TRACKING
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }

    fun stopTracking(context: Context) {
        viewModelScope.launch {
            repository.stopActiveTrip()
            
            // Stop the GPS background service
            val intent = Intent(context, MileageTrackingService::class.java).apply {
                action = MileageTrackingService.ACTION_STOP_TRACKING
            }
            context.stopService(intent)
            
            // Reset live state
            _currentSpeed.value = 0.0
            _liveActiveDistance.value = 0.0
        }
    }

    fun setCustomTotalMileage(mileage: Double) {
        viewModelScope.launch {
            repository.setCustomTotalMileage(mileage)
        }
    }

    fun toggleUseMiles(useMiles: Boolean) {
        viewModelScope.launch {
            repository.setUseMiles(useMiles)
        }
    }

    fun toggleAutostart(enabled: Boolean) {
        viewModelScope.launch {
            repository.setIsAutostartEnabled(enabled)
        }
    }

    fun toggleAutoCalculation(enabled: Boolean) {
        viewModelScope.launch {
            repository.setIsAutoCalculationEnabled(enabled)
        }
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch {
            repository.deleteTrip(trip)
        }
    }

    fun resetAll(context: Context) {
        viewModelScope.launch {
            // Stop service first if running
            val intent = Intent(context, MileageTrackingService::class.java)
            context.stopService(intent)
            
            // Reset DB and custom total
            repository.clearAllData()
            _currentSpeed.value = 0.0
            _liveActiveDistance.value = 0.0
        }
    }
}

class MileageTrackerViewModelFactory(
    private val repository: TripRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MileageTrackerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MileageTrackerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
