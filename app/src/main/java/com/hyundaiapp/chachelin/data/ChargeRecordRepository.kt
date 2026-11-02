package com.hyundaiapp.chachelin.data

import com.hyundaiapp.chachelin.domain.ChargeSessionRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 리뷰 태그 프리셋(S4). 서버 `reviews.tags`에 이 코드로 저장 */
enum class ReviewTag(val label: String) {
    FAST("빠름"), SLOW("느림"), NO_WAIT("대기 없음"), WAIT("대기 있음"), BROKEN_SLOT("고장 칸 있음"), CLEAN("깨끗함"),
}

/** 세션 업로드·리뷰 저장·내 기록. 실제 구현(서버)은 황준용 */
interface ChargeRecordRepository {
    suspend fun upload(record: ChargeSessionRecord)

    /** 인증 세션(SUCCESS)에만 1개 */
    suspend fun submitReview(sessionId: String, rating: Int, tags: Set<ReviewTag>)

    fun myRecords(): Flow<List<ChargeSessionRecord>>
}

class FakeChargeRecordRepository : ChargeRecordRepository {
    private val records = MutableStateFlow<List<ChargeSessionRecord>>(emptyList())
    val reviews = mutableMapOf<String, Pair<Int, Set<ReviewTag>>>()

    override suspend fun upload(record: ChargeSessionRecord) = records.update { listOf(record) + it }

    override suspend fun submitReview(sessionId: String, rating: Int, tags: Set<ReviewTag>) {
        reviews[sessionId] = rating to tags
    }

    override fun myRecords(): Flow<List<ChargeSessionRecord>> = records.asStateFlow()
}
