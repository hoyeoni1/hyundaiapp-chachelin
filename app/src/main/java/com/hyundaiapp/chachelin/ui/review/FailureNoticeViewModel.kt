package com.hyundaiapp.chachelin.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyundaiapp.chachelin.data.StationRepository
import com.hyundaiapp.chachelin.data.stationName
import com.hyundaiapp.chachelin.domain.ChargeResult
import com.hyundaiapp.chachelin.domain.ChargeSessionRecorder
import com.hyundaiapp.chachelin.ui.FailureNoticeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** S5 실패 기록 알림. 처리할 실패 세션이 없으면 상태가 null */
class FailureNoticeViewModel(
    private val recorder: ChargeSessionRecorder,
    private val stations: StationRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<FailureNoticeUiState?>(null)
    val state: StateFlow<FailureNoticeUiState?> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            recorder.pendingResult.collectLatest { record ->
                _state.value = record?.takeIf { it.result == ChargeResult.FAILED }?.let {
                    FailureNoticeUiState(it.id, it.stationId?.let { id -> stations.stationName(id) } ?: "충전소")
                }
            }
        }
    }

    fun onNotFailure() {
        _state.value?.let { recorder.retractFailure(it.sessionId) }
    }

    fun onConfirm() {
        _state.value?.let { recorder.dismiss(it.sessionId) }
    }
}
