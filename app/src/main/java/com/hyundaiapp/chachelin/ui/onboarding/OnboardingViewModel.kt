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
 */
class OnboardingViewModel(
    private val profile: VehicleProfileStore,
    private val onStarted: () -> Unit = {},
) : ViewModel() {
    private val _state = MutableStateFlow(emptyState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    /** 화면에 들어올 때 부른다(철회 뒤에는 빈 동의로 다시 보여 줌) */
    fun reload() {
        _state.value = emptyState()
    }

    private fun emptyState() = OnboardingUiState(vehiclePresets = VehiclePreset.ALL.map { it.label })

    fun onAgreeLocation() = _state.update { it.copy(locationAgreed = true) }

    fun onAgreePrivacy() = _state.update { it.copy(privacyAgreed = true) }

    fun onSelectPreset(index: Int) = _state.update { it.copy(selectedPreset = index) }

    /** @return 시작했으면 true */
    fun onStart(): Boolean {
        val current = _state.value
        val preset = current.selectedPreset?.let { VehiclePreset.ALL.getOrNull(it) } ?: return false
        if (!current.locationAgreed || !current.privacyAgreed) return false
        profile.completeOnboarding(preset.capacityKwh)
        onStarted()
        return true
    }
}
