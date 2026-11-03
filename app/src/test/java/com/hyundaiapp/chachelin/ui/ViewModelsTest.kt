package com.hyundaiapp.chachelin.ui

import com.hyundaiapp.chachelin.data.FakeChargeRecordRepository
import com.hyundaiapp.chachelin.data.FakeStationRepository
import com.hyundaiapp.chachelin.data.FakeVehicleProfileStore
import com.hyundaiapp.chachelin.data.ReviewTag
import com.hyundaiapp.chachelin.domain.ChargeResult
import com.hyundaiapp.chachelin.domain.ChargeRules
import com.hyundaiapp.chachelin.domain.ChargeSessionRecord
import com.hyundaiapp.chachelin.domain.ChargeSessionRecorder
import com.hyundaiapp.chachelin.domain.ChargeSessionTracker
import com.hyundaiapp.chachelin.domain.LiveCharge
import com.hyundaiapp.chachelin.domain.VehiclePreset
import com.hyundaiapp.chachelin.ui.charging.ChargingViewModel
import com.hyundaiapp.chachelin.ui.history.HistoryViewModel
import com.hyundaiapp.chachelin.ui.onboarding.OnboardingViewModel
import com.hyundaiapp.chachelin.ui.review.FailureNoticeViewModel
import com.hyundaiapp.chachelin.ui.review.ReviewViewModel
import com.hyundaiapp.chachelin.ui.stations.StationDetailViewModel
import com.hyundaiapp.core.FakeVehicleSignals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {
    private val fake = FakeVehicleSignals()
    private val stations = FakeStationRepository()
    private val records = FakeChargeRecordRepository()
    private var now = 1_000_000L

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.recorder(): ChargeSessionRecorder {
        val tracker = ChargeSessionTracker(fake, stations, ChargeRules.DEFAULT, presetCapacityKwh = { 77.4f }, clock = { now })
        val recorder = ChargeSessionRecorder(tracker, records, { "vehicle-1" })
        recorder.start(backgroundScope)
        tracker.start(backgroundScope)
        runCurrent()
        return recorder
    }

    private fun TestScope.session(socEnd: Int) {
        fake.setEv { copy(chargePortOpen = false, socPercent = 40) }
        runCurrent()
        fake.setEv { copy(chargePortOpen = true) }
        runCurrent()
        now += 32 * 60_000L
        fake.setEv { copy(chargePortOpen = false, socPercent = socEnd) }
        runCurrent()
    }

    // S2

    @Test
    fun `상세는 충전소 점수와 분포를 보여 준다`() = runTest {
        val vm = StationDetailViewModel("st-001", fake, fake, stations).also { it.refresh() }
        val state = vm.state.value
        assertEquals("판교 테크노밸리 E-pit", state.station?.name)
        assertEquals(42, state.ratingCounts.sum())
        assertTrue(ReviewTag.FAST in state.topTags)
        assertNotNull(state.station?.distanceM)
    }

    @Test
    fun `시드의 실패 수만큼 최근 7일 안의 실패 시각을 보여 준다`() = runTest {
        val state = StationDetailViewModel("st-003", fake, fake, stations).also { it.refresh() }.state.value
        assertEquals(4, state.recentFailureTimesMs.size)
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 3_600_000L
        assertTrue(state.recentFailureTimesMs.all { it > sevenDaysAgo })
    }

    @Test
    fun `없는 충전소면 station이 null로 남는다`() = runTest {
        assertNull(StationDetailViewModel("nope", fake, fake, stations).also { it.refresh() }.state.value.station)
    }

    @Test
    fun `길안내는 확인한 뒤에만 경로를 요청한다`() = runTest {
        val vm = StationDetailViewModel("st-001", fake, fake, stations).also { it.refresh() }
        vm.onRouteClick()
        assertTrue(vm.state.value.confirmRoute)
        assertTrue(fake.requestedRoutes.isEmpty())

        vm.onConfirmRoute()
        assertFalse(vm.state.value.confirmRoute)
        assertEquals("판교 테크노밸리 E-pit", fake.requestedRoutes.single().name)
        assertEquals(37.4020, fake.requestedRoutes.single().point.latitude, 1e-6)
    }

    @Test
    fun `길안내 취소는 경로를 건드리지 않는다`() = runTest {
        val vm = StationDetailViewModel("st-001", fake, fake, stations).also { it.refresh() }
        vm.onRouteClick()
        vm.onDismissRoute()
        assertFalse(vm.state.value.confirmRoute)
        assertTrue(fake.requestedRoutes.isEmpty())
    }

    // S3

    @Test
    fun `충전 중 화면은 1초마다 경과 시간을 갱신하고 충전소 이름을 보여 준다`() = runTest {
        val live = MutableStateFlow<LiveCharge?>(null)
        val vm = ChargingViewModel(live, stations, clock = { now })
        backgroundScope.launch { vm.state.collect {} }
        runCurrent()
        assertNull(vm.state.value)

        live.value = LiveCharge("st-001", now, 20, 54, 63.0)
        runCurrent()
        assertEquals("판교 테크노밸리 E-pit", vm.state.value?.stationName)
        assertEquals(0L, vm.state.value?.elapsedSec)

        now += 18 * 60_000L
        advanceTimeBy(1_001L)
        assertEquals(18 * 60L, vm.state.value?.elapsedSec)
        assertEquals(54, vm.state.value?.socNow)

        live.value = null
        runCurrent()
        assertNull(vm.state.value)
    }

    @Test
    fun `충전 중 화면은 시연 배속만큼 경과 시간을 곱한다`() = runTest {
        val live = MutableStateFlow<LiveCharge?>(LiveCharge(null, now, 40, 50, null))
        val vm = ChargingViewModel(live, stations, timeScale = 120, clock = { now })
        backgroundScope.launch { vm.state.collect {} }
        now += 15_000L
        advanceTimeBy(1_001L)
        assertEquals(30 * 60L, vm.state.value?.elapsedSec)
    }

    // S4

    @Test
    fun `리뷰 화면은 인증 세션을 보여 주고 완료하면 닫힌다`() = runTest {
        val recorder = recorder()
        val vm = ReviewViewModel(recorder, stations)
        assertNull(vm.state.value)

        session(socEnd = 80)
        val state = vm.state.value!!
        assertEquals("시흥동 주민센터", state.stationName)
        assertEquals(40, state.socStart)
        assertEquals(80, state.socEnd)
        assertEquals(32, state.durationMin)

        vm.onRate(5)
        vm.onToggleTag(ReviewTag.FAST)
        vm.onToggleTag(ReviewTag.CLEAN)
        vm.onToggleTag(ReviewTag.CLEAN)
        assertEquals(setOf(ReviewTag.FAST), vm.state.value?.selectedTags)

        vm.onSubmit()
        runCurrent()
        assertNull(vm.state.value)
        assertEquals(5 to setOf(ReviewTag.FAST), records.reviews[state.sessionId])
    }

    @Test
    fun `별점 없이는 리뷰를 보내지 않는다`() = runTest {
        val recorder = recorder()
        val vm = ReviewViewModel(recorder, stations)
        session(socEnd = 80)
        vm.onSubmit()
        runCurrent()
        assertNotNull(vm.state.value)
        assertTrue(records.reviews.isEmpty())
    }

    @Test
    fun `나중에는 리뷰 없이 닫고 세션 기록은 남긴다`() = runTest {
        val recorder = recorder()
        val vm = ReviewViewModel(recorder, stations)
        session(socEnd = 80)
        vm.onLater()
        assertNull(vm.state.value)
        assertTrue(records.reviews.isEmpty())
        assertEquals(1, records.myRecords().first().size)
    }

    @Test
    fun `리뷰 화면은 실패 세션에 반응하지 않는다`() = runTest {
        val recorder = recorder()
        val vm = ReviewViewModel(recorder, stations)
        session(socEnd = 40)
        assertNull(vm.state.value)
    }

    @Test
    fun `리뷰 화면은 시연 배속만큼 충전 시간을 곱한다`() = runTest {
        val recorder = recorder()
        val vm = ReviewViewModel(recorder, stations, timeScale = 2)
        session(socEnd = 80)
        assertEquals(64, vm.state.value?.durationMin)
    }

    // S5

    @Test
    fun `실패 알림의 실패 아님은 기록을 지운다`() = runTest {
        val recorder = recorder()
        val vm = FailureNoticeViewModel(recorder, stations)
        session(socEnd = 40)
        assertEquals("시흥동 주민센터", vm.state.value?.stationName)

        vm.onNotFailure()
        runCurrent()
        assertNull(vm.state.value)
        assertTrue(records.myRecords().first().isEmpty())
    }

    @Test
    fun `실패 알림의 확인은 기록을 남긴다`() = runTest {
        val recorder = recorder()
        val vm = FailureNoticeViewModel(recorder, stations)
        session(socEnd = 40)
        vm.onConfirm()
        assertNull(vm.state.value)
        assertEquals(ChargeResult.FAILED, records.myRecords().first().single().result)
    }

    // S6

    @Test
    fun `내 기록은 결과별로 표시하고 충전소 밖 기록은 이름이 없다`() = runTest {
        val vm = HistoryViewModel(records, stations)
        assertTrue(vm.state.value.items.isEmpty())

        records.upload(record("a", "st-001", ChargeResult.SUCCESS), "v")
        records.upload(record("b", "st-003", ChargeResult.FAILED), "v")
        records.upload(record("c", null, ChargeResult.NOT_AT_STATION), "v")
        runCurrent()

        val (c, b, a) = vm.state.value.items
        assertEquals("판교 테크노밸리 E-pit", a.stationName)
        assertTrue(a.verified && !a.failed)
        assertTrue(b.failed && !b.verified)
        assertNull(c.stationName)
        assertFalse(c.verified || c.failed)
    }

    // 시연 5·6단계: 내 기록이 충전소 점수에 바로 반영된다

    @Test
    fun `인증 충전과 리뷰가 S1 S2 숫자에 더해진다`() = runTest {
        val linked = FakeStationRepository(records = records)
        val before = linked.get("st-003")!!.score
        val beforeRatings = linked.detail("st-003")!!.ratingCounts
        records.upload(record("a", "st-003", ChargeResult.SUCCESS), "v")
        records.submitReview("a", 5, setOf(ReviewTag.CLEAN))

        val after = linked.get("st-003")!!.score
        assertEquals(before.verifiedCharges + 1, after.verifiedCharges)
        assertEquals(before.reviewCount + 1, after.reviewCount)
        assertTrue(after.chachelinScore!! > before.chachelinScore!!)
        val detail = linked.detail("st-003")!!
        assertEquals(beforeRatings[4] + 1, detail.ratingCounts[4])
        assertEquals(ReviewTag.CLEAN, detail.topTags.first())
    }

    @Test
    fun `리뷰 없이 인증 충전만 있어도 점수는 그대로 인증 수만 는다`() = runTest {
        val linked = FakeStationRepository(records = records)
        val before = linked.get("st-001")!!.score
        records.upload(record("a", "st-001", ChargeResult.SUCCESS), "v")
        val after = linked.get("st-001")!!.score
        assertEquals(before.verifiedCharges + 1, after.verifiedCharges)
        assertEquals(before.chachelinScore!!, after.chachelinScore!!, 1e-9)
    }

    @Test
    fun `실패가 최근 실패 수에 더해지고 실패 아님이면 되돌아간다`() = runTest {
        val clockNow = 10_000_000_000L
        val linked = FakeStationRepository(records = records, clock = { clockNow })
        val failed = ChargeSessionRecord("f", "st-001", clockNow - 60_000L, clockNow - 1_000L, 40, 40, null, ChargeResult.FAILED)
        records.upload(failed, "v")
        assertEquals(1, linked.get("st-001")!!.score.recentFailures7d)
        assertEquals(listOf(clockNow - 1_000L), linked.detail("st-001")!!.recentFailureTimesMs)

        records.retract("f")
        assertEquals(0, linked.get("st-001")!!.score.recentFailures7d)
        assertTrue(linked.detail("st-001")!!.recentFailureTimesMs.isEmpty())
    }

    @Test
    fun `평가 중인 충전소도 리뷰가 5개가 되면 점수가 생긴다`() = runTest {
        val linked = FakeStationRepository(records = records)
        assertNull(linked.get("st-004")!!.score.chachelinScore)
        listOf("a", "b").forEach {
            records.upload(record(it, "st-004", ChargeResult.SUCCESS), "v")
            records.submitReview(it, 5, emptySet())
        }
        assertNotNull(linked.get("st-004")!!.score.chachelinScore)
    }

    private fun record(id: String, stationId: String?, result: ChargeResult) =
        ChargeSessionRecord(id, stationId, 0L, 60_000L, 40, 80, 60.0, result)

    // S7

    @Test
    fun `동의 둘과 차종을 모두 골라야 저장한다`() {
        val profile = FakeVehicleProfileStore()
        var started = 0
        val vm = OnboardingViewModel(profile, onStarted = { started++ })
        assertEquals(VehiclePreset.ALL.map { it.label }, vm.state.value.vehiclePresets)

        vm.onSelectPreset(3)
        assertFalse(vm.onStart())
        assertFalse(profile.onboarded.value)

        vm.onAgreeLocation()
        assertFalse(vm.onStart())
        assertFalse(profile.onboarded.value)
        assertEquals(0, started)

        vm.onAgreePrivacy()
        assertTrue(vm.onStart())
        assertTrue(profile.onboarded.value)
        assertEquals(64.8f, profile.capacityKwh!!, 0.001f)
        assertEquals(1, started)
    }

    @Test
    fun `철회하면 동의와 차량 ID가 사라지고 다시 동의하면 새 ID를 쓴다`() {
        val profile = FakeVehicleProfileStore()
        val vm = OnboardingViewModel(profile)
        vm.onAgreeLocation()
        vm.onAgreePrivacy()
        vm.onSelectPreset(0)
        vm.onStart()
        val oldId = profile.vehicleId()

        profile.clear()
        vm.reload()
        assertFalse(profile.onboarded.value)
        assertNull(profile.capacityKwh)
        assertFalse(vm.state.value.locationAgreed || vm.state.value.privacyAgreed)
        assertNull(vm.state.value.selectedPreset)
        assertNotEquals(oldId, profile.vehicleId())
    }

    @Test
    fun `철회 삭제는 내 기록과 리뷰를 모두 지운다`() = runTest {
        val vm = HistoryViewModel(records, stations)
        records.upload(record("a", "st-001", ChargeResult.SUCCESS), "v")
        records.submitReview("a", 5, emptySet())
        records.deleteMine("v")
        runCurrent()
        assertTrue(vm.state.value.items.isEmpty())
        assertTrue(records.reviews.isEmpty())
    }

    @Test
    fun `차종을 고르지 않으면 동의해도 저장하지 않는다`() {
        val profile = FakeVehicleProfileStore()
        val vm = OnboardingViewModel(profile)
        vm.onAgreeLocation()
        vm.onAgreePrivacy()
        vm.onStart()
        assertFalse(profile.onboarded.value)
    }
}
