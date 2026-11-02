package com.hyundaiapp.chachelin.data

import com.hyundaiapp.chachelin.domain.ChargeResult
import com.hyundaiapp.core.GeoMath
import com.hyundaiapp.core.GeoPoint

/** 충전소 기본 정보(환경부 API 캐시, 서버 `stations`) */
data class Station(
    val id: String,
    val name: String,
    val operator: String,
    val point: GeoPoint,
    val maxKw: Int,
)

/** 서버 `station_scores` 뷰 한 행 */
data class StationScore(
    /** 리뷰 5개 미만이면 null("평가 중") */
    val chachelinScore: Double?,
    val avgRating: Double?,
    val reviewCount: Int,
    val verifiedCharges: Int,
    val avgKw: Double?,
    val recentFailures7d: Int,
)

data class StationWithScore(val station: Station, val score: StationScore)

/** S2 상세에만 필요한 집계 */
data class StationDetail(
    /** 별점 1~5 개수(인덱스 0 = 1점) */
    val ratingCounts: List<Int>,
    val topTags: List<ReviewTag>,
    /** 최근 7일 실패 시각, 최신이 먼저 */
    val recentFailureTimesMs: List<Long>,
)

/** 충전소·점수 조회. 실제 구현(서버)은 황준용, 그 전에는 [FakeStationRepository] */
interface StationRepository {
    suspend fun nearby(center: GeoPoint, radiusM: Double): List<StationWithScore>
    suspend fun get(stationId: String): StationWithScore?
    suspend fun detail(stationId: String): StationDetail?

    /** 반경 안에서 가장 가까운 충전소. 충전 세션을 충전소와 묶을 때 쓴다 */
    suspend fun nearest(point: GeoPoint, radiusM: Double): Station?
}

/** 화면 표시용 이름. 못 가져와도 화면을 막지 않는다 */
suspend fun StationRepository.stationName(stationId: String): String? =
    runCatching { get(stationId)?.station?.name }.getOrNull()

/** 시드 데이터(판교 부근). PRD 5.5: 시연에서 시드 데이터임을 밝힌다 */
class FakeStationRepository(
    private val data: List<StationWithScore> = SEED,
    private val clock: () -> Long = System::currentTimeMillis,
    /** 있으면 이 차의 기록을 점수에 바로 더한다(서버의 집계 뷰 대신. PRD 7절 시연 5·6단계) */
    private val records: FakeChargeRecordRepository? = null,
) : StationRepository {
    override suspend fun nearby(center: GeoPoint, radiusM: Double) =
        data.filter { GeoMath.distanceM(center, it.station.point) <= radiusM }
            .map { withMine(it) }
            .sortedByDescending { it.score.chachelinScore ?: -1.0 }

    override suspend fun get(stationId: String) = data.firstOrNull { it.station.id == stationId }?.let { withMine(it) }

    override suspend fun detail(stationId: String): StationDetail? {
        val base = data.firstOrNull { it.station.id == stationId } ?: return null
        val seedFailures = List(base.score.recentFailures7d) { clock() - (it + 1) * FAILURE_SPACING_MS }
        val myTags = myReviews(stationId).flatMap { it.second }.distinct()
        return StationDetail(
            ratingCounts = ratingCounts(stationId),
            topTags = (myTags + SEED_TAGS[stationId].orEmpty()).distinct().take(MAX_TOP_TAGS),
            recentFailureTimesMs = (myRecentFailures(stationId).map { it.endedAtMs } + seedFailures).sortedDescending(),
        )
    }

    private fun withMine(base: StationWithScore): StationWithScore {
        val id = base.station.id
        val verified = records?.sessionsAt(id).orEmpty().filter { it.result == ChargeResult.SUCCESS }
        val failures = myRecentFailures(id).size
        val reviews = myReviews(id).size
        if (verified.isEmpty() && failures == 0) return base
        val kwSamples = verified.mapNotNull { it.avgKw }
        val seedKwSum = (base.score.avgKw ?: 0.0) * base.score.verifiedCharges
        val seedAverage = average(SEED_RATINGS[id].orEmpty())
        val average = average(ratingCounts(id))
        val reviewCount = base.score.reviewCount + reviews
        return base.copy(
            score = base.score.copy(
                // 시드 점수에 평균 별점 변화만큼 더한다. 리뷰 5개 미만이면 "평가 중"
                chachelinScore = if (reviewCount < MIN_REVIEWS) null else (base.score.chachelinScore ?: seedAverage) + average - seedAverage,
                avgRating = average,
                reviewCount = reviewCount,
                verifiedCharges = base.score.verifiedCharges + verified.size,
                avgKw = (seedKwSum + kwSamples.sum()) / (base.score.verifiedCharges + kwSamples.size).coerceAtLeast(1),
                recentFailures7d = base.score.recentFailures7d + failures,
            ),
        )
    }

    private fun myReviews(stationId: String) =
        records?.sessionsAt(stationId).orEmpty().mapNotNull { records?.reviews?.get(it.id) }

    private fun myRecentFailures(stationId: String) = records?.sessionsAt(stationId).orEmpty()
        .filter { it.result == ChargeResult.FAILED && it.endedAtMs >= clock() - FAILURE_WINDOW_MS }

    private fun ratingCounts(stationId: String): List<Int> {
        val counts = (SEED_RATINGS[stationId] ?: List(5) { 0 }).toMutableList()
        myReviews(stationId).forEach { (rating, _) -> counts[rating - 1]++ }
        return counts
    }

    private fun average(counts: List<Int>): Double =
        counts.withIndex().sumOf { (i, n) -> (i + 1) * n }.toDouble() / counts.sum().coerceAtLeast(1)

    override suspend fun nearest(point: GeoPoint, radiusM: Double) =
        data.map { it.station }
            .map { it to GeoMath.distanceM(point, it.point) }
            .filter { it.second <= radiusM }
            .minByOrNull { it.second }?.first

    companion object {
        val SEED = listOf(
            seed("st-001", "판교 테크노밸리 E-pit", "현대자동차(E-pit)", 37.4020, 127.1086, 350, 4.8, 128, 68.0, 0),
            seed("st-002", "판교역 공영주차장", "환경부", 37.3947, 127.1112, 100, 4.1, 64, 52.0, 1),
            seed("st-003", "시흥동 주민센터", "한국전력공사", 37.4127, 127.0947, 50, 3.2, 21, 31.0, 4),
            seed("st-004", "대왕판교IC 휴게소", "GS차지비", 37.4180, 127.1020, 200, null, 3, 88.0, 0),
        )

        // 개수는 SEED의 리뷰 수·평균 별점과 맞춘다
        private val SEED_RATINGS = mapOf(
            "st-001" to listOf(0, 0, 1, 6, 35),
            "st-002" to listOf(0, 1, 4, 8, 8),
            "st-003" to listOf(1, 1, 2, 2, 1),
            "st-004" to listOf(0, 0, 1, 1, 1),
        )
        private val SEED_TAGS = mapOf(
            "st-001" to listOf(ReviewTag.FAST, ReviewTag.NO_WAIT, ReviewTag.CLEAN),
            "st-002" to listOf(ReviewTag.CLEAN, ReviewTag.WAIT),
            "st-003" to listOf(ReviewTag.BROKEN_SLOT, ReviewTag.SLOW),
            "st-004" to listOf(ReviewTag.FAST),
        )

        /** 실패 4건까지 7일 안에 들어가는 간격(36시간) */
        private const val FAILURE_SPACING_MS = 36 * 3_600_000L
        private const val FAILURE_WINDOW_MS = 7 * 24 * 3_600_000L
        private const val MIN_REVIEWS = 5
        private const val MAX_TOP_TAGS = 3

        private fun seed(
            id: String, name: String, op: String, lat: Double, lon: Double, maxKw: Int,
            score: Double?, verified: Int, avgKw: Double, failures: Int,
        ) = StationWithScore(
            Station(id, name, op, GeoPoint(lat, lon), maxKw),
            StationScore(score, score, if (score == null) 3 else verified / 3, verified, avgKw, failures),
        )
    }
}
