package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.repository.TripRepository
import com.example.service.MileageTrackingService
import com.example.ui.MileageTrackerScreen
import com.example.ui.MileageTrackerViewModel
import com.example.ui.MileageTrackerViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MileageTrackerViewModel

    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == MileageTrackingService.BROADCAST_ACTION_STATUS) {
                val speed = intent.getDoubleExtra(MileageTrackingService.EXTRA_SPEED, 0.0)
                val distance = intent.getDoubleExtra(MileageTrackingService.EXTRA_DISTANCE, 0.0)
                val isMonitoring = intent.getBooleanExtra(MileageTrackingService.EXTRA_IS_MONITORING, false)
                viewModel.updateLiveStatus(speed, distance, isMonitoring)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core dependency bootstrap
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TripRepository(database.tripDao(), applicationContext)
        val factory = MileageTrackerViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MileageTrackerViewModel::class.java]

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val useDarkTheme = when (themeMode) {
                1 -> false // Light Mode
                2 -> true  // Dark Mode
                else -> androidx.compose.foundation.isSystemInDarkTheme() // System (0)
            }
            MyApplicationTheme(darkTheme = useDarkTheme) {
                MileageTrackerScreen(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(MileageTrackingService.BROADCAST_ACTION_STATUS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(serviceReceiver, filter)
        }

        // Poll current service state if active to sync client view model immediately
        val intent = Intent(this, MileageTrackingService::class.java).apply {
            action = MileageTrackingService.ACTION_REFRESH_STATE
        }
        try {
            startService(intent)
        } catch (e: Exception) {
            // Service not running yet, that's fine
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(serviceReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
    }
}
