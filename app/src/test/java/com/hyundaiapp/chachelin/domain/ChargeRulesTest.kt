package com.hyundaiapp.chachelin.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** PRD 5.3 표의 경계값 */
class ChargeRulesTest {
    private val rules = ChargeRules.DEFAULT
    private val threeMin = 3 * 60_000L
    private val twoMin = 2 * 60_000L

    @Test
    fun `2퍼센트 이상 3분 이상 충전소 안이면 인증 충전`() {
        assertEquals(ChargeResult.SUCCESS, rules.judge(2, threeMin, atStation = true))
    }

    @Test
    fun `3분이 안 되면 인증도 실패도 아니다`() {
        assertNull(rules.judge(2, threeMin - 1, atStation = true))
    }

    @Test
    fun `1퍼센트만 오르면 인증도 실패도 아니다`() {
        assertNull(rules.judge(1, threeMin, atStation = true))
    }

    @Test
    fun `SoC가 그대로이고 2분 이상 열려 있었으면 실패`() {
        assertEquals(ChargeResult.FAILED, rules.judge(0, twoMin, atStation = true))
    }

    @Test
    fun `2분이 안 되면 잠깐 연 것이므로 실패가 아니다`() {
        assertNull(rules.judge(0, twoMin - 1, atStation = true))
    }

    @Test
    fun `충전소 밖에서 SoC가 올랐으면 기록만 남긴다`() {
        assertEquals(ChargeResult.NOT_AT_STATION, rules.judge(30, threeMin, atStation = false))
    }

    @Test
    fun `충전소 밖에서 SoC가 그대로면 기록하지 않는다`() {
        assertNull(rules.judge(0, threeMin, atStation = false))
    }

    @Test
    fun `시연 기준값은 시간 기준만 줄인다`() {
        assertEquals(ChargeResult.SUCCESS, ChargeRules.DEMO.judge(2, 10_000L, atStation = true))
        assertEquals(ChargeResult.FAILED, ChargeRules.DEMO.judge(0, 30_000L, atStation = true))
    }

    @Test
    fun `실사용 기준값은 시연 배속을 쓰지 않는다`() {
        assertEquals(1, ChargeRules.DEFAULT.demoTimeScale)
    }
}
