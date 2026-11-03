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

/** 충전구가 닫혀 끝난 세션 1건. 충전소 밖 기록은 기기에만, 충전소 기록은 서버에 저장 */
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
    /**
     * 시연 배속. 콘솔로 SoC를 15초 만에 올리면 실측 kW가 비현실적으로 나오므로(40→80%가 약 7,000kW)
     * 화면의 충전 시간과 kW 계산에만 곱한다. 판정 시간 기준에는 쓰지 않는다. 시연 때 배속임을 밝힌다(PRD 7절).
     */
    val demoTimeScale: Int = 1,
) {
    /**
     * 끝난 세션 판정. PRD 5.3 표에 없는 경우(충전구만 잠깐 열었음, 1%p만 오름 등)는 충전도 실패도 아니므로 null = 기록하지 않음.
     * 충전소 밖에서도 SoC가 오르지 않았으면 충전이 아니므로 기록하지 않는다.
     */
    fun judge(socGain: Int, durationMs: Long, atStation: Boolean): ChargeResult? = when {
        !atStation -> if (socGain >= chargingSocGain) ChargeResult.NOT_AT_STATION else null
        socGain >= successMinSocGain && durationMs >= successMinDurationMs -> ChargeResult.SUCCESS
        socGain < chargingSocGain && durationMs >= failMinOpenMs -> ChargeResult.FAILED
        else -> null
    }

    companion object {
        val DEFAULT = ChargeRules()

        /** 디버그 빌드 시연용(PRD 7절: 실패 시나리오 30초). 1초를 2분으로 보여 준다 */
        val DEMO = ChargeRules(successMinDurationMs = 10_000L, failMinOpenMs = 30_000L, demoTimeScale = 120)
    }
}
