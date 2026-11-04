package com.hyundaiapp.chachelin.ui.charging

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.hyundaiapp.chachelin.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hyundaiapp.chachelin.ui.ChargingUiState
import com.hyundaiapp.chachelin.ui.PreviewData
import com.hyundaiapp.chachelin.ui.components.CarCard
import com.hyundaiapp.chachelin.ui.components.ScreenHeader
import com.hyundaiapp.chachelin.ui.components.StatTile
import com.hyundaiapp.chachelin.ui.components.formatDuration
import com.hyundaiapp.chachelin.ui.components.formatWhole
import com.hyundaiapp.chachelin.ui.theme.CarColors
import com.hyundaiapp.chachelin.ui.theme.CarDimens
import com.hyundaiapp.chachelin.ui.theme.ChachelinTheme

/** 시작 SoC 구간을 나머지와 구분하는 어두운 레드 */
private val StartSegment = Color(0xFF9C2C38)

/**
 * S3 충전 중(정차 중). SoC를 크게, 경과 시간·지금까지 계산 평균을 옆에.
 * 뒤로 가도 기록은 이어진다. 충전구를 닫으면 ViewModel이 S4/S5로 넘긴다.
 */
@Composable
fun ChargingScreen(state: ChargingUiState, onBack: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val portrait = maxWidth < maxHeight
        Column(
            Modifier.fillMaxSize().padding(horizontal = if (portrait) 32.dp else CarDimens.ScreenPadding, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            ScreenHeader("충전 기록 중", subtitle = state.stationName, onBack = onBack)
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
            ) {
                if (portrait) {
                    SocCard(state, Modifier.fillMaxWidth())
                    Side(state, Modifier.fillMaxWidth(), vertical = false)
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.Top) {
                        SocCard(state, Modifier.weight(3f))
                        Side(state, Modifier.weight(2f), vertical = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun SocCard(state: ChargingUiState, modifier: Modifier) {
    val gained = state.socNow - state.socStart
    CarCard(
        modifier.semantics { contentDescription = "배터리 ${state.socNow}%, 시작 ${state.socStart}%" },
        contentPadding = PaddingValues(40.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${state.socNow}%", style = MaterialTheme.typography.displayLarge)
            Text(
                "시작 ${state.socStart}%에서 ${if (gained >= 0) "+" else ""}${gained}%p",
                style = MaterialTheme.typography.titleMedium,
                color = CarColors.OnSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp),
            )
        }
        val shape = RoundedCornerShape(16.dp)
        Box(Modifier.fillMaxWidth().height(32.dp).clip(shape).background(CarColors.Track)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(state.socNow.coerceIn(0, 100) / 100f).clip(shape).background(CarColors.Primary))
            Box(Modifier.fillMaxHeight().fillMaxWidth(state.socStart.coerceIn(0, 100) / 100f).background(StartSegment))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("0%", style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant)
            Text("100%", style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant)
        }
    }
}

@Composable
private fun Side(state: ChargingUiState, modifier: Modifier, vertical: Boolean) {
    val big = MaterialTheme.typography.displayMedium
    val centered = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    // 1시간 미만은 숫자만 크게 + "분"을 단위로, 넘으면 "1시간 5분" 통째로
    val minutes = state.elapsedSec / 60
    val elapsed: @Composable (Modifier) -> Unit = { m ->
        if (minutes < 60) {
            StatTile("경과 시간", "$minutes", m, unit = "분", valueStyle = big, verticalArrangement = centered)
        } else {
            StatTile("경과 시간", formatDuration(state.elapsedSec), m, valueStyle = MaterialTheme.typography.displaySmall, valueMaxLines = 2, verticalArrangement = centered)
        }
    }
    val speed: @Composable (Modifier) -> Unit = { m ->
        StatTile("지금까지 계산 평균", state.avgKw?.let { formatWhole(it) } ?: "측정 중", m, unit = state.avgKw?.let { "kW" }, valueStyle = big, verticalArrangement = centered)
    }
    if (vertical) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            elapsed(Modifier.fillMaxWidth())
            speed(Modifier.fillMaxWidth())
        }
    } else {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            elapsed(Modifier.weight(1f))
            speed(Modifier.weight(1f))
        }
    }
}

@Preview(name = "S3 가로", widthDp = 1280, heightDp = 720)
@Composable
private fun ChargingPreview() {
    ChachelinTheme { Surface { ChargingScreen(PreviewData.charging, onBack = {}) } }
}

@Preview(name = "S3 세로", widthDp = 720, heightDp = 1280)
@Composable
private fun ChargingPortraitPreview() {
    ChachelinTheme { Surface { ChargingScreen(PreviewData.charging, onBack = {}) } }
}
