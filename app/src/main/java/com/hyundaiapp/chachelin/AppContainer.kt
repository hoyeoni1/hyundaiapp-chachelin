package com.hyundaiapp.chachelin

import android.content.Context
import com.hyundaiapp.chachelin.data.ChargeRecordRepository
import com.hyundaiapp.chachelin.data.FakeChargeRecordRepository
import com.hyundaiapp.chachelin.data.FakeStationRepository
import com.hyundaiapp.chachelin.data.StationRepository
import com.hyundaiapp.chachelin.domain.ChargeRules
import com.hyundaiapp.chachelin.domain.ChargeSessionTracker
import com.hyundaiapp.core.CoreFeature
import com.hyundaiapp.core.PleosVehicleSignals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 앱 전체에서 하나만 쓰는 객체 모음(간단한 수동 DI).
 * 서버가 준비되면 Fake 저장소를 실제 구현으로 바꾼다(황준용).
 */
class AppContainer(context: Context) {
    /** 화면과 무관하게 앱 수명 동안 도는 작업(충전 기록 등) */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 매니페스트 퍼미션과 맞출 것: CAR_ENERGY → EV_BATTERY, NAVI_ROUTE → NAVI
    val signals = PleosVehicleSignals(context, setOf(CoreFeature.EV_BATTERY, CoreFeature.NAVI))

    val stations: StationRepository = FakeStationRepository()
    val records: ChargeRecordRepository = FakeChargeRecordRepository()

    val chargeTracker = ChargeSessionTracker(
        signals = signals,
        stations = stations,
        rules = if (BuildConfig.DEBUG) ChargeRules.DEMO else ChargeRules.DEFAULT,
    )
}
