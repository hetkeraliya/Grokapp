package com.example.miband5.ui.dashboard

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.miband5.data.AppDatabase
import com.example.miband5.data.AuthKeyStore
import com.example.miband5.data.entity.DailyStats
import com.example.miband5.data.entity.ManualWorkout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DayData(
    val date: LocalDate,
    val steps: Int,
    val hrAvg: Int?,
    val hrMin: Int?,
    val hrMax: Int?,
    val sleep: Int,
    val calories: Int,
    val distanceMeters: Int,
    val battery: Int?,
    val stress: Int?,
    val notes: String
)

data class DashboardReady(
    val days: List<DayData>,
    val today: LocalDate,
    val workouts: List<ManualWorkout>,
    val goal: Int,
    val streak: Int
)

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Ready(val data: DashboardReady) : DashboardUiState
}

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val store = AuthKeyStore(app)

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    var selectedIndex by mutableIntStateOf(6)
        private set
    var selectedTab by mutableIntStateOf(0)
        private set
    var page by mutableStateOf("chart")
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val start = today.minusDays(6)
            val rows = db.dailyStatsDao().getRange(start.toString(), today.toString())
            val byDate = rows.associateBy { it.date }
            val days = (0..6).map { i ->
                val d = start.plusDays(i.toLong())
                val row = byDate[d.toString()]
                DayData(
                    date = d,
                    steps = row?.steps ?: 0,
                    hrAvg = row?.heartRateAvg,
                    hrMin = row?.heartRateMin,
                    hrMax = row?.heartRateMax,
                    sleep = row?.sleepMinutes ?: 0,
                    calories = row?.calories ?: 0,
                    distanceMeters = row?.distanceMeters ?: 0,
                    battery = row?.batteryLast,
                    stress = row?.stressAvg,
                    notes = row?.notes ?: ""
                )
            }
            val workouts = db.manualWorkoutDao().getAll()
            val goal = store.stepGoal
            _uiState.value = DashboardUiState.Ready(
                DashboardReady(
                    days = days,
                    today = today,
                    workouts = workouts,
                    goal = goal,
                    streak = streak(days, goal)
                )
            )
        }
    }

    fun selectDay(index: Int) {
        selectedIndex = index
    }

    fun selectTab(tab: Int) {
        selectedTab = tab
    }

    fun selectPage(value: String) {
        page = value
    }

    fun setGoal(goal: Int) {
        store.stepGoal = goal
        refresh()
    }

    fun saveNote(date: LocalDate, note: String) {
        viewModelScope.launch {
            val key = date.toString()
            val existing = db.dailyStatsDao().getByDate(key) ?: DailyStats(date = key)
            db.dailyStatsDao().upsert(existing.copy(notes = note))
            refresh()
        }
    }

    fun logWorkout(label: String) {
        viewModelScope.launch {
            db.manualWorkoutDao().insert(
                ManualWorkout(date = LocalDate.now().toString(), label = label)
            )
            refresh()
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            db.dailyStatsDao().clear()
            refresh()
        }
    }

    private fun streak(days: List<DayData>, goal: Int): Int {
        var n = 0
        for (day in days.asReversed()) {
            if (day.steps >= goal) n++ else break
        }
        return n
    }
}
