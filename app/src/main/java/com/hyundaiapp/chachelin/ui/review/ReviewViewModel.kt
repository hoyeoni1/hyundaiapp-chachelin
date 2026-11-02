package com.hyundaiapp.chachelin.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyundaiapp.chachelin.data.ReviewTag
import com.hyundaiapp.chachelin.data.StationRepository
import com.hyundaiapp.chachelin.data.stationName
import com.hyundaiapp.chachelin.domain.ChargeResult
import com.hyundaiapp.chachelin.domain.ChargeSessionRecord
import com.hyundaiapp.chachelin.domain.ChargeSessionRecorder
import com.hyundaiapp.chachelin.ui.ReviewUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * S4 충전 완료 리뷰. 처리할 인증 세션이 없으면 상태가 null.
 * [완료]·[나중에] 모두 바로 닫힌다: 전송과 재시도는 [ChargeSessionRecorder]가 맡는다. [나중에]는 세션 기록만 남기고 리뷰는 남기지 않는다.
 */
class ReviewViewModel(
    private val recorder: ChargeSessionRecorder,
    private val stations: StationRepository,
    /** [com.hyundaiapp.chachelin.domain.ChargeRules.demoTimeScale] */
    private val timeScale: Int = 1,
) : ViewModel() {
    private val _state = MutableStateFlow<ReviewUiState?>(null)
    val state: StateFlow<ReviewUiState?> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            recorder.pendingResult.collectLatest { record ->
                _state.value = record?.takeIf { it.result == ChargeResult.SUCCESS }?.let { it.toUi() }
            }
        }
    }

    fun onRate(rating: Int) = _state.update { it?.copy(rating = rating) }

    fun onToggleTag(tag: ReviewTag) = _state.update {
        it?.copy(selectedTags = if (tag in it.selectedTags) it.selectedTags - tag else it.selectedTags + tag)
    }

    fun onSubmit() {
        val current = _state.value ?: return
        val rating = current.rating ?: return
        recorder.submitReview(current.sessionId, rating, current.selectedTags)
    }

    fun onLater() {
        _state.value?.let { recorder.dismiss(it.sessionId) }
    }

    private suspend fun ChargeSessionRecord.toUi() = ReviewUiState(
        sessionId = id,
        stationName = stationId?.let { stations.stationName(it) } ?: "충전소",
        socStart = socStart,
        socEnd = socEnd,
        durationMin = ((endedAtMs - startedAtMs) * timeScale / 60_000.0).roundToInt(),
        avgKw = avgKw,
    )
}
