package com.hyundaiapp.chachelin.ui

import com.hyundaiapp.chachelin.data.FakeStationRepository
import com.hyundaiapp.chachelin.data.StationRepository
import com.hyundaiapp.chachelin.data.StationWithScore
import com.hyundaiapp.chachelin.ui.stations.StationDetailViewModel
import com.hyundaiapp.core.FakeVehicleSignals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class StationDetailViewModelTest {
    private val signals = FakeVehicleSignals()
    private val seed = FakeStationRepository()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `one screen entry performs one query and missing station ends loading`() = runTest {
        var calls = 0
        val repository = object : StationRepository by seed {
            override suspend fun get(stationId: String): StationWithScore? { calls++; return null }
        }
        val vm = StationDetailViewModel("missing", signals, signals, repository)
        assertEquals(0, calls)
        vm.refresh()
        assertEquals(1, calls)
        assertFalse(vm.state.value.loading)
        assertNotNull(vm.state.value.error)
        vm.onRouteClick()
        vm.onConfirmRoute()
        assertFalse(vm.state.value.confirmRoute)
        assertTrue(signals.requestedRoutes.isEmpty())
    }

    @Test fun `failed query can retry and clears error after recovery`() = runTest {
        var fail = true
        val repository = object : StationRepository by seed {
            override suspend fun get(stationId: String): StationWithScore? {
                if (fail) error("network")
                return seed.get(stationId)
            }
        }
        val vm = StationDetailViewModel("st-001", signals, signals, repository)
        vm.refresh()
        assertFalse(vm.state.value.loading)
        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.station)
        fail = false
        vm.refresh()
        assertFalse(vm.state.value.loading)
        assertNull(vm.state.value.error)
        assertEquals("st-001", vm.state.value.station?.id)
    }

    @Test fun `stalled query times out and allows retry`() = runTest {
        var stall = true
        val repository = object : StationRepository by seed {
            override suspend fun get(stationId: String): StationWithScore? {
                if (stall) awaitCancellation()
                return seed.get(stationId)
            }
        }
        val vm = StationDetailViewModel("st-001", signals, signals, repository)
        vm.refresh()
        assertTrue(vm.state.value.loading)
        advanceTimeBy(10_001L)
        runCurrent()
        assertFalse(vm.state.value.loading)
        assertNotNull(vm.state.value.error)
        stall = false
        vm.refresh()
        assertNotNull(vm.state.value.station)
        assertNull(vm.state.value.error)
    }

    @Test fun `new refresh cancels previous request without replacing recovered state`() = runTest {
        var calls = 0
        var cancelled = false
        val repository = object : StationRepository by seed {
            override suspend fun get(stationId: String): StationWithScore? {
                calls++
                if (calls == 1) {
                    try { delay(5_000L) } finally { cancelled = true }
                    return null
                }
                return seed.get(stationId)
            }
        }
        val vm = StationDetailViewModel("st-001", signals, signals, repository)
        vm.refresh()
        vm.refresh()
        advanceTimeBy(5_001L)
        runCurrent()
        assertTrue(cancelled)
        assertNotNull(vm.state.value.station)
        assertNull(vm.state.value.error)
    }
}
