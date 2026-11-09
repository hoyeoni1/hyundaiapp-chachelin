package com.hyundaiapp.chachelin.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import com.hyundaiapp.chachelin.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import com.hyundaiapp.chachelin.ui.components.CarAlertDialog
import com.hyundaiapp.chachelin.ui.components.NotificationPermissionState
import com.hyundaiapp.chachelin.ui.components.SecondaryButton
import com.hyundaiapp.chachelin.ui.components.ScreenHeader
import com.hyundaiapp.chachelin.ui.components.rememberNotificationPermission
import com.hyundaiapp.chachelin.ui.debug.SignalMonitorScreen
import com.hyundaiapp.chachelin.ui.history.HistoryScreen
import com.hyundaiapp.chachelin.ui.history.HistoryViewModel
import com.hyundaiapp.chachelin.ui.home.AppNavRail
import com.hyundaiapp.chachelin.ui.home.HomeScreen
import com.hyundaiapp.chachelin.ui.home.HomeViewModel
import com.hyundaiapp.chachelin.ui.home.NavTab
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

/** PRD 5.4 화면 목록. 시작은 S7(동의 전)·홈(동의 후). 개발 메뉴는 디버그 빌드에서 홈·S1 제목을 길게 눌러 연다 */
enum class Screen(val title: String) {
    DEV_MENU("개발 메뉴"),
    HOME("홈"),
    STATIONS("S1 근처 충전소"),
    STATION_DETAIL("S2 충전소 상세"),
    CHARGING("S3 충전 중"),
    REVIEW("S4 충전 완료 리뷰"),
    FAILURE("S5 실패 기록 알림"),
    HISTORY("S6 내 충전 기록"),
    ONBOARDING("S7 첫 실행 동의"),
    SETTINGS("설정(S7 다시 보기)"),
    SIGNALS("신호 모니터"),
}

/** 충전 세션이 띄우고 닫는 화면 */
private val CHARGE_FLOW_SCREENS = setOf(Screen.CHARGING, Screen.REVIEW, Screen.FAILURE)

/**
 * 왼쪽 이동 막대를 보여 주는 화면과 그 칸(11-09 시연 피드백: 화면 이동 버튼·홈 화면 없음).
 * 충전 흐름(S3·S4·S5)과 첫 동의는 그 일에만 집중하도록 막대를 숨긴다.
 */
private val RAIL_TABS = mapOf(
    Screen.HOME to NavTab.HOME,
    Screen.STATIONS to NavTab.STATIONS,
    Screen.STATION_DETAIL to NavTab.STATIONS,
    Screen.HISTORY to NavTab.HISTORY,
    Screen.SETTINGS to NavTab.SETTINGS,
)

/** 화면 전환 애니메이션 길이. 정차 화면 전용이라 짧은 밀어 넣기만 쓴다 */
private const val TRANSITION_MS = 220

/** 간단한 화면 스택. 화면이 늘면 navigation-compose로 바꿔도 된다(길민종) */
@Composable
fun ChachelinNav(container: AppContainer, onExit: () -> Unit) {
    // 디버그 빌드도 실제 흐름으로 시작한다(시연 영상·심사 모두 같은 화면, 11-05 장호연 결정)
    val rootScreen = { if (container.vehicleProfile.onboarded.value) Screen.HOME else Screen.ONBOARDING }
    val screenStates = rememberSaveableStateHolder()
    // 액티비티가 다시 만들어져도(내비 왕복·설정 변경) 보던 화면으로 돌아온다
    val stack = rememberSaveable(saver = ScreenStackSaver) { mutableStateListOf(rootScreen()) }
    val go: (Screen) -> Unit = { stack.add(it) }
    val back: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) else onExit() }
    // 막대 칸은 홈 위에 한 단계로 쌓는다: 어느 칸에서든 뒤로 가면 홈, 홈에서 뒤로 가면 앱을 닫는다
    val goTab: (NavTab) -> Unit = { tab ->
        val screen = when (tab) {
            NavTab.HOME -> Screen.HOME
            NavTab.STATIONS -> Screen.STATIONS
            NavTab.HISTORY -> Screen.HISTORY
            NavTab.SETTINGS -> Screen.SETTINGS
        }
        if (stack.last() != screen) {
            stack.clear()
            stack.add(Screen.HOME)
            if (screen != Screen.HOME) stack.add(screen)
        }
    }
    BackHandler(enabled = stack.size > 1) { back() }

    var stationId by remember { mutableStateOf<String?>(null) }
    var terms by remember { mutableStateOf<Terms?>(null) }
    var confirmWithdraw by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val notifications = rememberNotificationPermission()

    // 충전 세션에 따라 S3·S4·S5를 띄우고 닫는다(충전구를 열면 S3, 닫으면 S4/S5)
    val target by container.chargeRecorder.target.collectAsState(ChargeScreenTarget.NONE)
    val openChargeFlow: () -> Unit = {
        when (target) {
            ChargeScreenTarget.CHARGING -> go(Screen.CHARGING)
            ChargeScreenTarget.REVIEW -> go(Screen.REVIEW)
            ChargeScreenTarget.FAILURE -> go(Screen.FAILURE)
            ChargeScreenTarget.NONE -> Unit
        }
    }
    LaunchedEffect(target) {
        if (stack.last() in CHARGE_FLOW_SCREENS) back()
        openChargeFlow()
    }

    val current = stack.last()
    Surface(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            RAIL_TABS[current]?.let { tab -> AppNavRail(tab, goTab) }
            Column(Modifier.weight(1f).fillMaxSize()) {
                // 화면을 바꿀 때 짧게 밀어 넣는다: 들어가면 오른쪽에서, 돌아오면 왼쪽에서(11-09 시연 피드백: 전환 과정 없음)
                AnimatedContent(
                    targetState = NavEntry(current, stack.size),
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        val forward = targetState.depth >= initialState.depth
                        (slideInHorizontally(tween(TRANSITION_MS)) { w -> if (forward) w / 8 else -w / 8 } + fadeIn(tween(TRANSITION_MS))) togetherWith
                            fadeOut(tween(TRANSITION_MS / 2))
                    },
                    label = "screen",
                ) { entry ->
                    Box(Modifier.fillMaxSize()) {
                        ScreenContent(
                            screen = entry.screen,
                            container = container,
                            screenStates = screenStates,
                            go = go,
                            back = back,
                            goTab = goTab,
                            openChargeFlow = openChargeFlow,
                            stationId = stationId,
                            onStationSelected = { stationId = it },
                            onShowTerms = { terms = it },
                            onWithdraw = { confirmWithdraw = true },
                            onFirstRunDone = { stack[0] = Screen.HOME },
                            stackSize = stack.size,
                            notifications = notifications,
                            context = context,
                        )
                    }
                }
                // 홈은 상태 칸에서 바로 들어가므로 아래 버튼을 따로 두지 않는다
                if (target != ChargeScreenTarget.NONE && current !in CHARGE_FLOW_SCREENS &&
                    current != Screen.ONBOARDING && current != Screen.HOME
                ) {
                    SecondaryButton(
                        if (target == ChargeScreenTarget.CHARGING) "충전 화면으로 돌아가기" else "충전 결과 확인하기",
                        openChargeFlow,
                        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }

    terms?.let { TermsDialog(it, onDismiss = { terms = null }) }
    if (confirmWithdraw) {
        WithdrawDialog(
            onConfirm = {
                confirmWithdraw = false
                container.withdrawConsent()
                screenStates.removeState(Screen.STATIONS.name)
                stack.clear()
                stack.add(rootScreen())
            },
            onDismiss = { confirmWithdraw = false },
        )
    }
}

/** 전환 방향을 정하려고 스택 깊이를 함께 둔다 */
private data class NavEntry(val screen: Screen, val depth: Int)

@Composable
private fun ScreenContent(
    screen: Screen,
    container: AppContainer,
    screenStates: SaveableStateHolder,
    go: (Screen) -> Unit,
    back: () -> Unit,
    goTab: (NavTab) -> Unit,
    openChargeFlow: () -> Unit,
    stationId: String?,
    onStationSelected: (String) -> Unit,
    onShowTerms: (Terms) -> Unit,
    onWithdraw: () -> Unit,
    onFirstRunDone: () -> Unit,
    stackSize: Int,
    notifications: NotificationPermissionState,
    context: Context,
) {
    when (screen) {
        Screen.DEV_MENU -> DevMenu(go, onBack = back)
        Screen.HOME -> {
            val vm = viewModel { HomeViewModel(container.chargeRecorder.target, container.records, container.stations, container.vehicleProfile) }
            val state by vm.state.collectAsState()
            HomeScreen(
                state,
                onStations = { goTab(NavTab.STATIONS) },
                onHistory = { goTab(NavTab.HISTORY) },
                onSettings = { goTab(NavTab.SETTINGS) },
                onOpenCharge = openChargeFlow,
                onBack = back,
                onDevMenu = if (BuildConfig.DEBUG) ({ go(Screen.DEV_MENU) }) else null,
            )
        }
        Screen.STATIONS -> screenStates.SaveableStateProvider(Screen.STATIONS.name) {
            val vm = viewModel { StationListViewModel(container.signals, container.stations) }
            // 화면에 들어올 때마다 새로 조회한다(방금 끝난 충전이 반영되게)
            LaunchedEffect(Unit) { vm.refresh() }
            val state by vm.state.collectAsState()
            StationListScreen(
                state,
                onStationClick = {
                    onStationSelected(it)
                    go(Screen.STATION_DETAIL)
                },
                onHistoryClick = { goTab(NavTab.HISTORY) },
                onRetry = vm::refresh,
                onBack = back,
                onDevMenu = if (BuildConfig.DEBUG) ({ go(Screen.DEV_MENU) }) else null,
            )
        }
        Screen.SIGNALS -> {
            // 신호 수신은 동의 후 기록 서비스가 시작한다. 개발 메뉴의 신호 모니터만 들어올 때 따로 켠다
            LaunchedEffect(Unit) { container.signals.start() }
            SignalMonitorScreen(container.signals, onBack = back)
        }
        Screen.STATION_DETAIL -> {
            // 개발 메뉴에서 바로 들어오면 고른 충전소가 없다 → 시드 첫 충전소
            val id = stationId ?: PreviewData.stationRow.id
            val vm = viewModel(key = id) { StationDetailViewModel(id, container.signals, container.signals, container.stations) }
            LaunchedEffect(vm) { vm.refresh() }
            val state by vm.state.collectAsState()
            StationDetailScreen(
                state,
                onBack = back,
                onRouteClick = vm::onRouteClick,
                onConfirmRoute = vm::onConfirmRoute,
                onDismissRoute = vm::onDismissRoute,
                onRetry = vm::refresh,
            )
        }
        Screen.CHARGING -> {
            val vm = viewModel { ChargingViewModel(container.chargeTracker.live, container.stations, container.chargeRules.demoTimeScale) }
            val state by vm.state.collectAsState()
            // 세션 없이 들어오는 경우는 개발 메뉴뿐이다 → 미리보기로 그린다
            (state ?: PreviewData.charging.takeIf { BuildConfig.DEBUG })?.let { ChargingScreen(it, onBack = back) }
        }
        Screen.REVIEW -> {
            val vm = viewModel { ReviewViewModel(container.chargeRecorder, container.stations, container.chargeRules.demoTimeScale) }
            val state by vm.state.collectAsState()
            val current = state
            if (current != null) {
                ReviewScreen(current, vm::onRate, vm::onToggleTag, vm::onSubmit, vm::onLater, onBack = back)
            } else if (BuildConfig.DEBUG) {
                ReviewScreen(PreviewData.review, onRate = {}, onToggleTag = {}, onSubmit = back, onLater = back, onBack = back)
            }
        }
        Screen.FAILURE -> {
            val vm = viewModel { FailureNoticeViewModel(container.chargeRecorder, container.stations) }
            val state by vm.state.collectAsState()
            val current = state
            if (current != null) {
                FailureNoticeScreen(current, vm::onNotFailure, vm::onConfirm, onBack = back)
            } else if (BuildConfig.DEBUG) {
                FailureNoticeScreen(PreviewData.failure, onNotFailure = back, onConfirm = back, onBack = back)
            }
        }
        Screen.HISTORY -> {
            val vm = viewModel { HistoryViewModel(container.records, container.stations) }
            val state by vm.state.collectAsState()
            HistoryScreen(state, onBack = back, onWithdrawConsent = onWithdraw)
        }
        Screen.ONBOARDING, Screen.SETTINGS -> {
            // 같은 화면을 두 곳에서 쓴다: 동의 전이면 첫 실행 동의, 동의 후면 설정(저장된 동의·차종, 차종 변경·철회)
            val vm = viewModel {
                OnboardingViewModel(container.vehicleProfile, onStarted = { RecordingService.startIfAllowed(context) })
            }
            LaunchedEffect(screen) { vm.reload() }
            val state by vm.state.collectAsState()
            OnboardingScreen(
                state,
                onAgreeLocation = vm::onAgreeLocation,
                onAgreePrivacy = vm::onAgreePrivacy,
                onSelectPreset = vm::onSelectPreset,
                onShowTerms = onShowTerms,
                onBack = back,
                notificationsGranted = notifications.granted,
                onRequestNotifications = notifications.request,
                notificationsNeedSettings = notifications.openSettings,
                onWithdraw = onWithdraw,
                onStart = {
                    val wasEditing = state.editing
                    if (vm.onStart()) {
                        if (wasEditing || stackSize > 1) back() else onFirstRunDone()
                    }
                },
            )
        }
    }
}

private val ScreenStackSaver = listSaver<SnapshotStateList<Screen>, String>(
    save = { it.map(Screen::name) },
    restore = { names -> mutableStateListOf<Screen>().apply { names.forEach { add(Screen.valueOf(it)) } } },
)

/** 동의 철회 확인. 취소·바깥 누르기는 철회 아님 */
@Composable
private fun WithdrawDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    CarAlertDialog("동의를 철회할까요?", "충전 기록을 멈추고 기기의 동의·기록 정보를 지워요. 서버 기록 삭제도 요청하며, 통신 상태에 따라 완료까지 걸릴 수 있어요. 다시 쓰려면 동의가 필요해요.",
        "철회", onConfirm, onDismiss, dismissLabel = "취소", destructive = true)
}

/** 약관 전문(TermsText, 원본 docs/terms-draft.md). 닫기는 동의가 아니다 */
@Composable
private fun TermsDialog(terms: Terms, onDismiss: () -> Unit) {
    val location = terms == Terms.LOCATION
    CarAlertDialog(if (location) "위치정보 이용" else "개인정보 수집·이용", if (location) TermsText.LOCATION else TermsText.PRIVACY, "닫기", onDismiss, onDismiss)
}

@Composable
private fun DevMenu(go: (Screen) -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader("차슐랭 개발 메뉴", subtitle = "디버그 빌드", onBack = onBack)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Screen.entries.filter { it != Screen.DEV_MENU }.forEach { s ->
                Button(onClick = { go(s) }) { Text(s.title) }
            }
        }
    }
}
