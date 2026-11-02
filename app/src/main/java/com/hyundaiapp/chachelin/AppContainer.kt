package com.hyundaiapp.chachelin

import android.content.Context
import android.content.Intent
import com.hyundaiapp.chachelin.data.ChargeRecordRepository
import com.hyundaiapp.chachelin.data.FakeChargeRecordRepository
import com.hyundaiapp.chachelin.data.FakeStationRepository
import com.hyundaiapp.chachelin.data.SharedPrefsVehicleProfileStore
import com.hyundaiapp.chachelin.data.StationRepository
import com.hyundaiapp.chachelin.data.VehicleProfileStore
import com.hyundaiapp.chachelin.domain.ChargeRules
import com.hyundaiapp.chachelin.domain.ChargeSessionRecorder
import com.hyundaiapp.chachelin.domain.ChargeSessionTracker
import com.hyundaiapp.core.CoreFeature
import com.hyundaiapp.core.PleosVehicleSignals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 앱 전체에서 하나만 쓰는 객체 모음(간단한 수동 DI).
 * 서버가 준비되면 Fake 저장소를 실제 구현으로 바꾼다(황준용).
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** 화면과 무관하게 앱 수명 동안 도는 작업(충전 기록 등) */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 매니페스트 퍼미션과 맞출 것: CAR_ENERGY → EV_BATTERY, NAVI_ROUTE → NAVI
    val signals = PleosVehicleSignals(context, setOf(CoreFeature.EV_BATTERY, CoreFeature.NAVI))

    val stations: StationRepository = FakeStationRepository()
    val records: ChargeRecordRepository = FakeChargeRecordRepository()

    /** S7 동의·차종 */
    val vehicleProfile: VehicleProfileStore = SharedPrefsVehicleProfileStore(context)

    val chargeTracker = ChargeSessionTracker(
        signals = signals,
        stations = stations,
        rules = if (BuildConfig.DEBUG) ChargeRules.DEMO else ChargeRules.DEFAULT,
        presetCapacityKwh = { vehicleProfile.capacityKwh },
    )
    val chargeRecorder = ChargeSessionRecorder(chargeTracker, records, vehicleProfile::vehicleId)

    /** 기록 중일 때만 있다. [stopRecording]이 취소하면 기록 관련 코루틴이 모두 멈춘다 */
    private var recordingJob: Job? = null

    /**
     * 차량·위치 신호 수신과 충전 기록을 시작한다(동의 전에는 받지 않음). [RecordingService]가 부른다. 여러 번 불러도 한 번만 시작.
     */
    fun startRecording() {
        if (recordingJob?.isActive == true) return
        val job = SupervisorJob(appScope.coroutineContext[Job])
        recordingJob = job
        val scope = CoroutineScope(appScope.coroutineContext + job)
        signals.start()
        chargeRecorder.start(scope)
        chargeTracker.start(scope)
    }

    /** 기록을 멈추고 신호 수신도 끊는다. [RecordingService.onDestroy]가 부른다 */
    fun stopRecording() {
        val job = recordingJob ?: return
        recordingJob = null
        job.cancel()
        chargeTracker.reset()
        chargeRecorder.reset()
        signals.stop()
    }

    /**
     * 동의 철회(앱 개발 규정 "철회 시 즉시 활용 중단·저장 데이터 삭제"):
     * 기록 서비스를 멈추고 이 차량의 충전 기록과 기기에 남은 동의·차종·가명 차량 ID를 지운다. 다시 동의하면 새 차량 ID로 시작한다.
     */
    fun withdrawConsent() {
        val vehicleId = vehicleProfile.vehicleId()
        appContext.stopService(Intent(appContext, RecordingService::class.java))
        stopRecording()
        vehicleProfile.clear()
        appScope.launch { runCatching { records.deleteMine(vehicleId) } }
    }
}
