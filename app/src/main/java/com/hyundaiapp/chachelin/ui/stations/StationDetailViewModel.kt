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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

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
    private var refreshJob: Job? = null

    /** Nav에서 진입할 때 한 번, 오류 화면에서 재시도할 때 다시 호출한다. */
    fun refresh() {
        refreshJob?.cancel()
        station = null
        _state.value = StationDetailUiState()
        refreshJob = viewModelScope.launch {
            try {
                val result = withTimeout(10_000L) {
                    stations.get(stationId)?.let { it to stations.detail(stationId) }
                }
                if (result == null) {
                    _state.value = StationDetailUiState(loading = false, error = "충전소 정보를 찾을 수 없어요")
                    return@launch
                }
                val (row, detail) = result
                val (st, sc) = row
                station = st
                _state.value = StationDetailUiState(
                    station = StationRowUi(
                        id = st.id, name = st.name, operator = st.operator,
                        distanceM = signals.location.value?.let { here -> GeoMath.distanceM(here.point, st.point) },
                        score = sc.chachelinScore, verifiedCharges = sc.verifiedCharges,
                        avgKw = sc.avgKw, recentFailures7d = sc.recentFailures7d,
                    ),
                    ratingCounts = detail?.ratingCounts ?: List(5) { 0 },
                    topTags = detail?.topTags.orEmpty(),
                    recentFailureTimesMs = detail?.recentFailureTimesMs.orEmpty(),
                    loading = false,
                )
            } catch (_: TimeoutCancellationException) {
                _state.value = StationDetailUiState(loading = false, error = "응답이 늦어지고 있어요. 다시 시도해 주세요")
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = StationDetailUiState(loading = false, error = "충전소 정보를 불러오지 못했어요")
            }
        }
    }

    fun onRouteClick() {
        if (station != null) _state.update { it.copy(confirmRoute = true) }
    }

    fun onConfirmRoute() {
        station?.let { navi.requestRoute(RouteDestination(it.name, it.point)) }
        _state.update { it.copy(confirmRoute = false) }
    }

    fun onDismissRoute() = _state.update { it.copy(confirmRoute = false) }
}
