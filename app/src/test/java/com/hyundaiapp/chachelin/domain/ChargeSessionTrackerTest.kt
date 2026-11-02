package com.hyundaiapp.chachelin.domain

import com.hyundaiapp.chachelin.data.FakeStationRepository
import com.hyundaiapp.chachelin.data.Station
import com.hyundaiapp.chachelin.data.StationRepository
import com.hyundaiapp.core.FakeVehicleSignals
import com.hyundaiapp.core.GeoPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** FakeVehicleSignals 기본 위치는 시드 충전소 st-003과 같은 좌표다 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChargeSessionTrackerTest {
    private val fake = FakeVehicleSignals()
    private var now = 1_000_000L
    private val minutes = 60_000L

    private class Harness(val tracker: ChargeSessionTracker, val done: List<ChargeSessionRecord>)

    private fun run(
        stations: StationRepository = FakeStationRepository(),
        capacity: Float? = 77.4f,
        body: suspend TestScope.(Harness) -> Unit,
    ) = runTest(UnconfinedTestDispatcher()) {
        fake.setEv { copy(capacityKwh = null) }
        val tracker = ChargeSessionTracker(
            fake, stations, ChargeRules.DEFAULT,
            presetCapacityKwh = { capacity }, clock = { now },
        )
        val done = mutableListOf<ChargeSessionRecord>()
        backgroundScope.launch { tracker.completed.toList(done) }
        tracker.start(backgroundScope)
        runCurrent()
        body(Harness(tracker, done))
    }

    private fun TestScope.ev(open: Boolean? = fake.evBattery.value.chargePortOpen, soc: Int? = fake.evBattery.value.socPercent) {
        fake.setEv { copy(chargePortOpen = open, socPercent = soc) }
        runCurrent()
    }

    @Test
    fun `열림 SoC 상승 닫힘이면 인증 충전으로 기록한다`() = run { h ->
        ev(open = false, soc = 40)
        ev(open = true)
        assertEquals(ChargePhase.PORT_OPEN, h.tracker.phase.value)
        now += 30 * minutes
        ev(soc = 41)
        assertEquals(ChargePhase.CHARGING, h.tracker.phase.value)
        ev(soc = 80)
        ev(open = false)

        val record = h.done.single()
        assertEquals(ChargeResult.SUCCESS, record.result)
        assertEquals("st-003", record.stationId)
        assertEquals(40, record.socStart)
        assertEquals(80, record.socEnd)
        assertEquals(30 * minutes, record.endedAtMs - record.startedAtMs)
        // 0.4 × 77.4 ÷ 0.5h
        assertEquals(61.92, record.avgKw!!, 0.01)
        assertEquals(ChargePhase.IDLE, h.tracker.phase.value)
        assertNull(h.tracker.live.value)
    }

    @Test
    fun `닫힘과 같은 신호에 들어온 마지막 SoC를 쓴다`() = run { h ->
        ev(open = false, soc = 40)
        ev(open = true)
        now += 30 * minutes
        ev(open = false, soc = 80)
        assertEquals(80, h.done.single().socEnd)
    }

    @Test
    fun `SoC가 오르지 않고 2분 이상 열려 있다 닫히면 실패로 기록한다`() = run { h ->
        ev(open = false, soc = 40)
        ev(open = true)
        now += 2 * minutes
        ev(open = false)

        val record = h.done.single()
        assertEquals(ChargeResult.FAILED, record.result)
        assertEquals("st-003", record.stationId)
        assertNull(record.avgKw)
    }

    @Test
    fun `충전구를 잠깐 열었다 닫으면 기록하지 않는다`() = run { h ->
        ev(open = false, soc = 40)
        ev(open = true)
        now += 10_000L
        ev(open = false)
        assertTrue(h.done.isEmpty())
        assertEquals(ChargePhase.IDLE, h.tracker.phase.value)
    }

    @Test
    fun `충전소에서 150m 밖이면 충전소 없이 기록만 남긴다`() = run { h ->
        fake.setLocation(37.5, 127.0)
        ev(open = false, soc = 40)
        ev(open = true)
        now += 30 * minutes
        ev(soc = 80)
        ev(open = false)

        val record = h.done.single()
        assertEquals(ChargeResult.NOT_AT_STATION, record.result)
        assertNull(record.stationId)
    }

    @Test
    fun `충전구를 연 뒤에 위치가 들어와도 충전소를 찾는다`() = run { h ->
        fake.location.value = null
        ev(open = false, soc = 40)
        ev(open = true)
        fake.setLocation(37.4127, 127.0947)
        runCurrent()
        assertEquals("st-003", h.tracker.live.value?.stationId)
        now += 30 * minutes
        ev(soc = 80)
        ev(open = false)
        assertEquals(ChargeResult.SUCCESS, h.done.single().result)
    }

    @Test
    fun `위치를 끝내 못 받으면 제한 시간 뒤 충전소 없이 끝낸다`() = run { h ->
        fake.location.value = null
        ev(open = false, soc = 40)
        ev(open = true)
        now += 30 * minutes
        ev(soc = 80)
        ev(open = false)
        assertTrue(h.done.isEmpty())
        advanceTimeBy(10_001L)
        assertEquals(ChargeResult.NOT_AT_STATION, h.done.single().result)
    }

    @Test
    fun `충전소 조회가 실패해도 앱이 죽지 않고 충전소 없이 기록한다`() = run(stations = ThrowingStations) { h ->
        ev(open = false, soc = 40)
        ev(open = true)
        now += 30 * minutes
        ev(soc = 80)
        ev(open = false)
        assertEquals(ChargeResult.NOT_AT_STATION, h.done.single().result)
    }

    @Test
    fun `충전소 조회가 느려도 다음 세션 신호를 놓치지 않는다`() = run(stations = SlowStations(FakeStationRepository())) { h ->
        ev(open = false, soc = 40)
        ev(open = true)
        now += 30 * minutes
        ev(open = false, soc = 80)
        ev(open = true)
        assertEquals(ChargePhase.PORT_OPEN, h.tracker.phase.value)
        now += 30 * minutes
        ev(open = false, soc = 90)
        advanceTimeBy(5_001L)

        assertEquals(listOf(40, 80), h.done.map { it.socStart })
        assertEquals(listOf(80, 90), h.done.map { it.socEnd })
        assertNotEquals(h.done[0].id, h.done[1].id)
    }

    @Test
    fun `SoC를 받기 전에 충전구가 열리면 처음 받은 값을 시작 SoC로 쓴다`() = run { h ->
        ev(open = false, soc = null)
        ev(open = true)
        assertNull(h.tracker.live.value)
        ev(soc = 40)
        assertEquals(40, h.tracker.live.value?.socStart)
        now += 30 * minutes
        ev(soc = 80)
        ev(open = false)
        assertEquals(40, h.done.single().socStart)
    }

    @Test
    fun `SoC를 끝내 못 받은 세션은 기록하지 않는다`() = run { h ->
        ev(open = false, soc = null)
        ev(open = true)
        now += 30 * minutes
        ev(open = false)
        assertTrue(h.done.isEmpty())
    }

    @Test
    fun `충전구 값이 null이면 세션을 끝내지 않는다`() = run { h ->
        ev(open = false, soc = 40)
        ev(open = true)
        ev(open = null)
        assertEquals(ChargePhase.PORT_OPEN, h.tracker.phase.value)
        assertTrue(h.done.isEmpty())
    }

    @Test
    fun `앱 시작 때 이미 충전구가 열려 있으면 세션을 시작한다`() = run { h ->
        ev(open = true, soc = 40)
        assertEquals(ChargePhase.PORT_OPEN, h.tracker.phase.value)
    }

    @Test
    fun `live는 SoC가 오르면 지금까지의 평균 속도를 보여 준다`() = run { h ->
        ev(open = false, soc = 40)
        ev(open = true)
        assertNull(h.tracker.live.value!!.avgKw)
        now += 30 * minutes
        ev(soc = 50)
        // 0.1 × 77.4 ÷ 0.5h
        assertEquals(15.48, h.tracker.live.value!!.avgKw!!, 0.01)
    }

    @Test
    fun `SDK 용량이 있으면 프리셋보다 먼저 쓴다`() = run(capacity = 64.8f) { h ->
        fake.setEv { copy(capacityKwh = 100f) }
        ev(open = false, soc = 40)
        ev(open = true)
        now += 30 * minutes
        ev(open = false, soc = 80)
        // 0.4 × 100 ÷ 0.5h
        assertEquals(80.0, h.done.single().avgKw!!, 0.01)
    }

    @Test
    fun `용량을 모르면 속도만 비우고 인증은 그대로 한다`() = run(capacity = null) { h ->
        ev(open = false, soc = 40)
        ev(open = true)
        now += 30 * minutes
        ev(open = false, soc = 80)
        assertEquals(ChargeResult.SUCCESS, h.done.single().result)
        assertNull(h.done.single().avgKw)
    }

    @Test
    fun `시연 배속은 kW에만 곱하고 기록 시각은 그대로 둔다`() = runTest(UnconfinedTestDispatcher()) {
        fake.setEv { copy(capacityKwh = null, chargePortOpen = false, socPercent = 40) }
        val tracker = ChargeSessionTracker(fake, FakeStationRepository(), ChargeRules.DEMO, { 77.4f }, clock = { now })
        val done = mutableListOf<ChargeSessionRecord>()
        backgroundScope.launch { tracker.completed.toList(done) }
        tracker.start(backgroundScope)
        runCurrent()
        ev(open = true)
        now += 15_000L
        ev(open = false, soc = 80)

        val record = done.single()
        assertEquals(ChargeResult.SUCCESS, record.result)
        assertEquals(15_000L, record.endedAtMs - record.startedAtMs)
        // 15초 × 120 = 30분: 0.4 × 77.4 ÷ 0.5h
        assertEquals(61.92, record.avgKw!!, 0.01)
    }

    @Test
    fun `기록을 멈추면 끝나지 않은 세션을 버린다`() = run { h ->
        ev(open = false, soc = 40)
        ev(open = true)
        now += 30 * minutes
        ev(soc = 80)
        h.tracker.reset()
        assertEquals(ChargePhase.IDLE, h.tracker.phase.value)
        assertNull(h.tracker.live.value)

        ev(open = false)
        assertTrue(h.done.isEmpty())
    }

    private object ThrowingStations : StationRepository by FakeStationRepository() {
        override suspend fun nearest(point: GeoPoint, radiusM: Double): Station? = error("네트워크 오류")
    }

    private class SlowStations(private val base: StationRepository) : StationRepository by base {
        override suspend fun nearest(point: GeoPoint, radiusM: Double): Station? {
            delay(5_000L)
            return base.nearest(point, radiusM)
        }
    }
}
