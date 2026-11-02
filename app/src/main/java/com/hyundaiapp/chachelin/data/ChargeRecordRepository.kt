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
    /** [vehicleId]는 가명 ID(서버 `vehicle_hash`). 다시 시도해도 같은 세션 id면 한 건만 남아야 한다 */
    suspend fun upload(record: ChargeSessionRecord, vehicleId: String)

    /** 인증 세션(SUCCESS)에만 1개 */
    suspend fun submitReview(sessionId: String, rating: Int, tags: Set<ReviewTag>)

    /** S5 [실패 아님]: 실패 기록 취소. 없는 세션이어도 오류가 아니다(다시 시도해도 안전해야 함) */
    suspend fun retract(sessionId: String)

    /** 동의 철회: 이 차량의 세션·리뷰를 모두 지운다 */
    suspend fun deleteMine(vehicleId: String)

    fun myRecords(): Flow<List<ChargeSessionRecord>>
}

class FakeChargeRecordRepository : ChargeRecordRepository {
    private val records = MutableStateFlow<List<ChargeSessionRecord>>(emptyList())
    val reviews = mutableMapOf<String, Pair<Int, Set<ReviewTag>>>()

    /** 테스트에서 어떤 차량 ID로 올라갔는지 확인용 */
    val uploadedVehicleIds = mutableListOf<String>()

    override suspend fun upload(record: ChargeSessionRecord, vehicleId: String) {
        uploadedVehicleIds += vehicleId
        records.update { listOf(record) + it }
    }

    override suspend fun submitReview(sessionId: String, rating: Int, tags: Set<ReviewTag>) {
        reviews[sessionId] = rating to tags
    }

    override suspend fun retract(sessionId: String) = records.update { list -> list.filterNot { it.id == sessionId } }

    // 이 저장소는 차량 한 대(이 기기) 기록만 들고 있다
    override suspend fun deleteMine(vehicleId: String) {
        records.value = emptyList()
        reviews.clear()
        uploadedVehicleIds.clear()
    }

    override fun myRecords(): Flow<List<ChargeSessionRecord>> = records.asStateFlow()
}
