package com.hyundaiapp.chachelin.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChargeResultNoticesTest {
    private val pending = MutableStateFlow<ChargeSessionRecord?>(null)
    private val visible = MutableStateFlow(false)
    private val shown = mutableListOf<String>()
    private var hidden = 0

    private fun record(id: String, result: ChargeResult = ChargeResult.SUCCESS) =
        ChargeSessionRecord(id, "st-003", 0L, 60_000L, 40, 80, 60.0, result)

    private fun run(body: suspend TestScope.() -> Unit) = runTest(UnconfinedTestDispatcher()) {
        ChargeResultNotices(pending, visible, show = { shown += it.id }, hide = { hidden++ }).start(backgroundScope)
        runCurrent()
        body()
    }

    @Test
    fun `앱이 뒤에 있을 때 결과가 생기면 알린다`() = run {
        pending.value = record("a")
        runCurrent()
        assertEquals(listOf("a"), shown)
    }

    @Test
    fun `실패 결과도 알린다`() = run {
        pending.value = record("f", ChargeResult.FAILED)
        runCurrent()
        assertEquals(listOf("f"), shown)
    }

    @Test
    fun `앱이 화면에 있으면 알리지 않는다`() = run {
        visible.value = true
        pending.value = record("a")
        runCurrent()
        assertTrue(shown.isEmpty())
    }

    @Test
    fun `앱을 열면 알림을 지우고 다시 뒤로 가도 같은 결과는 또 알리지 않는다`() = run {
        pending.value = record("a")
        visible.value = true
        val hiddenAfterOpen = hidden
        visible.value = false
        runCurrent()
        assertEquals(listOf("a"), shown)
        assertTrue(hiddenAfterOpen > 0)
    }

    @Test
    fun `결과를 처리하면 알림을 지운다`() = run {
        pending.value = record("a")
        val before = hidden
        pending.value = null
        runCurrent()
        assertEquals(before + 1, hidden)
    }

    @Test
    fun `새 세션 결과는 다시 알린다`() = run {
        pending.value = record("a")
        pending.value = null
        pending.value = record("b")
        runCurrent()
        assertEquals(listOf("a", "b"), shown)
    }
}
