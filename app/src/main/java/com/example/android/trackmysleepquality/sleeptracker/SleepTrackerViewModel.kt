/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {

        //Create tonight live data var and use coroutine to initialize from database
        private var tonight = MutableLiveData<SleepNight?>()
        //get all nights from database
        private val nights = database.getAllNights()
        //initialize tonight variable
        init {
                initializeTonight()
        }
        //Start a coroutine in the ViewModelScope
        private fun initializeTonight() {
                viewModelScope.launch {
                        tonight.value = getTonightFromDatabase()
                }
        }
        private suspend fun getTonightFromDatabase(): SleepNight? {
                //let coroutine get tonight from database
                var night = database.getTonight()
                //if start and end times are different then the night has already completed
                if(night?.endTimeMilli != night?.startTimeMilli) {
                        night = null
                }
                return night
        }

        private suspend fun insert(night: SleepNight) {
                database.insert(night)
        }

        private suspend fun update(night: SleepNight) {
                database.update(night)
        }
        private suspend fun clear() {
                withContext(Dispatchers.IO) {
                        database.clear()
                }
        }

        //Executes when the START button is clicked.
        fun onStartTracking() {
                viewModelScope.launch {
                        val newNight = SleepNight()
                        insert(newNight)
                        //set tonight to the new night
                        tonight.value= getTonightFromDatabase()
                }
        }

        //Executes when the STOP button is clicked.
        fun onStopTracking() {
                //Launch a coroutine in viewModelScope
                viewModelScope.launch {
                        //if endTimeMilli hasn't been set yet, set it to the current system time
                        val oldNight = tonight.value?: return@launch
                        oldNight.endTimeMilli = System.currentTimeMillis()
                        update(oldNight)
                }
        }

        //Executes when the CLEAR button is clicked.
        fun onClear() {
                viewModelScope.launch {
                        clear()
                        tonight.value = null
                }
        }

        //Transform nights into a nightsString
        val nightsString = Transformations.map(nights) { nights ->
                formatNights(nights, application.resources)
        }
}

