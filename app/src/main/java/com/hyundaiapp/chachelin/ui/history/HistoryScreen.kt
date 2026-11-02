package com.hyundaiapp.chachelin.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hyundaiapp.chachelin.ui.HistoryRowUi
import com.hyundaiapp.chachelin.ui.HistoryUiState
import com.hyundaiapp.chachelin.ui.PreviewData
import com.hyundaiapp.chachelin.ui.components.PillKind
import com.hyundaiapp.chachelin.ui.components.ScreenHeader
import com.hyundaiapp.chachelin.ui.components.SecondaryButton
import com.hyundaiapp.chachelin.ui.components.StatusPill
import com.hyundaiapp.chachelin.ui.components.formatDateTime
import com.hyundaiapp.chachelin.ui.theme.CarColors
import com.hyundaiapp.chachelin.ui.theme.CarDimens
import com.hyundaiapp.chachelin.ui.theme.ChachelinTheme

/**
 * S6 내 충전 기록(정차 중). 인증·실패·기록만(충전소 근처가 아니어서 점수에 안 들어간 충전)을 글자로 구분한다.
 */
@Composable
fun HistoryScreen(state: HistoryUiState, onBack: () -> Unit, onWithdrawConsent: () -> Unit = {}) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val portrait = maxWidth < maxHeight
        Column(
            Modifier.fillMaxSize().padding(horizontal = if (portrait) 32.dp else CarDimens.ScreenPadding, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ScreenHeader("내 충전 기록", onBack = onBack) {
                SecondaryButton("동의 철회", onWithdrawConsent, height = CarDimens.TouchTargetSmall)
            }
            if (state.items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "아직 기록이 없어요.\n충전구를 열면 자동으로 기록을 시작해요.",
                        style = MaterialTheme.typography.titleMedium,
                        color = CarColors.OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.items, key = { it.sessionId }) { HistoryRow(it, portrait) }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(row: HistoryRowUi, portrait: Boolean) {
    val name = row.stationName ?: "충전소 아님 (집 충전 등)"
    val nameColor = if (row.stationName == null) CarColors.OnSurfaceVariant else CarColors.OnSurface
    Surface(color = CarColors.Surface, shape = RoundedCornerShape(CarDimens.CardRadius), modifier = Modifier.fillMaxWidth()) {
        if (portrait) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDateTime(row.startedAtMs), style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant, modifier = Modifier.weight(1f))
                    ResultPill(row)
                }
                Text(name, style = MaterialTheme.typography.titleMedium, color = nameColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${row.socStart}% → ${row.socEnd}%", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Row(
                Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(formatDateTime(row.startedAtMs), style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant, modifier = Modifier.width(170.dp))
                Text(name, style = MaterialTheme.typography.titleMedium, color = nameColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("${row.socStart}% → ${row.socEnd}%", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End, modifier = Modifier.width(180.dp))
                Box(Modifier.width(120.dp), contentAlignment = Alignment.Center) { ResultPill(row) }
            }
        }
    }
}

@Composable
private fun ResultPill(row: HistoryRowUi) = when {
    row.verified -> StatusPill("인증", PillKind.PRIMARY)
    row.failed -> StatusPill("실패", PillKind.WARNING)
    else -> StatusPill("기록만", PillKind.OUTLINED)
}

private val previewState = HistoryUiState(
    PreviewData.history.items + HistoryRowUi("s-3", null, 0L, 40, 90, verified = false, failed = false),
)

@Preview(name = "S6 가로", widthDp = 1280, heightDp = 720)
@Composable
private fun HistoryPreview() {
    ChachelinTheme { Surface { HistoryScreen(previewState, onBack = {}) } }
}

@Preview(name = "S6 세로", widthDp = 720, heightDp = 1280)
@Composable
private fun HistoryPortraitPreview() {
    ChachelinTheme { Surface { HistoryScreen(previewState, onBack = {}) } }
}
