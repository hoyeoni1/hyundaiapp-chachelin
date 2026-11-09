package com.hyundaiapp.chachelin.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hyundaiapp.chachelin.ui.HistoryRowUi
import com.hyundaiapp.chachelin.ui.HomeChargeStatus
import com.hyundaiapp.chachelin.ui.HomeUiState
import com.hyundaiapp.chachelin.ui.components.CarCard
import com.hyundaiapp.chachelin.ui.components.CarIcons
import com.hyundaiapp.chachelin.ui.components.PrimaryButton
import com.hyundaiapp.chachelin.ui.components.ScreenHeader
import com.hyundaiapp.chachelin.ui.components.Text
import com.hyundaiapp.chachelin.ui.components.formatDateTime
import com.hyundaiapp.chachelin.ui.theme.CarColors
import com.hyundaiapp.chachelin.ui.theme.CarDimens
import com.hyundaiapp.chachelin.ui.theme.ChachelinTheme

/**
 * 홈(정차 중). 지금 충전 상태를 맨 위에 두고, 근처 충전소·내 충전 기록·설정으로 바로 가는 큰 버튼을 둔다.
 * 동의를 마친 뒤 앱을 열면 여기서 시작한다. 디버그 빌드는 제목을 길게 누르면 개발 메뉴.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onStations: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onOpenCharge: () -> Unit,
    onBack: () -> Unit,
    onDevMenu: (() -> Unit)? = null,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 900.dp
        Column(
            Modifier.fillMaxSize().padding(horizontal = if (wide) CarDimens.ScreenPadding else 32.dp, vertical = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ScreenHeader(
                "차슐랭",
                subtitle = "실제로 충전한 사람만 평가하는 충전소 가이드",
                onBack = onBack,
                onTitleLongPress = onDevMenu,
            )
            ChargeStatusCard(state.charge, onOpenCharge)
            val tiles: List<@Composable (Modifier) -> Unit> = listOf(
                { m -> HomeTile(NavIcons.Pin, "근처 충전소", "차슐랭 점수순으로 보고 길안내", onStations, m) },
                { m -> HomeTile(CarIcons.History, "내 충전 기록", if (state.recordCount > 0) "${state.recordCount}건" else "아직 기록이 없어요", onHistory, m) },
                { m -> HomeTile(CarIcons.Settings, "설정", state.carLabel?.let { "내 차 $it · 동의 관리" } ?: "내 차 · 동의 관리", onSettings, m) },
            )
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) { tiles.forEach { it(Modifier.weight(1f)) } }
            } else {
                tiles.forEach { it(Modifier.fillMaxWidth()) }
            }
            state.lastRecord?.let { LastRecord(it) }
        }
    }
}

@Composable
private fun ChargeStatusCard(status: HomeChargeStatus, onOpenCharge: () -> Unit) {
    val (title, body, action) = when (status) {
        HomeChargeStatus.CHARGING -> Triple("충전 기록 중", "SoC와 계산 평균 속도를 보고 있어요", "충전 화면 보기")
        HomeChargeStatus.REVIEW -> Triple("방금 끝난 충전을 평가해 주세요", "별점 하나면 3초에 끝나요", "평가하기")
        HomeChargeStatus.FAILURE -> Triple("충전 실패를 기록했어요", "실패가 아니면 취소할 수 있어요", "확인하기")
        HomeChargeStatus.IDLE -> Triple("충전구를 열면 자동으로 기록해요", "충전이 끝나면 평가 화면이 떠요", null)
    }
    val active = status != HomeChargeStatus.IDLE
    CarCard(
        Modifier.fillMaxWidth(),
        border = if (active) BorderStroke(2.dp, CarColors.Primary) else null,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Icon(CarIcons.Bolt, contentDescription = null, tint = if (active) CarColors.Primary else CarColors.OnSurfaceVariant, modifier = Modifier.size(40.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodyLarge, color = CarColors.OnSurfaceVariant)
            }
            if (action != null) PrimaryButton(action, onOpenCharge)
        }
    }
}

@Composable
private fun HomeTile(icon: ImageVector, title: String, body: String, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 180.dp),
        shape = RoundedCornerShape(CarDimens.CardRadius),
        color = CarColors.Surface,
        contentColor = CarColors.OnSurface,
    ) {
        Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = CarColors.Primary, modifier = Modifier.size(44.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant)
        }
    }
}

@Composable
private fun LastRecord(row: HistoryRowUi) {
    val result = when {
        row.verified -> "인증"
        row.failed -> "실패"
        else -> "기록만"
    }
    Text(
        "최근 기록 · ${formatDateTime(row.startedAtMs)} · ${row.stationName ?: "충전소 아님"} · ${row.socStart}% → ${row.socEnd}% · $result",
        style = MaterialTheme.typography.bodyMedium,
        color = CarColors.OnSurfaceVariant,
    )
}

@Preview(name = "홈 가로", widthDp = 1160, heightDp = 720)
@Composable
private fun HomePreview() {
    ChachelinTheme {
        Surface {
            HomeScreen(HomeUiState(charge = HomeChargeStatus.REVIEW, carLabel = "아이오닉 5", recordCount = 3), {}, {}, {}, {}, onBack = {})
        }
    }
}

@Preview(name = "홈 세로", widthDp = 720, heightDp = 1280)
@Composable
private fun HomePortraitPreview() {
    ChachelinTheme { Surface { HomeScreen(HomeUiState(), {}, {}, {}, {}, onBack = {}) } }
}
