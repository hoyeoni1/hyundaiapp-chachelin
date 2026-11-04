package com.hyundaiapp.chachelin.ui.stations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hyundaiapp.chachelin.ui.PreviewData
import com.hyundaiapp.chachelin.ui.StationDetailUiState
import com.hyundaiapp.chachelin.ui.StationRowUi
import com.hyundaiapp.chachelin.ui.components.CarAlertDialog
import com.hyundaiapp.chachelin.ui.components.CarCard
import com.hyundaiapp.chachelin.ui.components.CarIcons
import com.hyundaiapp.chachelin.ui.components.EmptyState
import com.hyundaiapp.chachelin.ui.components.MeterBar
import com.hyundaiapp.chachelin.ui.components.PrimaryButton
import com.hyundaiapp.chachelin.ui.components.ScreenHeader
import com.hyundaiapp.chachelin.ui.components.SecondaryButton
import com.hyundaiapp.chachelin.ui.components.StatTile
import com.hyundaiapp.chachelin.ui.components.formatDateTime
import com.hyundaiapp.chachelin.ui.components.formatKm
import com.hyundaiapp.chachelin.ui.components.formatOneDecimal
import com.hyundaiapp.chachelin.ui.components.formatWhole
import com.hyundaiapp.chachelin.ui.theme.CarColors
import com.hyundaiapp.chachelin.ui.theme.ChachelinTheme

/**
 * S2 충전소 상세(정차 중). 점수·숫자 | 별점 분포·태그 | 최근 실패, [길안내].
 * [길안내]는 차량 내비의 기존 경로를 덮어쓰므로(PRD 5.2) confirmRoute가 true일 때 확인창을 띄운다.
 */
@Composable
fun StationDetailScreen(
    state: StationDetailUiState,
    onBack: () -> Unit,
    onRouteClick: () -> Unit,
    onConfirmRoute: () -> Unit,
    onDismissRoute: () -> Unit,
    onRetry: () -> Unit = {},
) {
    val station = state.station
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 1000.dp
        Column(Modifier.fillMaxSize().padding(horizontal = if (maxWidth < maxHeight) 32.dp else 48.dp, vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            ScreenHeader(station?.name ?: "충전소", subtitle = station?.let { "${it.operator} · ${formatKm(it.distanceM)}" }, onBack = onBack)
            if (station == null) {
                EmptyState(if (state.loading) "충전소 정보를 불러오는 중이에요" else "충전소를 불러오지 못했어요",
                    state.error, loading = state.loading, onRetry = if (state.loading) null else onRetry,
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()))
            } else {
                if (state.loading || state.error != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(if (state.loading) "최신 정보를 확인하고 있어요" else "갱신하지 못했어요. 이전 정보를 표시해요", style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant, modifier = Modifier.weight(1f))
                    if (!state.loading) SecondaryButton("다시 시도", onRetry)
                }
                if (wide) Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { Summary(station) }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        RatingsCard(state, Modifier.fillMaxWidth())
                        FailuresCard(state.recentFailureTimesMs, Modifier.fillMaxWidth())
                    }
                } else Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Summary(station)
                    RatingsCard(state, Modifier.fillMaxWidth())
                    FailuresCard(state.recentFailureTimesMs, Modifier.fillMaxWidth())
                }
                RouteButton(onRouteClick)
            }
        }
    }
    if (state.confirmRoute && station != null) CarAlertDialog("길안내할까요?", "${station.name}\n지금 내비에 설정된 경로가 있으면 이 충전소로 바뀌어요.", "길안내 시작", onConfirmRoute, onDismissRoute, dismissLabel = "취소")
}

@Composable
private fun Summary(station: StationRowUi) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 이 화면의 주인공은 차슐랭 점수 하나. 나머지 숫자는 같은 크기 칸 셋으로 나란히
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val score = station.score
            Text(
                score?.let { formatOneDecimal(it) } ?: "평가 중",
                style = if (score != null) MaterialTheme.typography.displayLarge else MaterialTheme.typography.displaySmall,
                color = if (score != null) CarColors.Primary else CarColors.OnSurfaceVariant,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                if (score != null) "차슐랭 점수" else "리뷰 5개 미만",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                color = CarColors.OnSurfaceVariant,
                modifier = Modifier.alignByBaseline(),
            )
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val tile = Modifier.weight(1f).fillMaxHeight()
            StatTile("인증 충전", "${station.verifiedCharges}", tile, unit = "회")
            val kw = station.avgKw
            if (kw != null) StatTile("계산 평균", formatWhole(kw), tile, unit = "kW")
            else StatTile("계산 평균", "기록 없음", tile, valueStyle = MaterialTheme.typography.titleLarge, valueColor = CarColors.OnSurfaceVariant)
            StatTile(
                "7일 실패", "${station.recentFailures7d}", tile, unit = "회",
                valueColor = if (station.recentFailures7d > 0) CarColors.Warning else CarColors.OnSurface,
            )
        }
    }
}

@Composable
private fun RouteButton(onClick: () -> Unit) {
    PrimaryButton("길안내", onClick, Modifier.fillMaxWidth(), icon = CarIcons.Navigate, height = 84.dp)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RatingsCard(state: StationDetailUiState, modifier: Modifier) {
    val max = state.ratingCounts.maxOrNull()?.takeIf { it > 0 } ?: 1
    CarCard(modifier, contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("별점 분포", style = MaterialTheme.typography.titleMedium)
        // 5점이 위로 오게 뒤집어 그린다(인덱스 0 = 1점)
        for (stars in 5 downTo 1) {
            val count = state.ratingCounts.getOrElse(stars - 1) { 0 }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${stars}점", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(56.dp))
                MeterBar(count.toFloat() / max, CarColors.Primary, Modifier.weight(1f), height = 14.dp)
                Text(
                    "${count}건",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CarColors.OnSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(72.dp),
                )
            }
        }
        Text("많이 받은 태그", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        if (state.topTags.isEmpty()) {
            Text("아직 태그가 없어요", style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.topTags.forEach { tag ->
                    Surface(color = CarColors.SurfaceVariant, contentColor = CarColors.OnSurface, shape = RoundedCornerShape(999.dp)) {
                        Text(tag.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FailuresCard(times: List<Long>, modifier: Modifier) {
    CarCard(modifier, contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("최근 실패 기록 (최대 4건)", style = MaterialTheme.typography.titleMedium)
        if (times.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(CarIcons.Check, contentDescription = null, tint = CarColors.Primary, modifier = Modifier.size(26.dp))
                Text("최근 7일 실패 없음", style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant)
            }
        } else {
            times.take(MAX_FAILURES).forEach { ms ->
                Surface(color = CarColors.SurfaceVariant, contentColor = CarColors.OnSurface, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("실패", style = MaterialTheme.typography.labelMedium, color = CarColors.Warning)
                        Text(formatDateTime(ms), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private const val MAX_FAILURES = 4

@Preview(name = "S2 가로", widthDp = 1280, heightDp = 720)
@Composable
private fun StationDetailPreview() {
    ChachelinTheme { Surface { StationDetailScreen(PreviewData.stationDetail, {}, {}, {}, {}) } }
}

@Preview(name = "S2 세로", widthDp = 720, heightDp = 1280)
@Composable
private fun StationDetailPortraitPreview() {
    ChachelinTheme { Surface { StationDetailScreen(PreviewData.stationDetail, {}, {}, {}, {}) } }
}

@Preview(name = "S2 실패 있음 + 길안내 확인", widthDp = 1280, heightDp = 720)
@Composable
private fun StationDetailConfirmPreview() {
    val state = PreviewData.stationDetail.copy(
        station = PreviewData.stationRow.copy(recentFailures7d = 2),
        recentFailureTimesMs = listOf(1_793_300_000_000L, 1_793_100_000_000L),
        confirmRoute = true,
    )
    ChachelinTheme { Surface { StationDetailScreen(state, {}, {}, {}, {}) } }
}
