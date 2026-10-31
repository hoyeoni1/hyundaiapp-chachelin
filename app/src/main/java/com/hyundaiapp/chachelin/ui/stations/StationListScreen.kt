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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hyundaiapp.chachelin.ui.PreviewData
import com.hyundaiapp.chachelin.ui.StationListUiState
import com.hyundaiapp.chachelin.ui.StationRowUi
import com.hyundaiapp.chachelin.ui.components.PrimaryButton
import com.hyundaiapp.chachelin.ui.components.ScreenHeader
import com.hyundaiapp.chachelin.ui.components.SecondaryButton
import com.hyundaiapp.chachelin.ui.components.formatKm
import com.hyundaiapp.chachelin.ui.components.formatOneDecimal
import com.hyundaiapp.chachelin.ui.components.formatWhole
import com.hyundaiapp.chachelin.ui.theme.CarColors
import com.hyundaiapp.chachelin.ui.theme.CarDimens
import com.hyundaiapp.chachelin.ui.theme.ChachelinTheme

/**
 * S1 근처 충전소(정차 중). 차슐랭 점수순 목록. 화면은 UiState와 콜백만 받는다.
 * 가로: 점수 | 이름 | 숫자 3개 한 줄. 세로: 점수·이름 아래에 숫자 3개.
 */
@Composable
fun StationListScreen(
    state: StationListUiState,
    onStationClick: (String) -> Unit,
    onHistoryClick: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val portrait = maxWidth < maxHeight
        Column(
            Modifier.fillMaxSize().padding(horizontal = if (portrait) 32.dp else CarDimens.ScreenPadding, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ScreenHeader("근처 충전소", subtitle = if (portrait) null else "차슐랭 점수순") {
                SecondaryButton(if (portrait) "내 기록" else "내 충전 기록", onHistoryClick, height = CarDimens.TouchTargetSmall)
            }
            when {
                state.loading -> Centered { CircularProgressIndicator(Modifier.size(64.dp)) }
                state.error != null -> Centered {
                    Text("충전소를 불러오지 못했어요", style = MaterialTheme.typography.titleLarge)
                    Text(state.error, style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant, textAlign = TextAlign.Center)
                    PrimaryButton("다시 시도", onRetry)
                }
                state.items.isEmpty() -> Centered {
                    Text("근처 3km 안에 충전소가 없어요", style = MaterialTheme.typography.titleLarge, color = CarColors.OnSurfaceVariant)
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

@Composable
private fun StationCard(row: StationRowUi, portrait: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = CarColors.Surface,
        shape = RoundedCornerShape(CarDimens.CardRadius),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (portrait) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Score(row.score, Modifier.width(128.dp))
                    NameBlock(row, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth()) { Stats(row, Modifier.weight(1f), alignEnd = false) }
            }
        } else {
            Row(
                Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Score(row.score, Modifier.width(112.dp))
                NameBlock(row, Modifier.weight(1f))
                Stats(row, Modifier.width(IntrinsicStatsWidth), alignEnd = true)
            }
        }
    }
}

private val IntrinsicStatsWidth = 380.dp

/** 차슐랭 점수. null이면 "평가 중"(리뷰 5개 미만, PRD 5.5) */
@Composable
private fun Score(score: Double?, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (score != null) {
            Text(formatOneDecimal(score), style = MaterialTheme.typography.displaySmall, color = CarColors.Primary)
            Text("차슐랭", style = MaterialTheme.typography.bodySmall, color = CarColors.OnSurfaceVariant)
        } else {
            Text("평가 중", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = CarColors.OnSurfaceVariant)
            Text("리뷰 5개 미만", style = MaterialTheme.typography.bodySmall, color = CarColors.OnSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun NameBlock(row: StationRowUi, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(row.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            "${row.operator} · ${formatKm(row.distanceM)}",
            style = MaterialTheme.typography.bodyMedium,
            color = CarColors.OnSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Stats(row: StationRowUi, modifier: Modifier, alignEnd: Boolean) {
    Row(modifier, horizontalArrangement = Arrangement.SpaceBetween) {
        Stat("인증 충전", "${row.verifiedCharges}회", CarColors.OnSurface, alignEnd)
        Stat("실측 평균", "${formatWhole(row.avgKw)}kW", CarColors.OnSurface, alignEnd)
        Stat(
            "7일 실패",
            "${row.recentFailures7d}회",
            if (row.recentFailures7d > 0) CarColors.Warning else CarColors.OnSurface,
            alignEnd,
        )
    }
}

@Composable
private fun Stat(label: String, value: String, color: Color, alignEnd: Boolean) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Preview(name = "S1 가로", widthDp = 1280, heightDp = 720)
@Composable
private fun StationListPreview() {
    ChachelinTheme { Surface { StationListScreen(PreviewData.stationList, {}) } }
}

@Preview(name = "S1 세로", widthDp = 720, heightDp = 1280)
@Composable
private fun StationListPortraitPreview() {
    ChachelinTheme { Surface { StationListScreen(PreviewData.stationList, {}) } }
}

@Preview(name = "S1 오류", widthDp = 1280, heightDp = 720)
@Composable
private fun StationListErrorPreview() {
    ChachelinTheme { Surface { StationListScreen(StationListUiState(loading = false, error = "네트워크 연결 없음"), {}) } }
}
