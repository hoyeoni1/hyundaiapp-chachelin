package com.hyundaiapp.chachelin.ui.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.hyundaiapp.chachelin.ui.components.SecondaryButton
import com.hyundaiapp.chachelin.ui.components.SelectableButton
import com.hyundaiapp.chachelin.ui.components.StatTile
import com.hyundaiapp.chachelin.ui.components.formatWhole
import com.hyundaiapp.chachelin.ui.theme.CarColors
import com.hyundaiapp.chachelin.ui.theme.CarDimens
import com.hyundaiapp.chachelin.ui.theme.ChachelinTheme

/**
 * S4 충전 완료 리뷰(정차 중). 인증 카드 + 별점 1~5 + 태그 프리셋 + [완료]. 키보드 없음(PRD 3.1 F5).
 * 별점을 고르기 전에는 [완료]를 막는다. [나중에]는 리뷰 없이 닫는다.
 */
@Composable
fun ReviewScreen(
    state: ReviewUiState,
    onRate: (Int) -> Unit,
    onToggleTag: (ReviewTag) -> Unit,
    onSubmit: () -> Unit,
    onLater: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val portrait = maxWidth < maxHeight
        if (portrait) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 40.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                VerifiedCard(state, Modifier.fillMaxWidth(), compact = true)
                RatingAndTags(state, onRate, onToggleTag, starSize = 112.dp, tagColumns = 2)
                Spacer(Modifier.weight(1f))
                PrimaryButton(submitLabel(state), onSubmit, Modifier.fillMaxWidth(), enabled = canSubmit(state), height = 84.dp)
                SecondaryButton("나중에", onLater, Modifier.fillMaxWidth())
            }
        } else {
            Row(
                Modifier.fillMaxSize().padding(horizontal = CarDimens.ScreenPadding, vertical = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                VerifiedCard(state, Modifier.weight(2f).fillMaxHeight(), compact = false)
                Column(Modifier.weight(3f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    RatingAndTags(state, onRate, onToggleTag, starSize = 96.dp, tagColumns = 3)
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SecondaryButton("나중에", onLater, Modifier.weight(1f))
                        PrimaryButton(submitLabel(state), onSubmit, Modifier.weight(2f), enabled = canSubmit(state))
                    }
                }
            }
        }
    }
}

private fun canSubmit(state: ReviewUiState) = state.rating != null && !state.submitting

private fun submitLabel(state: ReviewUiState) = when {
    state.submitting -> "보내는 중…"
    state.rating == null -> "별점을 골라 주세요"
    else -> "완료"
}

@Composable
private fun VerifiedCard(state: ReviewUiState, modifier: Modifier, compact: Boolean) {
    CarCard(
        modifier,
        border = BorderStroke(2.dp, CarColors.Primary),
        contentPadding = PaddingValues(if (compact) 28.dp else 32.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(CarIcons.ShieldCheck, contentDescription = null, tint = CarColors.Primary, modifier = Modifier.size(30.dp))
            Text("인증 충전 완료", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = CarColors.Primary)
        }
        Text(state.stationName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${state.socStart}% → ${state.socEnd}%", style = MaterialTheme.typography.displayMedium)
        if (compact) {
            Text(
                "${state.durationMin}분 · 실측 평균 ${formatWhole(state.avgKw)}kW",
                style = MaterialTheme.typography.bodyLarge,
                color = CarColors.OnSurfaceVariant,
            )
        } else {
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("충전 시간", "${state.durationMin}분", Modifier.weight(1f), color = CarColors.SurfaceVariant)
                StatTile("실측 평균", "${formatWhole(state.avgKw)}kW", Modifier.weight(1f), color = CarColors.SurfaceVariant)
            }
        }
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
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (stars in 1..5) {
                val on = (state.rating ?: 0) >= stars
                Surface(
                    color = CarColors.SurfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .size(starSize)
                        .selectable(selected = state.rating == stars, role = Role.RadioButton, onClick = { onRate(stars) }),
                ) {
                    Icon(
                        CarIcons.Star,
                        contentDescription = "${stars}점",
                        tint = if (on) CarColors.Warning else CarColors.Track,
                        modifier = Modifier.padding(starSize * 0.2f),
                    )
                }
            }
        }
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
