package com.hyundaiapp.chachelin.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyundaiapp.chachelin.AppContainer
import com.hyundaiapp.chachelin.BuildConfig
import com.hyundaiapp.chachelin.ui.debug.SignalMonitorScreen
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
    BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.lastIndex) }

    Surface(Modifier.fillMaxSize()) {
        when (stack.last()) {
            Screen.DEV_MENU -> DevMenu(go)
            Screen.STATIONS -> {
                val vm = viewModel { StationListViewModel(container.signals, container.stations) }
                val state by vm.state.collectAsState()
                StationListScreen(state, onStationClick = { go(Screen.STATION_DETAIL) })
            }
            Screen.SIGNALS -> SignalMonitorScreen(container.signals)
            // 아래는 아직 ViewModel이 없다 → 미리보기 데이터로 상태만 보여 준다
            Screen.STATION_DETAIL -> Placeholder(Screen.STATION_DETAIL, PreviewData.stationDetail)
            Screen.CHARGING -> Placeholder(Screen.CHARGING, PreviewData.charging)
            Screen.REVIEW -> Placeholder(Screen.REVIEW, PreviewData.review)
            Screen.FAILURE -> Placeholder(Screen.FAILURE, PreviewData.failure)
            Screen.HISTORY -> Placeholder(Screen.HISTORY, PreviewData.history)
            Screen.ONBOARDING -> Placeholder(Screen.ONBOARDING, OnboardingUiState())
        }
    }
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

@Composable
private fun Placeholder(screen: Screen, state: Any) {
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text(screen.title, style = MaterialTheme.typography.headlineMedium)
        Text("화면 미구현 — 아래 UiState로 그린다", modifier = Modifier.padding(vertical = 12.dp))
        Text(state.toString())
    }
}
