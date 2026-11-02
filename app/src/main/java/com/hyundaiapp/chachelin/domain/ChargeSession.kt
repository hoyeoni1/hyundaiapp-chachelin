package com.hyundaiapp.chachelin.domain

/** 충전 세션 상태. PRD 5.3: IDLE → PORT_OPEN → CHARGING → (SUCCESS | FAILED) → IDLE */
enum class ChargePhase { IDLE, PORT_OPEN, CHARGING }

enum class ChargeResult {
    /** 인증 충전: 리뷰·점수에 반영 */
    SUCCESS,

    /** 충전 실패: 충전구를 열었지만 SoC가 오르지 않음 */
    FAILED,

    /** 충전소 근처가 아님(집 충전 등): 기록만 남기고 점수에 반영하지 않음 */
    NOT_AT_STATION,
}

/** 충전 중 화면(S3)에 보여 줄 실시간 값 */
data class LiveCharge(
    val stationId: String?,
    val startedAtMs: Long,
    val socStart: Int,
    val socNow: Int,
    /** 지금까지의 실측 평균 속도. 아직 계산할 수 없으면 null */
    val avgKw: Double?,
)

/** 충전구가 닫혀 끝난 세션 1건. 서버 `charge_sessions` 행과 1:1 */
data class ChargeSessionRecord(
    val id: String,
    val stationId: String?,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val socStart: Int,
    val socEnd: Int,
    val avgKw: Double?,
    val result: ChargeResult,
)

/** 판정 기준값. PRD 5.3 표와 같은 값. 시연에서는 [DEMO]로 시간 기준을 줄인다 */
data class ChargeRules(
    val successMinSocGain: Int = 2,
    val successMinDurationMs: Long = 3 * 60_000L,
    /** 충전 중으로 보는 SoC 상승폭 */
    val chargingSocGain: Int = 1,
    val failMinOpenMs: Long = 2 * 60_000L,
    val stationRadiusM: Double = 150.0,
) {
    companion object {
        val DEFAULT = ChargeRules()

        /** 디버그 빌드 시연용(PRD 7절: 실패 시나리오 30초) */
        val DEMO = ChargeRules(successMinDurationMs = 10_000L, failMinOpenMs = 30_000L)
    }
}
