package com.hyundaiapp.chachelin.data

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

/** 충전소·점수 조회. 실제 구현(서버)은 황준용, 그 전에는 [FakeStationRepository] */
interface StationRepository {
    suspend fun nearby(center: GeoPoint, radiusM: Double): List<StationWithScore>
    suspend fun get(stationId: String): StationWithScore?

    /** 반경 안에서 가장 가까운 충전소. 충전 세션을 충전소와 묶을 때 쓴다 */
    suspend fun nearest(point: GeoPoint, radiusM: Double): Station?
}

/** 시드 데이터(판교 부근). PRD 5.5: 시연에서 시드 데이터임을 밝힌다 */
class FakeStationRepository(
    private val data: List<StationWithScore> = SEED,
) : StationRepository {
    override suspend fun nearby(center: GeoPoint, radiusM: Double) =
        data.filter { GeoMath.distanceM(center, it.station.point) <= radiusM }
            .sortedByDescending { it.score.chachelinScore ?: -1.0 }

    override suspend fun get(stationId: String) = data.firstOrNull { it.station.id == stationId }

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

        private fun seed(
            id: String, name: String, op: String, lat: Double, lon: Double, maxKw: Int,
            score: Double?, verified: Int, avgKw: Double, failures: Int,
        ) = StationWithScore(
            Station(id, name, op, GeoPoint(lat, lon), maxKw),
            StationScore(score, score, if (score == null) 3 else verified / 3, verified, avgKw, failures),
        )
    }
}
