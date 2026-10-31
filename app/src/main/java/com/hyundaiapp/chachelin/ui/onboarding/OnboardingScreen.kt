package com.hyundaiapp.chachelin.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hyundaiapp.chachelin.ui.OnboardingUiState
import com.hyundaiapp.chachelin.ui.components.ConsentRow
import com.hyundaiapp.chachelin.ui.components.PrimaryButton
import com.hyundaiapp.chachelin.ui.components.SelectableButton
import com.hyundaiapp.chachelin.ui.theme.CarColors
import com.hyundaiapp.chachelin.ui.theme.CarDimens
import com.hyundaiapp.chachelin.ui.theme.ChachelinTheme

enum class Terms { LOCATION, PRIVACY }

/**
 * S7 첫 실행 동의 + 차종 선택(정차 중). 앱 개발 규정: [동의] 버튼을 누르는 것만 동의로 본다.
 * 차종은 실측 kW 계산용 배터리 용량(PRD 5.3). 필수 동의 둘과 차종을 모두 골라야 시작할 수 있다.
 */
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onAgreeLocation: () -> Unit,
    onAgreePrivacy: () -> Unit,
    onSelectPreset: (Int) -> Unit,
    onShowTerms: (Terms) -> Unit,
    onStart: () -> Unit,
) {
    val canStart = state.locationAgreed && state.privacyAgreed && state.selectedPreset != null
    val startLabel = when {
        canStart -> "시작하기"
        !state.locationAgreed || !state.privacyAgreed -> "필수 항목에 동의하면 시작할 수 있어요"
        else -> "내 차를 골라 주세요"
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val portrait = maxWidth < maxHeight
        if (portrait) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Intro()
                    Consents(state, onAgreeLocation, onAgreePrivacy, onShowTerms)
                    Presets(state, onSelectPreset)
                }
                PrimaryButton(startLabel, onStart, Modifier.fillMaxWidth(), enabled = canStart)
            }
        } else {
            Row(
                Modifier.fillMaxSize().padding(horizontal = CarDimens.ScreenPadding, vertical = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(40.dp),
            ) {
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Intro()
                    Spacer(Modifier.weight(1f))
                    Consents(state, onAgreeLocation, onAgreePrivacy, onShowTerms)
                }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Presets(state, onSelectPreset)
                    Spacer(Modifier.weight(1f))
                    PrimaryButton(startLabel, onStart, Modifier.fillMaxWidth(), enabled = canStart)
                }
            }
        }
    }
}

@Composable
private fun Intro() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("차슐랭", style = MaterialTheme.typography.bodyLarge, color = CarColors.Primary, fontWeight = FontWeight.Medium)
        Text("실제로 충전한 사람만 평가하는\n충전소 가이드", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun Consents(state: OnboardingUiState, onAgreeLocation: () -> Unit, onAgreePrivacy: () -> Unit, onShowTerms: (Terms) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ConsentRow("위치정보 이용 (필수)", state.locationAgreed, onAgreeLocation, onShowTerms = { onShowTerms(Terms.LOCATION) })
        ConsentRow("개인정보 수집·이용 (필수)", state.privacyAgreed, onAgreePrivacy, onShowTerms = { onShowTerms(Terms.PRIVACY) })
    }
}

@Composable
private fun Presets(state: OnboardingUiState, onSelectPreset: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("내 차 고르기", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("실측 충전 속도(kW)를 계산할 배터리 용량이에요", style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant)
    }
    state.vehiclePresets.withIndex().chunked(2).forEach { pair ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            pair.forEach { (i, preset) ->
                SelectableButton(
                    selected = state.selectedPreset == i,
                    onClick = { onSelectPreset(i) },
                    modifier = Modifier.weight(1f),
                    height = 96.dp,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(preset.substringBefore(" ("), style = MaterialTheme.typography.labelLarge.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize))
                        Text(preset.substringAfter("(", "").removeSuffix(")"), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Preview(name = "S7 가로", widthDp = 1280, heightDp = 720)
@Composable
private fun OnboardingPreview() {
    ChachelinTheme { Surface { OnboardingScreen(OnboardingUiState(locationAgreed = true, selectedPreset = 0), {}, {}, {}, {}, {}) } }
}

@Preview(name = "S7 세로", widthDp = 720, heightDp = 1280)
@Composable
private fun OnboardingPortraitPreview() {
    ChachelinTheme { Surface { OnboardingScreen(OnboardingUiState(locationAgreed = true, selectedPreset = 0), {}, {}, {}, {}, {}) } }
}
