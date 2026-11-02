package com.hyundaiapp.chachelin.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyundaiapp.chachelin.data.ChargeRecordRepository
import com.hyundaiapp.chachelin.data.StationRepository
import com.hyundaiapp.chachelin.data.stationName
import com.hyundaiapp.chachelin.domain.ChargeResult
import com.hyundaiapp.chachelin.ui.HistoryRowUi
import com.hyundaiapp.chachelin.ui.HistoryUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** S6 내 충전 기록. stationName이 null이면 충전소가 아닌 곳의 기록(NOT_AT_STATION) */
class HistoryViewModel(
    private val records: ChargeRecordRepository,
    private val stations: StationRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            records.myRecords().collectLatest { list ->
                _state.value = HistoryUiState(
                    list.map {
                        HistoryRowUi(
                            sessionId = it.id,
                            stationName = it.stationId?.let { id -> stations.stationName(id) },
                            startedAtMs = it.startedAtMs,
                            socStart = it.socStart,
                            socEnd = it.socEnd,
                            verified = it.result == ChargeResult.SUCCESS,
                            failed = it.result == ChargeResult.FAILED,
                        )
                    },
                )
            }
        }
    }
}
