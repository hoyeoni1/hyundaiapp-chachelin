package com.hyundaiapp.chachelin.ui

import com.hyundaiapp.chachelin.data.FakeChargeRecordRepository
import com.hyundaiapp.chachelin.data.FakeStationRepository
import com.hyundaiapp.chachelin.data.FakeVehicleProfileStore
import com.hyundaiapp.chachelin.domain.ChargeResult
import com.hyundaiapp.chachelin.domain.ChargeScreenTarget
import com.hyundaiapp.chachelin.domain.ChargeSessionRecord
import com.hyundaiapp.chachelin.ui.home.HomeViewModel
import com.hyundaiapp.chachelin.ui.onboarding.OnboardingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 11-09 시연 피드백: 동의 화면 재진입(4번), 홈 화면(3번) */
@OptIn(ExperimentalCoroutinesApi::class)
class DemoFeedbackTest {
    private val profile = FakeVehicleProfileStore()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    // 4. 동의 화면에 다시 들어오면 동의를 다시 받지 않는다

    @Test
    fun `동의를 마친 뒤 다시 들어오면 저장된 동의와 차종을 보여 준다`() {
        profile.completeOnboarding(64.8f, presetIndex = 3)
        val vm = OnboardingViewModel(profile)
        vm.reload()
        val state = vm.state.value
        assertTrue(state.editing)
        assertTrue(state.locationAgreed && state.privacyAgreed)
        assertEquals(3, state.selectedPreset)
    }

    @Test
    fun `설정에서 차종을 바꾸면 용량과 차종이 바뀌고 동의는 그대로다`() {
        profile.completeOnboarding(77.4f, presetIndex = 0)
        var started = 0
        val vm = OnboardingViewModel(profile, onStarted = { started++ })
        vm.reload()
        vm.onSelectPreset(3)
        assertTrue(vm.onStart())
        assertEquals(64.8f, profile.capacityKwh!!, 0.001f)
        assertEquals(3, profile.presetIndex)
        assertTrue(profile.onboarded.value)
        assertEquals(1, started)
    }

    @Test
    fun `차종 순서를 저장하기 전에 동의했으면 용량이 같은 차종으로 보여 준다`() {
        profile.completeOnboarding(64.8f)
        val vm = OnboardingViewModel(profile)
        vm.reload()
        assertEquals(3, vm.state.value.selectedPreset)
    }

    @Test
    fun `처음이거나 철회한 뒤에는 빈 동의 화면이다`() {
        val vm = OnboardingViewModel(profile)
        vm.reload()
        assertFalse(vm.state.value.editing)
        assertFalse(vm.state.value.locationAgreed)
        assertNull(vm.state.value.selectedPreset)

        profile.completeOnboarding(77.4f, presetIndex = 0)
        profile.clear()
        vm.reload()
        assertFalse(vm.state.value.editing)
        assertNull(profile.presetIndex)
    }

    // 3. 홈 화면

    @Test
    fun `홈은 충전 상태와 내 차와 최근 기록을 보여 준다`() = runTest {
        val records = FakeChargeRecordRepository()
        val target = MutableStateFlow(ChargeScreenTarget.NONE)
        profile.completeOnboarding(77.4f, presetIndex = 0)
        val vm = HomeViewModel(target, records, FakeStationRepository(), profile)
        assertEquals(HomeChargeStatus.IDLE, vm.state.value.charge)
        assertEquals("아이오닉 5", vm.state.value.carLabel)
        assertEquals(0, vm.state.value.recordCount)

        target.value = ChargeScreenTarget.REVIEW
        records.upload(ChargeSessionRecord("a", "st-001", 1_000L, 61_000L, 40, 80, 60.0, ChargeResult.SUCCESS), "v")
        records.upload(ChargeSessionRecord("b", null, 2_000L, 62_000L, 50, 70, 40.0, ChargeResult.NOT_AT_STATION), "v")
        runCurrent()

        val state = vm.state.value
        assertEquals(HomeChargeStatus.REVIEW, state.charge)
        assertEquals(2, state.recordCount)
        assertEquals("b", state.lastRecord?.sessionId)
        assertNull(state.lastRecord?.stationName)
    }

    @Test
    fun `충전 흐름 상태가 홈 상태로 바뀐다`() = runTest {
        val target = MutableStateFlow(ChargeScreenTarget.CHARGING)
        val vm = HomeViewModel(target, FakeChargeRecordRepository(), FakeStationRepository(), profile)
        assertEquals(HomeChargeStatus.CHARGING, vm.state.value.charge)
        target.value = ChargeScreenTarget.FAILURE
        assertEquals(HomeChargeStatus.FAILURE, vm.state.value.charge)
        target.value = ChargeScreenTarget.NONE
        assertEquals(HomeChargeStatus.IDLE, vm.state.value.charge)
    }
}
