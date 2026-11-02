package com.hyundaiapp.chachelin.domain

import com.hyundaiapp.chachelin.data.Station
import com.hyundaiapp.chachelin.data.StationRepository
import com.hyundaiapp.core.EvBatteryState
import com.hyundaiapp.core.VehicleSignals
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

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
    /** S7에서 고른 차종의 용량. SDK가 값을 주면(현재는 퍼미션 문제로 null) SDK 값을 먼저 쓴다 */
    private val presetCapacityKwh: () -> Float? = { null },
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _phase = MutableStateFlow(ChargePhase.IDLE)
    private val _live = MutableStateFlow<LiveCharge?>(null)
    private val _completed = MutableSharedFlow<ChargeSessionRecord>(extraBufferCapacity = 8)

    val phase: StateFlow<ChargePhase> = _phase.asStateFlow()
    val live: StateFlow<LiveCharge?> = _live.asStateFlow()
    val completed: SharedFlow<ChargeSessionRecord> = _completed.asSharedFlow()

    private var current: Session? = null

    /** 기록을 시작할 때 부른다(scope가 끝나면 함께 멈춤). */
    fun start(scope: CoroutineScope) {
        scope.launch {
            signals.evBattery.collect { ev ->
                val session = current
                when {
                    ev.chargePortOpen == false -> session?.let { close(it, ev, scope) }
                    session != null -> update(session, ev)
                    ev.chargePortOpen == true -> open(ev, scope)
                }
            }
        }
    }

    /** 기록을 멈출 때 부른다. 끝나지 않은 세션은 기록하지 않고 버린다 */
    fun reset() {
        current = null
        _phase.value = ChargePhase.IDLE
        _live.value = null
    }

    private fun open(ev: EvBatteryState, scope: CoroutineScope) {
        val session = Session(startedAtMs = clock())
        current = session
        _phase.value = ChargePhase.PORT_OPEN
        update(session, ev)
        // 조회가 서버를 타도 충전구 신호를 놓치지 않도록 따로 돌린다
        session.station = scope.async {
            findStation().also { station ->
                session.stationId = station?.id
                if (current === session) publishLive(session)
            }
        }
    }

    private fun update(session: Session, ev: EvBatteryState) {
        // 충전구가 열렸을 때 SoC를 아직 못 받았으면 처음 받은 값을 시작 SoC로 쓴다
        val soc = ev.socPercent ?: return
        val socStart = session.socStart ?: soc.also { session.socStart = it }
        session.socNow = soc
        if (soc - socStart >= rules.chargingSocGain) _phase.value = ChargePhase.CHARGING
        publishLive(session)
    }

    private fun close(session: Session, ev: EvBatteryState, scope: CoroutineScope) {
        val endedAtMs = clock()
        val socEnd = ev.socPercent ?: session.socNow
        current = null
        _phase.value = ChargePhase.IDLE
        _live.value = null
        scope.launch {
            val station = withTimeoutOrNull(STATION_LOOKUP_TIMEOUT_MS) { session.station.await() }
            session.station.cancel()
            val socStart = session.socStart ?: return@launch
            if (socEnd == null) return@launch
            val durationMs = endedAtMs - session.startedAtMs
            val result = rules.judge(socEnd - socStart, durationMs, atStation = station != null) ?: return@launch
            _completed.emit(
                ChargeSessionRecord(
                    id = UUID.randomUUID().toString(),
                    stationId = station?.id,
                    startedAtMs = session.startedAtMs,
                    endedAtMs = endedAtMs,
                    socStart = socStart,
                    socEnd = socEnd,
                    avgKw = averageKw(socStart, socEnd, durationMs),
                    result = result,
                ),
            )
        }
    }

    private fun publishLive(session: Session) {
        val socStart = session.socStart ?: return
        val socNow = session.socNow ?: return
        _live.value = LiveCharge(
            stationId = session.stationId,
            startedAtMs = session.startedAtMs,
            socStart = socStart,
            socNow = socNow,
            avgKw = averageKw(socStart, socNow, clock() - session.startedAtMs),
        )
    }

    private fun averageKw(socStart: Int, socEnd: Int, durationMs: Long): Double? {
        val capacity = signals.evBattery.value.capacityKwh?.takeIf { it > 0f } ?: presetCapacityKwh() ?: return null
        return ChargeMath.averageKw(socStart, socEnd, capacity, durationMs)
    }

    /** 위치를 아직 못 받았으면 받을 때까지 기다린다. 조회가 실패하면 충전소를 모르는 것으로 본다(null) */
    private suspend fun findStation(): Station? = try {
        val here = signals.location.filterNotNull().first().point
        stations.nearest(here, rules.stationRadiusM)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private class Session(val startedAtMs: Long) {
        lateinit var station: Deferred<Station?>
        var stationId: String? = null
        var socStart: Int? = null
        var socNow: Int? = null
    }

    private companion object {
        /** 충전구를 닫은 뒤 위치·충전소 조회를 기다리는 최대 시간. 넘으면 충전소를 모르는 것으로 본다 */
        const val STATION_LOOKUP_TIMEOUT_MS = 10_000L
    }
}
