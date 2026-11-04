package com.hyundaiapp.chachelin.ui.stations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.hyundaiapp.chachelin.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hyundaiapp.chachelin.ui.PreviewData
import com.hyundaiapp.chachelin.ui.StationListUiState
import com.hyundaiapp.chachelin.ui.StationRowUi
import com.hyundaiapp.chachelin.ui.components.CarIcons
import com.hyundaiapp.chachelin.ui.components.HeaderIconButton
import com.hyundaiapp.chachelin.ui.components.EmptyState
import com.hyundaiapp.chachelin.ui.components.ScreenHeader
import com.hyundaiapp.chachelin.ui.components.formatKm
import com.hyundaiapp.chachelin.ui.components.formatKw
import com.hyundaiapp.chachelin.ui.components.formatOneDecimal
import com.hyundaiapp.chachelin.ui.theme.CarColors
import com.hyundaiapp.chachelin.ui.theme.CarDimens
import com.hyundaiapp.chachelin.ui.theme.ChachelinTheme

/**
 * S1 근처 충전소(정차 중). 차슐랭 점수순 목록. 화면은 UiState와 콜백만 받는다.
 * 가로: 점수 | 이름·거리 | 평균 kW·인증 수. 세로: 평균 kW·인증 수를 이름 아래로.
 */
@Composable
fun StationListScreen(
    state: StationListUiState,
    onStationClick: (String) -> Unit,
    onHistoryClick: () -> Unit = {},
    onRetry: () -> Unit = {},
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val portrait = maxWidth < 1000.dp
        Column(
            Modifier.fillMaxSize().padding(horizontal = if (portrait) 32.dp else CarDimens.ScreenPadding, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ScreenHeader("근처 충전소", onBack = onBack, inlineActions = true) {
                HeaderIconButton(CarIcons.History, "내 충전 기록", onHistoryClick)
            }
            when {
                state.loading -> EmptyState("충전소를 불러오는 중이에요", loading = true, modifier = Modifier.weight(1f))
                state.error != null -> EmptyState("충전소를 불러오지 못했어요", state.error, onRetry = onRetry, modifier = Modifier.weight(1f))
                state.items.isEmpty() -> EmptyState("근처 3km 안에 충전소가 없어요", onRetry = onRetry, modifier = Modifier.weight(1f))
                else -> LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.items, key = { it.id }) { row ->
                        StationCard(row, portrait) { onStationClick(row.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
    }
}

/**
 * 충전소 한 줄. 점수(빨간 별) → 이름·거리 → 평균 kW·인증 수 순서로 크기를 줄인다.
 * 칸마다 붙던 라벨은 빼고, 7일 실패는 있을 때만 주황 경고로 보여 준다.
 */
@Composable
private fun StationCard(row: StationRowUi, portrait: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = CarColors.Surface,
        shape = RoundedCornerShape(CarDimens.CardRadius),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Score(row.score, Modifier.width(if (portrait) 120.dp else 136.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(row.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${formatKm(row.distanceM)} · ${row.operator}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CarColors.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (portrait) Facts(row, alignEnd = false)
                Failures(row.recentFailures7d)
            }
            if (!portrait) Facts(row, alignEnd = true)
            Icon(CarIcons.ChevronRight, contentDescription = "충전소 상세", modifier = Modifier.size(28.dp), tint = CarColors.OnSurfaceVariant)
        }
    }
}

/** 차슐랭 점수. null이면 "평가 중"(리뷰 5개 미만, PRD 5.5) */
@Composable
private fun Score(score: Double?, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            CarIcons.Star,
            contentDescription = "차슐랭 점수",
            tint = if (score != null) CarColors.Primary else CarColors.Track,
            modifier = Modifier.size(32.dp),
        )
        if (score != null) {
            Text(formatOneDecimal(score), style = MaterialTheme.typography.displaySmall, color = CarColors.Primary, maxLines = 1)
        } else {
            Text("평가 중", style = MaterialTheme.typography.labelMedium, color = CarColors.OnSurfaceVariant, maxLines = 1)
        }
    }
}

/** 평균 kW를 크게, 인증 충전 수를 작게. 가로는 오른쪽 끝, 세로는 이름 아래 한 줄 */
@Composable
private fun Facts(row: StationRowUi, alignEnd: Boolean) {
    val kw = @Composable {
        Text(
            row.avgKw?.let { formatKw(it) } ?: "kW 기록 없음",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (row.avgKw != null) CarColors.OnSurface else CarColors.OnSurfaceVariant,
            maxLines = 1,
        )
    }
    val verified = @Composable {
        Text("인증 ${row.verifiedCharges}회", style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant, maxLines = 1)
    }
    if (alignEnd) {
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) { kw(); verified() }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) { kw(); verified() }
    }
}

@Composable
private fun Failures(count: Int) {
    if (count <= 0) return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(CarIcons.Warning, contentDescription = null, tint = CarColors.Warning, modifier = Modifier.size(22.dp))
        Text("7일 실패 ${count}회", style = MaterialTheme.typography.labelMedium, color = CarColors.Warning, maxLines = 1)
    }
}

@Preview(name = "S1 가로", widthDp = 1280, heightDp = 720)
@Composable
private fun StationListPreview() {
    ChachelinTheme { Surface { StationListScreen(PreviewData.stationList, {}, onBack = {}) } }
}

@Preview(name = "S1 세로", widthDp = 720, heightDp = 1280)
@Composable
private fun StationListPortraitPreview() {
    ChachelinTheme { Surface { StationListScreen(PreviewData.stationList, {}, onBack = {}) } }
}

@Preview(name = "S1 오류", widthDp = 1280, heightDp = 720)
@Composable
private fun StationListErrorPreview() {
    ChachelinTheme { Surface { StationListScreen(StationListUiState(loading = false, error = "네트워크 연결 없음"), {}, onBack = {}) } }
}
