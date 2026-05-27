package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.repository.TripRepository

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MileageTrackerViewModel::class.java)) {
            val database = AppDatabase.getDatabase(context)
            val repository = TripRepository(database.tripDao(), context)
            @Suppress("UNCHECKED_CAST")
            return MileageTrackerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
