package com.hyundaiapp.chachelin.domain

import com.hyundaiapp.chachelin.data.ChargeRecordRepository
import com.hyundaiapp.chachelin.data.ReviewTag
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 충전 흐름에서 지금 띄울 화면 */
enum class ChargeScreenTarget { NONE, CHARGING, REVIEW, FAILURE }

/**
 * 끝난 세션의 업로드·재시도와 S4/S5에서 처리할 결과를 맡는다. 담당: 이상준.
 *
 * 화면은 기다리지 않는다: 리뷰·취소는 여기에 맡기면 네트워크가 돌아올 때까지 다시 시도한다(3초 평가, PRD F5).
 * 재시도 대기열은 메모리에만 있어서 앱 프로세스가 죽으면 사라진다.
 */
class ChargeSessionRecorder(
    private val tracker: ChargeSessionTracker,
    private val records: ChargeRecordRepository,
    private val vehicleId: () -> String,
    /** 재시도 간격. 끝까지 쓰면 마지막 값으로 계속 시도한다 */
    private val retryDelaysMs: List<Long> = listOf(5_000L, 15_000L, 60_000L, 300_000L),
) {
    private val _pendingResult = MutableStateFlow<ChargeSessionRecord?>(null)
    private val uploads = mutableMapOf<String, Job>()
    private lateinit var scope: CoroutineScope

    /** S4(SUCCESS) 또는 S5(FAILED)에서 사용자가 아직 처리하지 않은 세션 */
    val pendingResult: StateFlow<ChargeSessionRecord?> = _pendingResult.asStateFlow()

    /** 내비게이션용. 새 세션이 시작되면 처리하지 않은 결과보다 충전 화면이 먼저다 */
    val target: Flow<ChargeScreenTarget> = combine(tracker.live, _pendingResult) { live, pending ->
        when {
            live != null -> ChargeScreenTarget.CHARGING
            pending?.result == ChargeResult.SUCCESS -> ChargeScreenTarget.REVIEW
            pending?.result == ChargeResult.FAILED -> ChargeScreenTarget.FAILURE
            else -> ChargeScreenTarget.NONE
        }
    }.distinctUntilChanged()

    /** [ChargeSessionTracker.start]보다 먼저 부른다(completed는 지난 값을 다시 주지 않음) */
    fun start(scope: CoroutineScope) {
        this.scope = scope
        scope.launch { tracker.completed.collect(::onCompleted) }
    }

    /** 기록을 멈출 때 부른다. 처리하지 않은 결과 화면을 닫는다 */
    fun reset() {
        uploads.clear()
        _pendingResult.value = null
    }

    private fun onCompleted(record: ChargeSessionRecord) {
        val id = vehicleId()
        uploads[record.id] = scope.launch { retrying { records.upload(record, id) } }
        if (record.result != ChargeResult.NOT_AT_STATION) _pendingResult.value = record
    }

    /** S4 [완료]. 세션 업로드가 끝난 뒤에 리뷰를 보낸다(서버 reviews가 세션을 참조) */
    fun submitReview(sessionId: String, rating: Int, tags: Set<ReviewTag>) {
        dismiss(sessionId)
        val upload = uploads[sessionId]
        scope.launch {
            upload?.join()
            retrying { records.submitReview(sessionId, rating, tags) }
        }
    }

    /** S5 [실패 아님]. 이미 업로드 중일 수 있으므로 업로드를 멈춘 뒤 서버에서도 지운다 */
    fun retractFailure(sessionId: String) {
        dismiss(sessionId)
        val upload = uploads[sessionId]
        scope.launch {
            upload?.cancelAndJoin()
            retrying { records.retract(sessionId) }
        }
    }

    /** S4 [나중에], S5 [확인]. 세션 기록은 그대로 두고 화면만 닫는다 */
    fun dismiss(sessionId: String) {
        _pendingResult.update { if (it?.id == sessionId) null else it }
    }

    private suspend fun retrying(block: suspend () -> Unit) {
        var attempt = 0
        while (true) {
            try {
                block()
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                delay(retryDelaysMs[attempt.coerceAtMost(retryDelaysMs.lastIndex)])
                attempt++
            }
        }
    }
}
