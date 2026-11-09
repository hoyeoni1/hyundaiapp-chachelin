package com.hyundaiapp.chachelin.ui

import com.hyundaiapp.chachelin.data.ReviewTag

/*
 * 화면 상태 계약(UiState). PRD 5.4 화면 S1~S7.
 * - ViewModel(이상준)이 이 상태를 내보내고, 화면(길민종)은 이 상태만 보고 그린다.
 * - 바꿀 때는 두 사람이 같은 PR에서 합의한다. 미리보기 데이터는 PreviewData.kt.
 * - 숫자 포맷(소수점, 단위)은 화면에서 한다. 여기에는 원래 값만 둔다.
 */

/** S1 근처 충전소 / S2 상세 공용 행 */
data class StationRowUi(
    val id: String,
    val name: String,
    val operator: String,
    val distanceM: Double?,
    /** null이면 "평가 중" */
    val score: Double?,
    val verifiedCharges: Int,
    val avgKw: Double?,
    val recentFailures7d: Int,
)

/** S1 근처 충전소 */
data class StationListUiState(
    val loading: Boolean = true,
    val items: List<StationRowUi> = emptyList(),
    val error: String? = null,
)

/** S2 충전소 상세 */
data class StationDetailUiState(
    val station: StationRowUi? = null,
    /** 별점 1~5 개수(인덱스 0 = 1점) */
    val ratingCounts: List<Int> = List(5) { 0 },
    val topTags: List<ReviewTag> = emptyList(),
    val recentFailureTimesMs: List<Long> = emptyList(),
    /** [길안내] 누르기 전 "현재 경로를 덮어씁니다" 확인 */
    val confirmRoute: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
)

/** S3 충전 중 */
data class ChargingUiState(
    val stationName: String?,
    val socStart: Int,
    val socNow: Int,
    val elapsedSec: Long,
    val avgKw: Double?,
)

/** S4 충전 완료 리뷰 */
data class ReviewUiState(
    val sessionId: String,
    val stationName: String,
    val socStart: Int,
    val socEnd: Int,
    val durationMin: Int,
    val avgKw: Double?,
    val rating: Int? = null,
    val selectedTags: Set<ReviewTag> = emptySet(),
    val submitting: Boolean = false,
)

/** S5 실패 기록 알림 */
data class FailureNoticeUiState(
    val sessionId: String,
    val stationName: String,
)

/** S6 내 충전 기록 */
data class HistoryRowUi(
    val sessionId: String,
    val stationName: String?,
    val startedAtMs: Long,
    val socStart: Int,
    val socEnd: Int,
    val verified: Boolean,
    val failed: Boolean,
)

data class HistoryUiState(val items: List<HistoryRowUi> = emptyList())

/** S7 첫 실행 동의(동의 버튼 외 동작은 동의 아님) + 차종 선택 */
data class OnboardingUiState(
    val locationAgreed: Boolean = false,
    val privacyAgreed: Boolean = false,
    val vehiclePresets: List<String> = listOf("아이오닉 5 (77.4kWh)", "아이오닉 6 (77.4kWh)", "EV6 (77.4kWh)", "코나 일렉트릭 (64.8kWh)"),
    val selectedPreset: Int? = null,
    /** 이미 동의를 마친 뒤 다시 들어온 경우(설정). 동의를 다시 받지 않고 저장된 값을 보여 준다(11-09 시연 피드백) */
    val editing: Boolean = false,
)

/** 홈의 충전 상태 칸 */
enum class HomeChargeStatus { IDLE, CHARGING, REVIEW, FAILURE }

/** 홈(11-09 시연 피드백: 홈 화면 없음) */
data class HomeUiState(
    val charge: HomeChargeStatus = HomeChargeStatus.IDLE,
    /** S7에서 고른 차 이름. 모르면 null */
    val carLabel: String? = null,
    val recordCount: Int = 0,
    val lastRecord: HistoryRowUi? = null,
)
