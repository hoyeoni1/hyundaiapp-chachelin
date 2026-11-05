package com.hyundaiapp.chachelin.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.hyundaiapp.chachelin.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hyundaiapp.chachelin.ui.FailureNoticeUiState
import com.hyundaiapp.chachelin.ui.PreviewData
import com.hyundaiapp.chachelin.ui.components.CarCard
import com.hyundaiapp.chachelin.ui.components.CarIcons
import com.hyundaiapp.chachelin.ui.components.PrimaryButton
import com.hyundaiapp.chachelin.ui.components.SecondaryButton
import com.hyundaiapp.chachelin.ui.components.ScreenHeader
import com.hyundaiapp.chachelin.ui.theme.CarColors
import com.hyundaiapp.chachelin.ui.theme.ChachelinTheme

/**
 * S5 실패 기록 알림(정차 중). 운전자가 아무것도 안 해도 실패는 이미 기록됐다는 안내(PRD 3.1 F4).
 * [실패 아님]은 잘못 잡힌 기록을 취소한다.
 */
@Composable
fun FailureNoticeScreen(
    state: FailureNoticeUiState,
    onNotFailure: () -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(CarColors.Scrim).padding(horizontal = if (maxWidth < maxHeight) 32.dp else 48.dp, vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            ScreenHeader("충전 결과", onBack = onBack)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CarCard(
                    Modifier.widthIn(max = 760.dp).fillMaxWidth().heightIn(max = 900.dp),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.size(96.dp).background(CarColors.Warning, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(CarIcons.BoltOff, contentDescription = null, tint = CarColors.OnAccent, modifier = Modifier.size(52.dp))
                        }
                        Text("충전 실패를 기록했어요", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                        Text(
                            state.stationName,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "충전구를 열었지만\n배터리가 오르지 않았어요.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = CarColors.OnSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SecondaryButton("실패 기록 취소", onNotFailure, Modifier.weight(1f))
                        PrimaryButton("닫기", onConfirm, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Preview(name = "S5 실패 알림", widthDp = 1280, heightDp = 720)
@Composable
private fun FailureNoticePreview() {
    ChachelinTheme { Surface { FailureNoticeScreen(PreviewData.failure, {}, {}, onBack = {}) } }
}

@Preview(name = "S5 세로", widthDp = 720, heightDp = 1280)
@Composable
private fun FailureNoticePortraitPreview() {
    ChachelinTheme { Surface { FailureNoticeScreen(PreviewData.failure, {}, {}, onBack = {}) } }
}
