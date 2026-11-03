package com.hyundaiapp.chachelin.ui.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hyundaiapp.chachelin.data.ReviewTag
import com.hyundaiapp.chachelin.ui.PreviewData
import com.hyundaiapp.chachelin.ui.ReviewUiState
import com.hyundaiapp.chachelin.ui.components.CarCard
import com.hyundaiapp.chachelin.ui.components.CarIcons
import com.hyundaiapp.chachelin.ui.components.PrimaryButton
import com.hyundaiapp.chachelin.ui.components.ScreenHeader
import com.hyundaiapp.chachelin.ui.components.SecondaryButton
import com.hyundaiapp.chachelin.ui.components.SelectableButton
import com.hyundaiapp.chachelin.ui.components.formatKw
import com.hyundaiapp.chachelin.ui.theme.CarColors
import com.hyundaiapp.chachelin.ui.theme.ChachelinTheme

/**
 * S4 충전 완료 리뷰(정차 중). 별점·태그는 스크롤하고 전송·건너뛰기는 고정한다.
 * 뒤로는 결과를 보류하고, 건너뛰기는 리뷰 없이 결과를 닫는다.
 */
@Composable
fun ReviewScreen(
    state: ReviewUiState,
    onRate: (Int) -> Unit,
    onToggleTag: (ReviewTag) -> Unit,
    onSubmit: () -> Unit,
    onLater: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 1000.dp
        Column(Modifier.fillMaxSize().padding(horizontal = if (wide) 48.dp else 32.dp, vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            ScreenHeader("충전 완료", onBack = onBack)
            if (wide) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { VerifiedCard(state, Modifier.fillMaxWidth()) }
                    Column(Modifier.weight(2f).verticalScroll(rememberScrollState())) { RatingAndTags(state, onRate, onToggleTag, 96.dp, 3) }
                }
            } else {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    RatingAndTags(state, onRate, onToggleTag, 96.dp, 2)
                    VerifiedCard(state, Modifier.fillMaxWidth())
                }
            }
            if (wide) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SecondaryButton("평가 건너뛰기", onLater, Modifier.weight(1f))
                PrimaryButton(submitLabel(state), onSubmit, Modifier.weight(2f), enabled = canSubmit(state))
            } else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(submitLabel(state), onSubmit, Modifier.fillMaxWidth(), enabled = canSubmit(state))
                SecondaryButton("평가 건너뛰기", onLater, Modifier.fillMaxWidth())
            }
        }
    }
}

private fun canSubmit(state: ReviewUiState) = state.rating != null && !state.submitting

private fun submitLabel(state: ReviewUiState) = when {
    state.submitting -> "보내는 중…"
    state.rating == null -> "별점을 골라 주세요"
    else -> "평가 보내기"
}

@Composable
private fun VerifiedCard(state: ReviewUiState, modifier: Modifier) {
    CarCard(
        modifier,
        border = BorderStroke(2.dp, CarColors.Primary),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(CarIcons.ShieldCheck, contentDescription = null, tint = CarColors.Primary, modifier = Modifier.size(30.dp))
            Text("인증 충전 완료", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = CarColors.Primary)
        }
        Text(state.stationName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${state.socStart}% → ${state.socEnd}%", style = MaterialTheme.typography.displaySmall)
        Text("${state.durationMin}분 · 계산 평균 ${state.avgKw?.let { formatKw(it) } ?: "기록 없음"}", style = MaterialTheme.typography.bodyLarge, color = CarColors.OnSurfaceVariant)
        Text("배터리 잔량 변화와 차종 용량으로 계산해요", style = MaterialTheme.typography.bodyMedium, color = CarColors.OnSurfaceVariant)
    }
}

@Composable
private fun RatingAndTags(
    state: ReviewUiState,
    onRate: (Int) -> Unit,
    onToggleTag: (ReviewTag) -> Unit,
    starSize: Dp,
    tagColumns: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("이 충전소 어땠나요?", style = MaterialTheme.typography.headlineMedium)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val fittedStarSize = minOf(starSize, (maxWidth - 32.dp) / 5)
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (stars in 1..5) {
                    val on = (state.rating ?: 0) >= stars
                    Surface(
                        color = CarColors.SurfaceVariant,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .size(fittedStarSize)
                            .selectable(selected = state.rating == stars, role = Role.RadioButton, onClick = { onRate(stars) }),
                    ) {
                        Icon(
                            CarIcons.Star,
                            contentDescription = "${stars}점",
                            tint = if (on) CarColors.Warning else CarColors.Track,
                            modifier = Modifier.padding(fittedStarSize * 0.2f),
                        )
                    }
                }
            }
        }
        Text(state.rating?.let { "${it}점 선택" } ?: "별점을 선택해 주세요", style = MaterialTheme.typography.bodyLarge, color = CarColors.Primary)
        ReviewTag.entries.chunked(tagColumns).forEach { rowTags ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowTags.forEach { tag ->
                    SelectableButton(
                        text = tag.label,
                        selected = tag in state.selectedTags,
                        onClick = { onToggleTag(tag) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(tagColumns - rowTags.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private val previewState = PreviewData.review.copy(rating = 5, selectedTags = setOf(ReviewTag.FAST, ReviewTag.NO_WAIT))

@Preview(name = "S4 가로", widthDp = 1280, heightDp = 720)
@Composable
private fun ReviewPreview() {
    ChachelinTheme { Surface { ReviewScreen(previewState, {}, {}, {}, {}) } }
}

@Preview(name = "S4 세로", widthDp = 720, heightDp = 1280)
@Composable
private fun ReviewPortraitPreview() {
    ChachelinTheme { Surface { ReviewScreen(previewState, {}, {}, {}, {}) } }
}

@Preview(name = "S4 별점 전", widthDp = 1280, heightDp = 720)
@Composable
private fun ReviewEmptyPreview() {
    ChachelinTheme { Surface { ReviewScreen(PreviewData.review, {}, {}, {}, {}) } }
}
