package com.hyundaiapp.chachelin.data

import android.annotation.SuppressLint
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/** S7 결과(동의 완료 여부, 차종 배터리 용량)와 가명 차량 ID. 기기에만 저장한다 */
interface VehicleProfileStore {
    /** 필수 동의 둘과 차종 선택을 마쳤는지. 기록은 이게 true일 때만 한다 */
    val onboarded: StateFlow<Boolean>
    val capacityKwh: Float?

    /** S7에서 고른 차종([com.hyundaiapp.chachelin.domain.VehiclePreset.ALL]의 순서). 설정 화면에서 다시 보여 줄 때 쓴다 */
    val presetIndex: StateFlow<Int?>

    /** 처음 동의할 때와 설정에서 차종을 바꿀 때 부른다 */
    fun completeOnboarding(capacityKwh: Float, presetIndex: Int? = null)

    /** 서버 `charge_sessions.vehicle_hash`용(PRD 5.5). 설치마다 무작위 UUID, 처음 부를 때 만든다. 차대번호·계정은 쓰지 않는다 */
    fun vehicleId(): String

    /** 동의 철회: 전부 지운다. 다시 동의하면 새 차량 ID가 만들어져 이전 기록과 이어지지 않는다 */
    fun clear()
}

class SharedPrefsVehicleProfileStore(context: Context) : VehicleProfileStore {
    private val prefs = context.getSharedPreferences("vehicle_profile", Context.MODE_PRIVATE)
    private val _onboarded = MutableStateFlow(prefs.contains(KEY_CAPACITY))
    private val _presetIndex = MutableStateFlow(if (prefs.contains(KEY_PRESET)) prefs.getInt(KEY_PRESET, 0) else null)

    override val onboarded: StateFlow<Boolean> = _onboarded.asStateFlow()
    override val capacityKwh: Float?
        get() = if (prefs.contains(KEY_CAPACITY)) prefs.getFloat(KEY_CAPACITY, 0f) else null

    override val presetIndex: StateFlow<Int?> = _presetIndex.asStateFlow()

    override fun completeOnboarding(capacityKwh: Float, presetIndex: Int?) {
        prefs.edit().putFloat(KEY_CAPACITY, capacityKwh).apply {
            if (presetIndex != null) putInt(KEY_PRESET, presetIndex) else remove(KEY_PRESET)
        }.apply()
        _presetIndex.value = presetIndex
        _onboarded.value = true
    }

    @Synchronized
    override fun vehicleId(): String {
        prefs.getString(KEY_VEHICLE_ID, null)?.let { return it }
        val id = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_VEHICLE_ID, id).apply()
        return id
    }

    @SuppressLint("ApplySharedPref")
    @Synchronized
    override fun clear() {
        // 철회는 바로 반영돼야 하므로 commit(동기 저장)
        prefs.edit().clear().commit()
        _presetIndex.value = null
        _onboarded.value = false
    }

    private companion object {
        const val KEY_CAPACITY = "capacity_kwh"
        const val KEY_PRESET = "preset_index"
        const val KEY_VEHICLE_ID = "vehicle_id"
    }
}

class FakeVehicleProfileStore : VehicleProfileStore {
    override val onboarded = MutableStateFlow(false)
    override var capacityKwh: Float? = null
        private set
    private val _presetIndex = MutableStateFlow<Int?>(null)
    override val presetIndex: StateFlow<Int?> = _presetIndex.asStateFlow()
    private var id: String? = null

    override fun completeOnboarding(capacityKwh: Float, presetIndex: Int?) {
        this.capacityKwh = capacityKwh
        _presetIndex.value = presetIndex
        onboarded.value = true
    }

    override fun vehicleId(): String = id ?: UUID.randomUUID().toString().also { id = it }

    override fun clear() {
        capacityKwh = null
        _presetIndex.value = null
        onboarded.value = false
        id = null
    }
}
