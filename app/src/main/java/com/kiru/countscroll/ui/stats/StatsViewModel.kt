package com.kiru.countscroll.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kiru.countscroll.data.local.dao.CountScrollDao
import com.kiru.countscroll.domain.AppFilter
import com.kiru.countscroll.domain.StatisticsCalculator
import com.kiru.countscroll.domain.StatisticsReport
import com.kiru.countscroll.domain.StatsPeriod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ChartMetric(val label: String) {
    SCROLLS("Grafik Scroll"),
    DURATION("Grafik Durasi")
}

data class StatsUiState(
    val selectedPeriod: StatsPeriod = StatsPeriod.TODAY,
    val selectedFilter: AppFilter = AppFilter.ALL,
    val selectedMetric: ChartMetric = ChartMetric.SCROLLS,
    val hasUsageAccessPermission: Boolean = false,
    val report: StatisticsReport = StatisticsReport(
        period = StatsPeriod.TODAY,
        filter = AppFilter.ALL,
        totalScrolls = 0,
        totalDurationMillis = 0L,
        comparisonText = "Belum ada aktivitas scroll tercatat",
        dailyPoints = emptyList()
    )
)

class StatsViewModel(
    private val dao: CountScrollDao
) : ViewModel() {

    private val periodFlow = MutableStateFlow(StatsPeriod.TODAY)
    private val filterFlow = MutableStateFlow(AppFilter.ALL)
    private val metricFlow = MutableStateFlow(ChartMetric.SCROLLS)
    private val usageAccessFlow = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentScrollsFlow = periodFlow.flatMapLatest { period ->
        val (start, end) = StatisticsCalculator.getWindowBounds(period)
        dao.observeScrollEventsBetween(start, end)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentSessionsFlow = periodFlow.flatMapLatest { period ->
        val (start, end) = StatisticsCalculator.getWindowBounds(period)
        dao.observeUsageSessionsBetween(start, end)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val previousScrollsFlow = periodFlow.flatMapLatest { period ->
        val (prevStart, prevEnd) = StatisticsCalculator.getPreviousWindowBounds(period)
        dao.observeScrollEventsBetween(prevStart, prevEnd)
    }

    val uiState: StateFlow<StatsUiState> = combine(
        periodFlow,
        filterFlow,
        metricFlow,
        usageAccessFlow,
        currentScrollsFlow,
        currentSessionsFlow,
        previousScrollsFlow
    ) { values ->
        val period = values[0] as StatsPeriod
        val filter = values[1] as AppFilter
        val metric = values[2] as ChartMetric
        val usageAccess = values[3] as Boolean
        @Suppress("UNCHECKED_CAST")
        val scrolls = values[4] as List<com.kiru.countscroll.data.local.entity.ScrollEvent>
        @Suppress("UNCHECKED_CAST")
        val sessions = values[5] as List<com.kiru.countscroll.data.local.entity.UsageSession>
        @Suppress("UNCHECKED_CAST")
        val prevScrolls = values[6] as List<com.kiru.countscroll.data.local.entity.ScrollEvent>

        val report = StatisticsCalculator.buildReport(
            period = period,
            filter = filter,
            currentScrolls = scrolls,
            currentSessions = sessions,
            previousScrolls = prevScrolls
        )

        StatsUiState(
            selectedPeriod = period,
            selectedFilter = filter,
            selectedMetric = metric,
            hasUsageAccessPermission = usageAccess,
            report = report
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState()
    )

    fun selectPeriod(period: StatsPeriod) {
        periodFlow.value = period
    }

    fun selectFilter(filter: AppFilter) {
        filterFlow.value = filter
    }

    fun selectMetric(metric: ChartMetric) {
        metricFlow.value = metric
    }

    fun refreshUsageAccessStatus(granted: Boolean) {
        usageAccessFlow.value = granted
    }

    fun resetAllData() {
        viewModelScope.launch {
            dao.clearAllScrollEvents()
            dao.clearAllUsageSessions()
            dao.clearAllDelayLogs()
        }
    }
}

class StatsViewModelFactory(
    private val dao: CountScrollDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatsViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
