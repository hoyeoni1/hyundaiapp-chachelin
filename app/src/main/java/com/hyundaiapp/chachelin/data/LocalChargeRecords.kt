package com.hyundaiapp.chachelin.data

import com.hyundaiapp.chachelin.domain.ChargeResult
import com.hyundaiapp.chachelin.domain.ChargeSessionRecord
import com.hyundaiapp.core.backend.BackendStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** Device-only records. Production uses a separate Keystore-encrypted noBackup file. */
class LocalChargeRecords(private val store: BackendStore) {
    private val state = JSONObject(store.read() ?: "{\"owners\":{},\"revoked\":{}}")
    private val owners get() = state.getJSONObject("owners")
    private val revoked get() = state.getJSONObject("revoked")
    private val changes = MutableStateFlow(0L)
    val revision = changes.asStateFlow()

    @Synchronized fun put(owner: String, record: ChargeSessionRecord) {
        check(!revoked.has(owner)) { "Consent withdrawn" }
        require(record.stationId == null || record.result == ChargeResult.NOT_AT_STATION)
        val records = owners.optJSONObject(owner) ?: JSONObject().also { owners.put(owner, it) }
        records.put(record.id, JSONObject().put("id", record.id).put("started", record.startedAtMs)
            .put("ended", record.endedAtMs).put("socStart", record.socStart).put("socEnd", record.socEnd)
            .put("kw", record.avgKw ?: JSONObject.NULL))
        save()
    }

    @Synchronized fun list(owner: String): List<ChargeSessionRecord> {
        val records = owners.optJSONObject(owner) ?: return emptyList()
        return records.keys().asSequence().map { records.getJSONObject(it) }.map {
            ChargeSessionRecord(it.getString("id"), null, it.getLong("started"), it.getLong("ended"),
                it.getInt("socStart"), it.getInt("socEnd"), if (it.isNull("kw")) null else it.getDouble("kw"),
                ChargeResult.NOT_AT_STATION)
        }.toList()
    }

    @Synchronized fun contains(owner: String, id: String) = owners.optJSONObject(owner)?.has(id) == true

    @Synchronized fun retract(owner: String, id: String): Boolean {
        if (!contains(owner, id)) return false
        owners.getJSONObject(owner).remove(id)
        save()
        return true
    }

    @Synchronized fun clear(owner: String) {
        owners.remove(owner)
        // Keep only a revoked UUID marker so a late producer cannot recreate deleted records.
        revoked.put(owner, true)
        save()
    }

    private fun save() {
        store.write(state.toString())
        changes.value++
    }
}
