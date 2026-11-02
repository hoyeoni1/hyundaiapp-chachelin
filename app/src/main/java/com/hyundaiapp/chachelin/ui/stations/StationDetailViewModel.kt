package com.hyundaiapp.chachelin.ui.stations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyundaiapp.chachelin.data.Station
import com.hyundaiapp.chachelin.data.StationRepository
import com.hyundaiapp.chachelin.ui.StationDetailUiState
import com.hyundaiapp.chachelin.ui.StationRowUi
import com.hyundaiapp.core.GeoMath
import com.hyundaiapp.core.NaviControl
import com.hyundaiapp.core.RouteDestination
import com.hyundaiapp.core.VehicleSignals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** S2 충전소 상세와 길안내(F6). [길안내]는 기존 경로를 덮어쓰므로 확인을 받은 뒤에만 경로를 요청한다 */
class StationDetailViewModel(
    private val stationId: String,
    private val signals: VehicleSignals,
    private val navi: NaviControl,
    private val stations: StationRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(StationDetailUiState())
    val state: StateFlow<StationDetailUiState> = _state.asStateFlow()

    private var station: Station? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            // 실패하면 station이 null로 남는다(화면은 불러오는 중으로 표시)
            runCatching {
                val (st, sc) = stations.get(stationId) ?: return@launch
                val detail = stations.detail(stationId)
                station = st
                _state.update {
                    it.copy(
                        station = StationRowUi(
                            id = st.id, name = st.name, operator = st.operator,
                            distanceM = signals.location.value?.let { here -> GeoMath.distanceM(here.point, st.point) },
                            score = sc.chachelinScore, verifiedCharges = sc.verifiedCharges,
                            avgKw = sc.avgKw, recentFailures7d = sc.recentFailures7d,
                        ),
                        ratingCounts = detail?.ratingCounts ?: it.ratingCounts,
                        topTags = detail?.topTags.orEmpty(),
                        recentFailureTimesMs = detail?.recentFailureTimesMs.orEmpty(),
                    )
                }
            }
        }
    }

    fun onRouteClick() = _state.update { it.copy(confirmRoute = true) }

    fun onConfirmRoute() {
        station?.let { navi.requestRoute(RouteDestination(it.name, it.point)) }
        _state.update { it.copy(confirmRoute = false) }
    }

    fun onDismissRoute() = _state.update { it.copy(confirmRoute = false) }
}
