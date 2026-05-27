package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.dao.TripDao
import com.example.data.entity.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TripRepository(
    private val tripDao: TripDao,
    context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "mileage_tracker_preferences",
        Context.MODE_PRIVATE
    )

    // Flow of completed and all trips
    val completedTrips: Flow<List<Trip>> = tripDao.getCompletedTripsFlow()
    val activeTrip: Flow<Trip?> = tripDao.getActiveTripFlow()
    val totalCompletedDistance: Flow<Double?> = tripDao.getTotalCompletedDistanceFlow()

    // Preferences: Custom Total Mileage Offset
    private val _customTotalMileage = MutableStateFlow(getCustomTotalMileagePref())
    val customTotalMileage: StateFlow<Double> = _customTotalMileage.asStateFlow()

    // Preferences: Use Miles instead of Kilometers
    private val _useMiles = MutableStateFlow(getUseMilesPref())
    val useMiles: StateFlow<Boolean> = _useMiles.asStateFlow()

    // Preferences: Autostart service on system boot
    private val _isAutostartEnabled = MutableStateFlow(getIsAutostartEnabledPref())
    val isAutostartEnabled: StateFlow<Boolean> = _isAutostartEnabled.asStateFlow()

    // Preferences: Enable automatic trip detection (auto start/stop)
    private val _isAutoCalculationEnabled = MutableStateFlow(getIsAutoCalculationEnabledPref())
    val isAutoCalculationEnabled: StateFlow<Boolean> = _isAutoCalculationEnabled.asStateFlow()

    // Preferences: Theme Mode (0 = System, 1 = Light, 2 = Dark)
    private val _themeMode = MutableStateFlow(getThemeModePref())
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    // Preferences: Speedometer Theme (0 = Analog, 1 = Digital, 2 = Retro, 3 = Minimalist, 4 = Sporty)
    private val _speedometerTheme = MutableStateFlow(getSpeedometerThemePref())
    val speedometerTheme: StateFlow<Int> = _speedometerTheme.asStateFlow()

    // Preferences: Dashboard Card Order (comma-separated lists of: "odometer", "gps", "speedometer")
    private val _cardOrder = MutableStateFlow(getCardOrderPref())
    val cardOrder: StateFlow<List<String>> = _cardOrder.asStateFlow()

    // Preferences: Landscape featured column side ("left" or "right")
    private val _landscapeFeaturedSide = MutableStateFlow(getLandscapeFeaturedSidePref())
    val landscapeFeaturedSide: StateFlow<String> = _landscapeFeaturedSide.asStateFlow()

    // Preferences: Landscape featured card ID ("speedometer", "odometer", "gps")
    private val _landscapeFeaturedCard = MutableStateFlow(getLandscapeFeaturedCardPref())
    val landscapeFeaturedCard: StateFlow<String> = _landscapeFeaturedCard.asStateFlow()

    // Helper functions for prefs
    private fun getLandscapeFeaturedSidePref(): String {
        return prefs.getString("landscape_featured_side", "left") ?: "left"
    }

    private fun getLandscapeFeaturedCardPref(): String {
        return prefs.getString("landscape_featured_card", "speedometer") ?: "speedometer"
    }

    fun setLandscapeFeaturedSide(side: String) {
        prefs.edit().putString("landscape_featured_side", side).apply()
        _landscapeFeaturedSide.value = side
    }

    fun setLandscapeFeaturedCard(cardId: String) {
        prefs.edit().putString("landscape_featured_card", cardId).apply()
        _landscapeFeaturedCard.value = cardId
    }

    private fun getCardOrderPref(): List<String> {
        val saved = prefs.getString("dashboard_card_order", null)
        val defaultCards = listOf("odometer", "gps", "speedometer")
        return if (!saved.isNullOrEmpty()) {
            saved.split(",").filter { it in defaultCards }
        } else {
            defaultCards
        }
    }

    fun setCardOrder(order: List<String>) {
        prefs.edit().putString("dashboard_card_order", order.joinToString(",")).apply()
        _cardOrder.value = order
    }

    private fun getCustomTotalMileagePref(): Double {
        return prefs.getFloat("custom_total_mileage", 0f).toDouble()
    }

    private fun getUseMilesPref(): Boolean {
        return prefs.getBoolean("use_miles", false)
    }

    private fun getIsAutostartEnabledPref(): Boolean {
        return prefs.getBoolean("is_autostart_enabled", false)
    }

    private fun getIsAutoCalculationEnabledPref(): Boolean {
        return prefs.getBoolean("is_auto_calculation_enabled", true) // Default to true
    }

    private fun getThemeModePref(): Int {
        return prefs.getInt("theme_mode", 0) // Default to 0 (System)
    }

    private fun getSpeedometerThemePref(): Int {
        return prefs.getInt("speedometer_theme", 0) // Default to 0 (Analog)
    }

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    fun setSpeedometerTheme(theme: Int) {
        prefs.edit().putInt("speedometer_theme", theme).apply()
        _speedometerTheme.value = theme
    }

    fun setCustomTotalMileage(mileage: Double) {
        prefs.edit().putFloat("custom_total_mileage", mileage.toFloat()).apply()
        _customTotalMileage.value = mileage
    }

    fun setUseMiles(useMiles: Boolean) {
        prefs.edit().putBoolean("use_miles", useMiles).apply()
        _useMiles.value = useMiles
    }

    fun setIsAutostartEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_autostart_enabled", enabled).apply()
        _isAutostartEnabled.value = enabled
    }

    fun setIsAutoCalculationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_auto_calculation_enabled", enabled).apply()
        _isAutoCalculationEnabled.value = enabled
    }

    // Database operations
    suspend fun startNewTrip(isAutomatic: Boolean = false): Long {
        // First end any existing active trip
        val existingActive = tripDao.getActiveTripSync()
        if (existingActive != null) {
            tripDao.update(existingActive.copy(isActive = false, endTime = System.currentTimeMillis()))
        }

        // Start a new one
        val newTrip = Trip(
            startTime = System.currentTimeMillis(),
            isActive = true,
            isAutomatic = isAutomatic
        )
        return tripDao.insert(newTrip)
    }

    suspend fun updateActiveTripDistance(distanceMeters: Double) {
        val active = tripDao.getActiveTripSync() ?: return
        tripDao.update(active.copy(distanceMeters = distanceMeters))
    }

    suspend fun stopActiveTrip(): Trip? {
        val active = tripDao.getActiveTripSync() ?: return null
        val stoppedTrip = active.copy(
            isActive = false,
            endTime = System.currentTimeMillis()
        )
        tripDao.update(stoppedTrip)
        return stoppedTrip
    }

    suspend fun deleteTrip(trip: Trip) {
        tripDao.delete(trip)
    }

    suspend fun deleteTripById(id: Int) {
        tripDao.deleteById(id)
    }

    suspend fun clearAllData() {
        tripDao.deleteAll()
        setCustomTotalMileage(0.0)
    }
}
