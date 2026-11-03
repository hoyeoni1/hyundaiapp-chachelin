package com.hyundaiapp.chachelin.data

import com.hyundaiapp.chachelin.domain.ChargeResult
import com.hyundaiapp.chachelin.domain.ChargeSessionRecord
import com.hyundaiapp.core.backend.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LocalChargeRecordsTest {
    private class Memory : BackendStore {
        var value: String? = null
        override fun read() = value
        override fun write(value: String) { this.value = value }
    }
    private val owner = "10000000-0000-4000-8000-000000000001"
    private fun record(id: String = "local") = ChargeSessionRecord(id, null, 1000, 2000, 20, 40, null, ChargeResult.NOT_AT_STATION)

    @Test fun survivesRestartDeduplicatesAndDoesNotLeakAcrossOwners() {
        val disk = Memory()
        val local = LocalChargeRecords(disk)
        local.put(owner, record()); local.put(owner, record())
        assertEquals(listOf(record()), LocalChargeRecords(disk).list(owner))
        assertTrue(local.list("another-owner").isEmpty())
        local.clear(owner)
        assertTrue(LocalChargeRecords(disk).list(owner).isEmpty())
        assertThrows(IllegalStateException::class.java) { LocalChargeRecords(disk).put(owner, record()) }
    }

    @Test fun outsideStationNeverEntersOutboxAndRemainsVisibleOffline() = runTest {
        val queue = Memory()
        val localDisk = Memory()
        val local = LocalChargeRecords(localDisk)
        val backend = SupabaseBackend(BackendConfig("https://example.supabase.co", "sb_publishable_test", "CHACHELIN", true),
            queue, BackendTransport { _, _, _ -> throw IllegalStateException("offline") }, backgroundScope, startWorker = false)
        backend.activate(owner)
        val repo = SupabaseChargeRecordRepository(backend, { owner }, local) { true }
        repo.upload(record(), owner)
        repo.upload(record("station").copy(stationId = "st-001", result = ChargeResult.SUCCESS), owner)
        val tasks = JSONObject(queue.value!!).getJSONArray("generations").getJSONObject(0).getJSONArray("tasks")
        assertEquals(1, tasks.length())
        assertEquals("station", tasks.getJSONObject(0).getJSONObject("payload").getString("p_id"))
        assertEquals(listOf(record()), repo.myRecords().first())
        // A new repository/store after process restart still exposes the device-only history.
        val restored = SupabaseChargeRecordRepository(backend, { owner }, LocalChargeRecords(localDisk)) { true }
        assertEquals(listOf(record()), restored.myRecords().first())
        try { repo.submitReview("local", 5, emptySet()); fail("Local record must not create a server review") }
        catch (_: IllegalArgumentException) { }
        repo.retract("local")
        assertTrue(local.list(owner).isEmpty())
        assertEquals(1, JSONObject(queue.value!!).getJSONArray("generations").getJSONObject(0).getJSONArray("tasks").length())
    }

    @Test fun withdrawalClearsLocalHistoryWithoutWaitingForNetwork() = runTest {
        val disk = Memory()
        val local = LocalChargeRecords(disk)
        val backend = SupabaseBackend(BackendConfig("https://example.supabase.co", "sb_publishable_test", "CHACHELIN", true),
            Memory(), BackendTransport { _, _, _ -> error("No network expected") }, backgroundScope, startWorker = false)
        // No server generation: deleteMine is immediately idempotent.
        local.put(owner, record())
        val repo = SupabaseChargeRecordRepository(backend, { owner }, local) { true }
        repo.deleteMine(owner)
        assertTrue(LocalChargeRecords(disk).list(owner).isEmpty())
        try { repo.upload(record(), owner); fail("Late writer must be rejected") }
        catch (_: IllegalStateException) { }
    }
}
