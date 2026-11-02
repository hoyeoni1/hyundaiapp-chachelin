package com.hyundaiapp.chachelin.domain

/** S7 차종 프리셋. 배터리 용량은 실측 kW 계산에 쓴다(PRD 5.3) */
data class VehiclePreset(val label: String, val capacityKwh: Float) {
    companion object {
        val ALL = listOf(
            VehiclePreset("아이오닉 5 (77.4kWh)", 77.4f),
            VehiclePreset("아이오닉 6 (77.4kWh)", 77.4f),
            VehiclePreset("EV6 (77.4kWh)", 77.4f),
            VehiclePreset("코나 일렉트릭 (64.8kWh)", 64.8f),
        )
    }
}
