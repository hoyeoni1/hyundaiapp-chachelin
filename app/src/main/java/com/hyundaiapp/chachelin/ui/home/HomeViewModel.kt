package com.hyundaiapp.chachelin.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyundaiapp.chachelin.data.ChargeRecordRepository
import com.hyundaiapp.chachelin.data.StationRepository
import com.hyundaiapp.chachelin.data.VehicleProfileStore
import com.hyundaiapp.chachelin.data.stationName
import com.hyundaiapp.chachelin.domain.ChargeResult
import com.hyundaiapp.chachelin.domain.ChargeScreenTarget
import com.hyundaiapp.chachelin.domain.VehiclePreset
import com.hyundaiapp.chachelin.ui.HistoryRowUi
import com.hyundaiapp.chachelin.ui.HomeChargeStatus
import com.hyundaiapp.chachelin.ui.HomeUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 홈. 지금 충전 상태, 내 차, 최근 기록 한 건을 보여 준다(11-09 시연 피드백: 홈 화면 없음) */
class HomeViewModel(
    target: Flow<ChargeScreenTarget>,
    private val records: ChargeRecordRepository,
    private val stations: StationRepository,
    private val profile: VehicleProfileStore,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            target.collect { t -> _state.update { it.copy(charge = t.toHomeStatus()) } }
        }
        viewModelScope.launch {
            profile.presetIndex.collect { index ->
                val label = index?.let { VehiclePreset.ALL.getOrNull(it)?.label?.substringBefore(" (") }
                _state.update { it.copy(carLabel = label) }
            }
        }
        viewModelScope.launch {
            combine(records.myRecords(), profile.onboarded) { list, _ -> list }.collectLatest { list ->
                val last = list.maxByOrNull { it.startedAtMs }
                _state.update {
                    it.copy(
                        recordCount = list.size,
                        lastRecord = last?.let { r ->
                            HistoryRowUi(
                                sessionId = r.id,
                                stationName = r.stationId?.let { id -> stations.stationName(id) },
                                startedAtMs = r.startedAtMs,
                                socStart = r.socStart,
                                socEnd = r.socEnd,
                                verified = r.result == ChargeResult.SUCCESS,
                                failed = r.result == ChargeResult.FAILED,
                            )
                        },
                    )
                }
            }
        }
    }

    private fun ChargeScreenTarget.toHomeStatus() = when (this) {
        ChargeScreenTarget.CHARGING -> HomeChargeStatus.CHARGING
        ChargeScreenTarget.REVIEW -> HomeChargeStatus.REVIEW
        ChargeScreenTarget.FAILURE -> HomeChargeStatus.FAILURE
        ChargeScreenTarget.NONE -> HomeChargeStatus.IDLE
    }
}
