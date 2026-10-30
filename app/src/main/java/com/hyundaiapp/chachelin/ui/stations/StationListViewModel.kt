package com.hyundaiapp.chachelin.ui.stations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyundaiapp.chachelin.data.StationRepository
import com.hyundaiapp.chachelin.ui.StationListUiState
import com.hyundaiapp.chachelin.ui.StationRowUi
import com.hyundaiapp.core.GeoMath
import com.hyundaiapp.core.VehicleSignals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * S1 근처 충전소. 저장소 → ViewModel → UiState → 화면 흐름의 예시(다른 화면도 이 모양으로 만든다).
 * 위치는 처음 한 번만 받는다(정차 화면이라 실시간 갱신 불필요).
 */
class StationListViewModel(
    private val signals: VehicleSignals,
    private val stations: StationRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(StationListUiState())
    val state: StateFlow<StationListUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching {
                val here = signals.location.filterNotNull().first().point
                stations.nearby(here, RADIUS_M).map { (st, sc) ->
                    StationRowUi(
                        id = st.id, name = st.name, operator = st.operator,
                        distanceM = GeoMath.distanceM(here, st.point),
                        score = sc.chachelinScore, verifiedCharges = sc.verifiedCharges,
                        avgKw = sc.avgKw, recentFailures7d = sc.recentFailures7d,
                    )
                }
            }.onSuccess { rows ->
                _state.value = StationListUiState(loading = false, items = rows)
            }.onFailure {
                _state.value = StationListUiState(loading = false, error = it.message)
            }
        }
    }

    private companion object {
        const val RADIUS_M = 3_000.0
    }
}
