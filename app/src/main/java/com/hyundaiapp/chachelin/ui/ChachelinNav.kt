package com.hyundaiapp.chachelin.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyundaiapp.chachelin.AppContainer
import com.hyundaiapp.chachelin.BuildConfig
import com.hyundaiapp.chachelin.ui.charging.ChargingScreen
import com.hyundaiapp.chachelin.ui.debug.SignalMonitorScreen
import com.hyundaiapp.chachelin.ui.history.HistoryScreen
import com.hyundaiapp.chachelin.ui.onboarding.OnboardingScreen
import com.hyundaiapp.chachelin.ui.onboarding.Terms
import com.hyundaiapp.chachelin.ui.review.FailureNoticeScreen
import com.hyundaiapp.chachelin.ui.review.ReviewScreen
import com.hyundaiapp.chachelin.ui.stations.StationDetailScreen
import com.hyundaiapp.chachelin.ui.stations.StationListScreen
import com.hyundaiapp.chachelin.ui.stations.StationListViewModel

/** PRD 5.4 화면 목록. 디버그 빌드는 개발 메뉴에서 시작한다 */
enum class Screen(val title: String) {
    DEV_MENU("개발 메뉴"),
    STATIONS("S1 근처 충전소"),
    STATION_DETAIL("S2 충전소 상세"),
    CHARGING("S3 충전 중"),
    REVIEW("S4 충전 완료 리뷰"),
    FAILURE("S5 실패 기록 알림"),
    HISTORY("S6 내 충전 기록"),
    ONBOARDING("S7 첫 실행 동의"),
    SIGNALS("신호 모니터"),
}

/** 간단한 화면 스택. 화면이 늘면 navigation-compose로 바꿔도 된다(길민종) */
@Composable
fun ChachelinNav(container: AppContainer) {
    val stack = remember { mutableStateListOf(if (BuildConfig.DEBUG) Screen.DEV_MENU else Screen.STATIONS) }
    val go: (Screen) -> Unit = { stack.add(it) }
    val back: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    BackHandler(enabled = stack.size > 1) { back() }

    // S2·S4·S7은 아직 ViewModel이 없다 → 화면 동작 확인용으로 여기서 상태를 들고 있는다(이상준 ViewModel로 교체)
    var detail by remember { mutableStateOf(PreviewData.stationDetail) }
    var review by remember { mutableStateOf(PreviewData.review) }
    var onboarding by remember { mutableStateOf(OnboardingUiState()) }
    var terms by remember { mutableStateOf<Terms?>(null) }

    Surface(Modifier.fillMaxSize()) {
        when (stack.last()) {
            Screen.DEV_MENU -> DevMenu(go)
            Screen.STATIONS -> {
                val vm = viewModel { StationListViewModel(container.signals, container.stations) }
                val state by vm.state.collectAsState()
                StationListScreen(
                    state,
                    onStationClick = { go(Screen.STATION_DETAIL) },
                    onHistoryClick = { go(Screen.HISTORY) },
                    onRetry = vm::refresh,
                )
            }
            Screen.SIGNALS -> SignalMonitorScreen(container.signals)
            // 아래는 아직 ViewModel이 없다 → 미리보기 데이터로 그린다
            Screen.STATION_DETAIL -> StationDetailScreen(
                detail,
                onBack = back,
                onRouteClick = { detail = detail.copy(confirmRoute = true) },
                // 실제 경로 설정(NaviHelper requestRoute)은 이상준 ViewModel에서
                onConfirmRoute = { detail = detail.copy(confirmRoute = false) },
                onDismissRoute = { detail = detail.copy(confirmRoute = false) },
            )
            Screen.CHARGING -> ChargingScreen(PreviewData.charging)
            Screen.REVIEW -> ReviewScreen(
                review,
                onRate = { review = review.copy(rating = it) },
                onToggleTag = { tag ->
                    review = review.copy(
                        selectedTags = if (tag in review.selectedTags) review.selectedTags - tag else review.selectedTags + tag,
                    )
                },
                onSubmit = { review = PreviewData.review; back(); go(Screen.HISTORY) },
                onLater = back,
            )
            Screen.FAILURE -> FailureNoticeScreen(PreviewData.failure, onNotFailure = back, onConfirm = back)
            Screen.HISTORY -> HistoryScreen(PreviewData.history, onBack = back)
            Screen.ONBOARDING -> OnboardingScreen(
                onboarding,
                onAgreeLocation = { onboarding = onboarding.copy(locationAgreed = true) },
                onAgreePrivacy = { onboarding = onboarding.copy(privacyAgreed = true) },
                onSelectPreset = { onboarding = onboarding.copy(selectedPreset = it) },
                onShowTerms = { terms = it },
                onStart = back,
            )
        }
    }

    terms?.let { TermsDialog(it, onDismiss = { terms = null }) }
}

/** 약관 전문 자리. 문구는 장호연이 정리하면 바꾼다. 닫기는 동의가 아니다 */
@Composable
private fun TermsDialog(terms: Terms, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (terms == Terms.LOCATION) "위치정보 이용" else "개인정보 수집·이용") },
        text = { Text("[약관 전문 준비 중]") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

@Composable
private fun DevMenu(go: (Screen) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("차슐랭 개발 메뉴 (디버그 빌드에만 보임)", style = MaterialTheme.typography.headlineMedium)
        Screen.entries.filter { it != Screen.DEV_MENU }.forEach { s ->
            Button(onClick = { go(s) }) { Text(s.title) }
        }
    }
}
