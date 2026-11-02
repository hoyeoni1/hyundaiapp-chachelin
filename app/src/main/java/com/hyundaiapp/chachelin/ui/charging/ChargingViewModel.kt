package com.hyundaiapp.chachelin.ui.charging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyundaiapp.chachelin.data.StationRepository
import com.hyundaiapp.chachelin.data.stationName
import com.hyundaiapp.chachelin.domain.LiveCharge
import com.hyundaiapp.chachelin.ui.ChargingUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** S3 충전 중. 경과 시간은 1초마다 다시 계산한다. 진행 중인 세션이 없으면 null */
class ChargingViewModel(
    live: StateFlow<LiveCharge?>,
    stations: StationRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val stationName: Flow<String?> = live.map { it?.stationId }.distinctUntilChanged()
        .map { id -> id?.let { stations.stationName(it) } }

    private val ticker: Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(1_000L)
        }
    }

    val state: StateFlow<ChargingUiState?> = combine(live, stationName, ticker) { charge, name, _ ->
        charge?.let {
            ChargingUiState(
                stationName = name,
                socStart = it.socStart,
                socNow = it.socNow,
                elapsedSec = ((clock() - it.startedAtMs) / 1_000L).coerceAtLeast(0L),
                avgKw = it.avgKw,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)
}
