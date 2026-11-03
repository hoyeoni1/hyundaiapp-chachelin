package com.hyundaiapp.chachelin.data

import com.hyundaiapp.chachelin.domain.ChargeResult
import com.hyundaiapp.chachelin.domain.ChargeSessionRecord
import com.hyundaiapp.core.GeoMath
import com.hyundaiapp.core.GeoPoint
import com.hyundaiapp.core.backend.SupabaseBackend
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

private fun JSONObject.nullableDouble(key: String): Double? = if (isNull(key)) null else getDouble(key)
private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)

class SupabaseStationRepository(
    private val backend: SupabaseBackend,
    private val vehicleId: () -> String,
    private val consented: () -> Boolean,
) : StationRepository {
    private val cache = mutableMapOf<String, StationWithScore>()
    private fun query() = JSONObject().put("p_demo", backend.config.simulated)
    private fun decode(json: JSONObject): StationWithScore = StationWithScore(
        Station(json.getString("id"), json.getString("name"), json.getString("operator"),
            GeoPoint(json.getDouble("latitude"), json.getDouble("longitude")), json.getDouble("max_kw").toInt()),
        StationScore(json.nullableDouble("chachelin_score"), json.nullableDouble("avg_rating"), json.getInt("review_count"),
            json.getInt("verified_charges"), json.nullableDouble("avg_kw"), json.getInt("recent_failures_7d")),
    ).also { cache[it.station.id] = it }

    override suspend fun nearby(center: GeoPoint, radiusM: Double): List<StationWithScore> {
        if (!consented()) return emptyList()
        val params = query().put("p_lat", center.latitude).put("p_lon", center.longitude).put("p_radius_m", radiusM)
        val array = JSONArray(backend.query(vehicleId(), "pleos_nearby_stations", params))
        return (0 until array.length()).map { decode(array.getJSONObject(it)) }
    }

    private suspend fun detailJson(stationId: String): JSONObject? {
        if (!consented()) return null
        val raw = backend.query(vehicleId(), "pleos_station_detail", query().put("p_station_id", stationId))
        return if (raw.trim() == "null") null else JSONObject(raw)
    }
    override suspend fun get(stationId: String): StationWithScore? = detailJson(stationId)?.let { decode(it.getJSONObject("station")) }
    override suspend fun detail(stationId: String): StationDetail? = detailJson(stationId)?.let { json ->
        StationDetail(
            json.getJSONArray("rating_counts").let { a -> (0 until a.length()).map(a::getInt) },
            json.getJSONArray("top_tags").let { a -> (0 until a.length()).map { ReviewTag.valueOf(a.getString(it)) } },
            json.getJSONArray("recent_failure_hour_ms").let { a -> (0 until a.length()).map(a::getLong) },
        )
    }
    override suspend fun nearest(point: GeoPoint, radiusM: Double): Station? {
        val candidates = try { nearby(point, radiusM) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { cache.values.toList() }
        return candidates.map { it.station }.filter { GeoMath.distanceM(point, it.point) <= radiusM }
            .minByOrNull { GeoMath.distanceM(point, it.point) }
    }
}

class SupabaseChargeRecordRepository(
    private val backend: SupabaseBackend,
    private val vehicleId: () -> String,
    private val local: LocalChargeRecords,
    private val consented: () -> Boolean,
) : ChargeRecordRepository {
    private var cachedOwner: String? = null
    private var cachedRecords = emptyList<ChargeSessionRecord>()

    override suspend fun upload(record: ChargeSessionRecord, vehicleId: String) {
        if (record.stationId == null || record.result == ChargeResult.NOT_AT_STATION) {
            check(consented() && this.vehicleId() == vehicleId) { "Explicit current consent required" }
            local.put(vehicleId, record)
            return
        }
        backend.enqueue(vehicleId, "pleos_upload_charge", "charge:${record.id}", JSONObject()
            .put("p_id", record.id).put("p_station_id", record.stationId ?: JSONObject.NULL)
            .put("p_started_at", Instant.ofEpochMilli(record.startedAtMs).toString())
            .put("p_ended_at", Instant.ofEpochMilli(record.endedAtMs).toString())
            .put("p_soc_start", record.socStart).put("p_soc_end", record.socEnd)
            .put("p_avg_kw", record.avgKw ?: JSONObject.NULL).put("p_result", record.result.name)
            .put("p_time_scale", if (backend.config.simulated) 120 else 1))
    }
    override suspend fun submitReview(sessionId: String, rating: Int, tags: Set<ReviewTag>) {
        require(!local.contains(vehicleId(), sessionId)) { "Local-only sessions cannot be reviewed" }
        backend.enqueue(vehicleId(), "pleos_submit_review", "review:$sessionId", JSONObject()
            .put("p_session_id", sessionId).put("p_rating", rating).put("p_tags", JSONArray(tags.map { it.name }.sorted())))
    }
    override suspend fun retract(sessionId: String) {
        if (local.retract(vehicleId(), sessionId)) return
        backend.enqueue(vehicleId(), "pleos_retract_failure", "retract:$sessionId", JSONObject().put("p_session_id", sessionId))
    }
    override suspend fun deleteMine(vehicleId: String) {
        local.clear(vehicleId)
        backend.withdraw(vehicleId)
        backend.awaitWithdrawal(vehicleId)
    }

    override fun myRecords(): Flow<List<ChargeSessionRecord>> = combine(remoteRecords(), local.revision) { remote, _ ->
        if (!consented()) emptyList()
        else (local.list(vehicleId()) + remote).distinctBy { it.id }
            .sortedWith(compareByDescending<ChargeSessionRecord> { it.endedAtMs }.thenByDescending { it.id })
    }.distinctUntilChanged()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun remoteRecords(): Flow<List<ChargeSessionRecord>> = backend.revision.mapLatest {
        if (!consented()) { cachedOwner = null; cachedRecords = emptyList() }
        else {
            val owner = vehicleId()
            if (cachedOwner != owner) { cachedOwner = owner; cachedRecords = emptyList() }
            try {
                val records = mutableListOf<ChargeSessionRecord>()
                var cursor: ChargeSessionRecord? = null
                do {
                    val params = JSONObject().put("p_limit", 200)
                    cursor?.let { params.put("p_before", Instant.ofEpochMilli(it.endedAtMs).toString()).put("p_before_id", it.id) }
                    val array = JSONArray(backend.query(owner, "pleos_my_charges", params))
                    val page = (0 until array.length()).map { index -> array.getJSONObject(index).let { row ->
                        ChargeSessionRecord(row.getString("id"), row.nullableString("station_id"),
                            Instant.parse(row.getString("started_at")).toEpochMilli(), Instant.parse(row.getString("ended_at")).toEpochMilli(),
                            row.getInt("soc_start"), row.getInt("soc_end"), row.nullableDouble("avg_kw"), ChargeResult.valueOf(row.getString("result")))
                    } }
                    records += page
                    cursor = page.lastOrNull()
                } while (page.size == 200)
                if (consented() && vehicleId() == owner) cachedRecords = records
                else cachedRecords = emptyList()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* retain this owner's last successful read while offline */ }
        }
        cachedRecords
    }.distinctUntilChanged()
}
