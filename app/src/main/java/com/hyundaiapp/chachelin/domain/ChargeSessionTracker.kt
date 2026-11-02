package com.hyundaiapp.chachelin.domain

import com.hyundaiapp.chachelin.data.StationRepository
import com.hyundaiapp.core.VehicleSignals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 충전 자동 기록(F1)·인증 판정(F2)·실측 속도(F3)·실패 기록(F4). 담당: 이상준.
 *
 * 입력: [VehicleSignals.evBattery](충전구·SoC·용량), [VehicleSignals.location], [StationRepository.nearest]
 * 출력: [phase], [live](S3 화면), [completed](S4 리뷰 / S5 실패 알림, 업로드)
 *
 * 판정 규칙은 PRD 5.3과 [ChargeRules]. 시간은 [clock]으로만 읽어서 테스트에서 바꿀 수 있게 한다.
 * 테스트 예: FakeVehicleSignals로 충전구 열림 → SoC 40→80 → 닫힘을 넣고 completed에 SUCCESS가 나오는지 확인.
 */
class ChargeSessionTracker(
    private val signals: VehicleSignals,
    private val stations: StationRepository,
    private val rules: ChargeRules,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _phase = MutableStateFlow(ChargePhase.IDLE)
    private val _live = MutableStateFlow<LiveCharge?>(null)
    private val _completed = MutableSharedFlow<ChargeSessionRecord>(extraBufferCapacity = 8)

    val phase: StateFlow<ChargePhase> = _phase.asStateFlow()
    val live: StateFlow<LiveCharge?> = _live.asStateFlow()
    val completed: SharedFlow<ChargeSessionRecord> = _completed.asSharedFlow()

    /** 앱 수명 동안 한 번 부른다(scope가 끝나면 함께 멈춤). */
    fun start(scope: CoroutineScope) {
        // TODO(이상준): signals.evBattery를 구독해 PRD 5.3 상태 전이를 구현
        //  - 충전구 열림: 시작 SoC·시각·위치 저장, 가장 가까운 충전소 조회 → PORT_OPEN
        //  - SoC +chargingSocGain 이상 → CHARGING, live 갱신(ChargeMath.averageKw)
        //  - 충전구 닫힘: SUCCESS / FAILED / NOT_AT_STATION 판정 → completed.emit, IDLE
    }
}
