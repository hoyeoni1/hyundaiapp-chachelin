package com.hyundaiapp.chachelin.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChargeMathTest {
    @Test
    fun `77_4kWh 배터리를 30분에 20에서 80퍼센트로 채우면 약 92_9kW`() {
        // 0.6 × 77.4 ÷ 0.5h
        assertEquals(92.88, ChargeMath.averageKw(20, 80, 77.4f, 30 * 60_000L)!!, 0.01)
    }

    @Test
    fun `SoC가 오르지 않았으면 속도를 계산하지 않는다`() {
        assertNull(ChargeMath.averageKw(40, 40, 77.4f, 60_000L))
    }
}
