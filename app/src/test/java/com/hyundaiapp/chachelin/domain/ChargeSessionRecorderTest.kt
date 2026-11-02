package com.hyundaiapp.chachelin.domain

import com.hyundaiapp.chachelin.data.ChargeRecordRepository
import com.hyundaiapp.chachelin.data.FakeChargeRecordRepository
import com.hyundaiapp.chachelin.data.FakeStationRepository
import com.hyundaiapp.chachelin.data.ReviewTag
import com.hyundaiapp.core.FakeVehicleSignals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChargeSessionRecorderTest {
    private val fake = FakeVehicleSignals()
    private var now = 1_000_000L
    private val base = FakeChargeRecordRepository()
    private val records = FlakyRecords(base)

    /** 앞의 [failures]번은 네트워크 오류로 실패한다. 호출 순서를 남긴다 */
    private class FlakyRecords(private val base: FakeChargeRecordRepository) : ChargeRecordRepository by base {
        var failures = 0
        val calls = mutableListOf<String>()

        private suspend fun attempt(name: String, block: suspend () -> Unit) {
            calls += name
            if (failures > 0) {
                failures--
                error("네트워크 오류")
            }
            block()
        }

        override suspend fun upload(record: ChargeSessionRecord, vehicleId: String) = attempt("upload") { base.upload(record, vehicleId) }
        override suspend fun submitReview(sessionId: String, rating: Int, tags: Set<ReviewTag>) =
            attempt("review") { base.submitReview(sessionId, rating, tags) }
        override suspend fun retract(sessionId: String) = attempt("retract") { base.retract(sessionId) }
    }

    private fun run(body: suspend TestScope.(ChargeSessionRecorder) -> Unit) = runTest(UnconfinedTestDispatcher()) {
        val tracker = ChargeSessionTracker(fake, FakeStationRepository(), ChargeRules.DEFAULT, clock = { now })
        val recorder = ChargeSessionRecorder(tracker, records, { "vehicle-1" }, retryDelaysMs = listOf(1_000L, 2_000L))
        recorder.start(backgroundScope)
        tracker.start(backgroundScope)
        runCurrent()
        body(recorder)
    }

    private fun TestScope.session(socEnd: Int, durationMs: Long = 30 * 60_000L) {
        fake.setEv { copy(chargePortOpen = false, socPercent = 40) }
        runCurrent()
        fake.setEv { copy(chargePortOpen = true) }
        runCurrent()
        now += durationMs
        fake.setEv { copy(chargePortOpen = false, socPercent = socEnd) }
        runCurrent()
    }

    private suspend fun uploaded() = base.myRecords().first()

    @Test
    fun `인증 충전은 업로드하고 리뷰 화면으로 보낸다`() = run { recorder ->
        session(socEnd = 80)
        assertEquals(ChargeResult.SUCCESS, uploaded().single().result)
        assertEquals(listOf("vehicle-1"), base.uploadedVehicleIds)
        assertEquals(ChargeResult.SUCCESS, recorder.pendingResult.value?.result)
        assertEquals(ChargeScreenTarget.REVIEW, recorder.target.first())
    }

    @Test
    fun `실패는 업로드하고 실패 알림으로 보낸다`() = run { recorder ->
        session(socEnd = 40)
        assertEquals(ChargeResult.FAILED, uploaded().single().result)
        assertEquals(ChargeScreenTarget.FAILURE, recorder.target.first())
    }

    @Test
    fun `충전소 밖 기록은 업로드만 하고 화면을 띄우지 않는다`() = run { recorder ->
        fake.setLocation(37.5, 127.0)
        session(socEnd = 80)
        assertEquals(ChargeResult.NOT_AT_STATION, uploaded().single().result)
        assertNull(recorder.pendingResult.value)
        assertEquals(ChargeScreenTarget.NONE, recorder.target.first())
    }

    @Test
    fun `충전구가 열리면 처리하지 않은 결과보다 충전 화면이 먼저다`() = run { recorder ->
        session(socEnd = 80)
        fake.setEv { copy(chargePortOpen = true) }
        runCurrent()
        assertEquals(ChargeScreenTarget.CHARGING, recorder.target.first())
    }

    @Test
    fun `업로드가 실패하면 간격을 두고 다시 시도한다`() = run { _ ->
        records.failures = 2
        session(socEnd = 80)
        assertTrue(uploaded().isEmpty())
        advanceTimeBy(1_001L)
        assertTrue(uploaded().isEmpty())
        advanceTimeBy(2_000L)
        assertEquals(1, uploaded().size)
        assertEquals(3, records.calls.size)
    }

    @Test
    fun `리뷰는 세션 업로드가 끝난 뒤에 보낸다`() = run { recorder ->
        records.failures = 1
        session(socEnd = 80)
        val id = recorder.pendingResult.value!!.id
        recorder.submitReview(id, 5, setOf(ReviewTag.FAST))
        runCurrent()

        assertNull(recorder.pendingResult.value)
        assertEquals(listOf("upload"), records.calls)
        advanceTimeBy(1_001L)
        assertEquals(listOf("upload", "upload", "review"), records.calls)
        assertEquals(5 to setOf(ReviewTag.FAST), base.reviews[id])
    }

    @Test
    fun `나중에는 세션 기록을 두고 리뷰를 보내지 않는다`() = run { recorder ->
        session(socEnd = 80)
        recorder.dismiss(recorder.pendingResult.value!!.id)
        assertNull(recorder.pendingResult.value)
        assertEquals(1, uploaded().size)
        assertTrue(base.reviews.isEmpty())
    }

    @Test
    fun `실패 아님은 기록을 지운다`() = run { recorder ->
        session(socEnd = 40)
        recorder.retractFailure(recorder.pendingResult.value!!.id)
        runCurrent()
        assertNull(recorder.pendingResult.value)
        assertTrue(uploaded().isEmpty())
    }

    @Test
    fun `업로드 재시도 중에 실패 아님을 누르면 다시 올라가지 않는다`() = run { recorder ->
        records.failures = 1
        session(socEnd = 40)
        recorder.retractFailure(recorder.pendingResult.value!!.id)
        advanceTimeBy(10_000L)
        assertEquals(listOf("upload", "retract"), records.calls)
        assertTrue(uploaded().isEmpty())
    }

    @Test
    fun `기록을 멈추면 처리하지 않은 결과 화면을 닫는다`() = run { recorder ->
        session(socEnd = 80)
        recorder.reset()
        assertEquals(ChargeScreenTarget.NONE, recorder.target.first())
    }

    @Test
    fun `다른 세션 id로 닫아도 지금 결과는 그대로다`() = run { recorder ->
        session(socEnd = 80)
        recorder.dismiss("other")
        assertEquals(ChargeResult.SUCCESS, recorder.pendingResult.value?.result)
    }
}
