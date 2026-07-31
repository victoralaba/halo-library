package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.ReadingStatsRepository
import com.example.data.repository.ReadingStatsSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

enum class ChartMetricMode {
    TIME_SPENT,
    BOOKS_COMPLETED
}

class ReadingStatsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ReadingStatsRepository.getInstance(
        context = application,
        bookDao = db.bookDao(),
        audioTrackDao = db.audioTrackDao()
    )

    val statsSummary: StateFlow<ReadingStatsSummary> = repository.observeStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReadingStatsSummary()
        )

    private val _chartMetricMode = MutableStateFlow(ChartMetricMode.TIME_SPENT)
    val chartMetricMode: StateFlow<ChartMetricMode> = _chartMetricMode.asStateFlow()

    fun setChartMetricMode(mode: ChartMetricMode) {
        _chartMetricMode.value = mode
    }

    fun addManualEbookTime(minutes: Long) {
        repository.addEbookReadingTime(minutes * 60)
    }

    fun addManualAudioTime(minutes: Long) {
        repository.addAudioListeningTime(minutes * 60)
    }

    fun resetStats() {
        repository.resetAllStats()
    }
}
