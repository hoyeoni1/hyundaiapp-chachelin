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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyundaiapp.chachelin.AppContainer
import com.hyundaiapp.chachelin.BuildConfig
import com.hyundaiapp.chachelin.RecordingService
import com.hyundaiapp.chachelin.domain.ChargeScreenTarget
import com.hyundaiapp.chachelin.ui.charging.ChargingScreen
import com.hyundaiapp.chachelin.ui.charging.ChargingViewModel
import com.hyundaiapp.chachelin.ui.debug.SignalMonitorScreen
import com.hyundaiapp.chachelin.ui.history.HistoryScreen
import com.hyundaiapp.chachelin.ui.history.HistoryViewModel
import com.hyundaiapp.chachelin.ui.onboarding.OnboardingScreen
import com.hyundaiapp.chachelin.ui.onboarding.OnboardingViewModel
import com.hyundaiapp.chachelin.ui.onboarding.Terms
import com.hyundaiapp.chachelin.ui.review.FailureNoticeScreen
import com.hyundaiapp.chachelin.ui.review.FailureNoticeViewModel
import com.hyundaiapp.chachelin.ui.review.ReviewScreen
import com.hyundaiapp.chachelin.ui.review.ReviewViewModel
import com.hyundaiapp.chachelin.ui.stations.StationDetailScreen
import com.hyundaiapp.chachelin.ui.stations.StationDetailViewModel
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

/** 충전 세션이 띄우고 닫는 화면 */
private val CHARGE_FLOW_SCREENS = setOf(Screen.CHARGING, Screen.REVIEW, Screen.FAILURE)

/** 간단한 화면 스택. 화면이 늘면 navigation-compose로 바꿔도 된다(길민종) */
@Composable
fun ChachelinNav(container: AppContainer) {
    val rootScreen = {
        when {
            BuildConfig.DEBUG -> Screen.DEV_MENU
            container.vehicleProfile.onboarded.value -> Screen.STATIONS
            else -> Screen.ONBOARDING
        }
    }
    val stack = remember { mutableStateListOf(rootScreen()) }
    val go: (Screen) -> Unit = { stack.add(it) }
    val back: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    BackHandler(enabled = stack.size > 1) { back() }

    var stationId by remember { mutableStateOf<String?>(null) }
    var terms by remember { mutableStateOf<Terms?>(null) }
    var confirmWithdraw by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // 충전 세션에 따라 S3·S4·S5를 띄우고 닫는다(충전구를 열면 S3, 닫으면 S4/S5)
    val target by container.chargeRecorder.target.collectAsState(ChargeScreenTarget.NONE)
    LaunchedEffect(target) {
        if (stack.last() in CHARGE_FLOW_SCREENS) back()
        when (target) {
            ChargeScreenTarget.CHARGING -> go(Screen.CHARGING)
            ChargeScreenTarget.REVIEW -> go(Screen.REVIEW)
            ChargeScreenTarget.FAILURE -> go(Screen.FAILURE)
            ChargeScreenTarget.NONE -> Unit
        }
    }

    Surface(Modifier.fillMaxSize()) {
        when (stack.last()) {
            Screen.DEV_MENU -> DevMenu(go)
            Screen.STATIONS -> {
                val vm = viewModel { StationListViewModel(container.signals, container.stations) }
                // 화면에 들어올 때마다 새로 조회한다(방금 끝난 충전이 반영되게)
                LaunchedEffect(Unit) { vm.refresh() }
                val state by vm.state.collectAsState()
                StationListScreen(
                    state,
                    onStationClick = {
                        stationId = it
                        go(Screen.STATION_DETAIL)
                    },
                    onHistoryClick = { go(Screen.HISTORY) },
                    onRetry = vm::refresh,
                )
            }
            Screen.SIGNALS -> {
                // 신호 수신은 동의 후 기록 서비스가 시작한다. 개발 메뉴의 신호 모니터만 들어올 때 따로 켠다
                LaunchedEffect(Unit) { container.signals.start() }
                SignalMonitorScreen(container.signals)
            }
            Screen.STATION_DETAIL -> {
                // 개발 메뉴에서 바로 들어오면 고른 충전소가 없다 → 시드 첫 충전소
                val id = stationId ?: PreviewData.stationRow.id
                val vm = viewModel(key = id) { StationDetailViewModel(id, container.signals, container.signals, container.stations) }
                LaunchedEffect(Unit) { vm.refresh() }
                val state by vm.state.collectAsState()
                StationDetailScreen(
                    state,
                    onBack = back,
                    onRouteClick = vm::onRouteClick,
                    onConfirmRoute = vm::onConfirmRoute,
                    onDismissRoute = vm::onDismissRoute,
                )
            }
            Screen.CHARGING -> {
                val vm = viewModel { ChargingViewModel(container.chargeTracker.live, container.stations, container.chargeRules.demoTimeScale) }
                val state by vm.state.collectAsState()
                // 세션 없이 들어오는 경우는 개발 메뉴뿐이다 → 미리보기로 그린다
                (state ?: PreviewData.charging.takeIf { BuildConfig.DEBUG })?.let { ChargingScreen(it) }
            }
            Screen.REVIEW -> {
                val vm = viewModel { ReviewViewModel(container.chargeRecorder, container.stations, container.chargeRules.demoTimeScale) }
                val state by vm.state.collectAsState()
                val current = state
                if (current != null) {
                    ReviewScreen(current, vm::onRate, vm::onToggleTag, vm::onSubmit, vm::onLater)
                } else if (BuildConfig.DEBUG) {
                    ReviewScreen(PreviewData.review, onRate = {}, onToggleTag = {}, onSubmit = back, onLater = back)
                }
            }
            Screen.FAILURE -> {
                val vm = viewModel { FailureNoticeViewModel(container.chargeRecorder, container.stations) }
                val state by vm.state.collectAsState()
                val current = state
                if (current != null) {
                    FailureNoticeScreen(current, vm::onNotFailure, vm::onConfirm)
                } else if (BuildConfig.DEBUG) {
                    FailureNoticeScreen(PreviewData.failure, onNotFailure = back, onConfirm = back)
                }
            }
            Screen.HISTORY -> {
                val vm = viewModel { HistoryViewModel(container.records, container.stations) }
                val state by vm.state.collectAsState()
                HistoryScreen(state, onBack = back, onWithdrawConsent = { confirmWithdraw = true })
            }
            Screen.ONBOARDING -> {
                val vm = viewModel {
                    OnboardingViewModel(container.vehicleProfile, onStarted = { RecordingService.startIfAllowed(context) })
                }
                LaunchedEffect(Unit) { vm.reload() }
                val state by vm.state.collectAsState()
                OnboardingScreen(
                    state,
                    onAgreeLocation = vm::onAgreeLocation,
                    onAgreePrivacy = vm::onAgreePrivacy,
                    onSelectPreset = vm::onSelectPreset,
                    onShowTerms = { terms = it },
                    onStart = {
                        if (vm.onStart()) {
                            if (stack.size > 1) back() else stack[0] = Screen.STATIONS
                        }
                    },
                )
            }
        }
    }

    terms?.let { TermsDialog(it, onDismiss = { terms = null }) }
    if (confirmWithdraw) {
        WithdrawDialog(
            onConfirm = {
                confirmWithdraw = false
                container.withdrawConsent()
                stack.clear()
                stack.add(rootScreen())
            },
            onDismiss = { confirmWithdraw = false },
        )
    }
}

/** 동의 철회 확인. 취소·바깥 누르기는 철회 아님 */
@Composable
private fun WithdrawDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("동의를 철회할까요?") },
        text = { Text("충전 기록을 멈추고, 내 충전 기록과 이 차에 저장된 동의·차종 정보를 지워요. 다시 쓰려면 동의가 필요해요.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("철회") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
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
