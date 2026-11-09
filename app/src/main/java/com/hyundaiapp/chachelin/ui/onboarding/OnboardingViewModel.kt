package com.hyundaiapp.chachelin.ui.onboarding

import androidx.lifecycle.ViewModel
import com.hyundaiapp.chachelin.data.VehicleProfileStore
import com.hyundaiapp.chachelin.domain.VehiclePreset
import com.hyundaiapp.chachelin.ui.OnboardingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * S7 첫 실행 동의. 동의 둘과 차종이 모두 있어야 저장한다(동의 버튼 외 동작은 동의 아님).
 * [onStarted]는 저장한 뒤 불린다(기록 서비스 시작).
 * 이미 동의했으면 같은 화면을 설정으로 쓴다: 동의를 다시 받지 않고 저장된 동의·차종을 보여 주며 차종만 바꿀 수 있다.
 */
class OnboardingViewModel(
    private val profile: VehicleProfileStore,
    private val onStarted: () -> Unit = {},
) : ViewModel() {
    private val _state = MutableStateFlow(emptyState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    /** 화면에 들어올 때 부른다. 동의를 마쳤으면 저장된 값으로, 철회 뒤나 처음이면 빈 동의로 보여 준다 */
    fun reload() {
        _state.value = if (profile.onboarded.value) {
            emptyState().copy(locationAgreed = true, privacyAgreed = true, selectedPreset = savedPreset(), editing = true)
        } else emptyState()
    }

    /** 차종 순서를 저장하기 전 버전에서 동의한 경우는 용량이 같은 첫 차종으로 보여 준다 */
    private fun savedPreset(): Int? = profile.presetIndex
        ?: profile.capacityKwh?.let { kwh -> VehiclePreset.ALL.indexOfFirst { it.capacityKwh == kwh }.takeIf { it >= 0 } }

    private fun emptyState() = OnboardingUiState(vehiclePresets = VehiclePreset.ALL.map { it.label })

    fun onAgreeLocation() = _state.update { it.copy(locationAgreed = true) }

    fun onAgreePrivacy() = _state.update { it.copy(privacyAgreed = true) }

    fun onSelectPreset(index: Int) = _state.update { it.copy(selectedPreset = index) }

    /** @return 시작했으면 true */
    fun onStart(): Boolean {
        val current = _state.value
        val preset = current.selectedPreset?.let { VehiclePreset.ALL.getOrNull(it) } ?: return false
        if (!current.locationAgreed || !current.privacyAgreed) return false
        profile.completeOnboarding(preset.capacityKwh, current.selectedPreset)
        onStarted()
        return true
    }
}
