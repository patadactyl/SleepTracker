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
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {

        private var viewModelJob = Job()

        //Define a scope for coroutines to run in
        private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

        //Create tonight live data var and use coroutine to initialize from database
        private var tonight = MutableLiveData<SleepNight?>()
        //get all nights from database
        private val nights = database.getAllNights()
        init {
                initializeTonight()
        }
        private fun initializeTonight() {
                uiScope.launch {
                        tonight.value = getTonightFromDatabase()
                }
        }
        private suspend fun getTonightFromDatabase(): SleepNight? {
                return withContext(Dispatchers.IO) {
                       var night = database.getTonight()
                        if(night?.endTimeMilli != night?.startTimeMilli) {
                                night = null
                        }
                        night
                }
        }

        //Add local functions for insert(). update(), and clear()
        //Implement click handlers for Start, Stop, and Clear buttons using coroutines to do the database work
        //Executes when the START button is clicked.
        fun onStartTracking() {
                uiScope.launch {
                        val newNight = SleepNight()
                        insert(newNight)
                        tonight.value= getTonightFromDatabase()
                }
        }
        private suspend fun insert(night: SleepNight) {
                withContext(Dispatchers.IO) {
                        database.insert(night)
                }
        }
        //Executes when the STOP button is clicked.
        fun onStopTracking() {
                uiScope.launch {
                        val oldNight = tonight.value?: return@launch
                        oldNight.endTimeMilli = System.currentTimeMillis()
                        update(oldNight)
                }
        }
        private suspend fun update(night: SleepNight) {
                withContext(Dispatchers.IO) {
                        database.update(night)
                }
        }
        //Executes when the CLEAR button is clicked.
        fun onClear() {
                uiScope.launch {
                        clear()
                        tonight.value = null
                }
        }
        private suspend fun clear() {
                withContext(Dispatchers.IO) {
                        database.clear()
                }
        }

        //Transform nights into a nightsString


        override fun onCleared() {
                super.onCleared()
                viewModelJob.cancel()
        }
}

