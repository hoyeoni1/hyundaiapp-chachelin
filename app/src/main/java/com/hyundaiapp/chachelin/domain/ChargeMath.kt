package com.hyundaiapp.chachelin.domain

object ChargeMath {
    /**
     * 실측 평균 충전 속도(kW) = ΔSoC ÷ 100 × 배터리 용량(kWh) ÷ 충전 시간(h). PRD 5.3.
     * 시간이 0이거나 SoC가 오르지 않았으면 null.
     */
    fun averageKw(socStart: Int, socEnd: Int, capacityKwh: Float, durationMs: Long): Double? {
        val gain = socEnd - socStart
        if (gain <= 0 || durationMs <= 0) return null
        val hours = durationMs / 3_600_000.0
        return gain / 100.0 * capacityKwh / hours
    }
}
