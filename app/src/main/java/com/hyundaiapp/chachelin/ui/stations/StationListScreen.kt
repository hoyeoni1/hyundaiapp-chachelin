package com.hyundaiapp.chachelin.ui.stations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hyundaiapp.chachelin.ui.PreviewData
import com.hyundaiapp.chachelin.ui.StationListUiState
import com.hyundaiapp.chachelin.ui.StationRowUi
import com.hyundaiapp.chachelin.ui.theme.ChachelinTheme

// 임시 화면(동작 확인용). 디자인은 길민종이 교체한다. 화면은 UiState와 콜백만 받는다
@Composable
fun StationListScreen(state: StationListUiState, onStationClick: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("근처 충전소", style = MaterialTheme.typography.headlineMedium)
        when {
            state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
            state.error != null -> Text("불러오지 못했어요: ${state.error}")
            state.items.isEmpty() -> Text("근처에 충전소가 없어요")
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 16.dp),
            ) {
                items(state.items, key = { it.id }) { StationRow(it, onStationClick) }
            }
        }
    }
}

@Composable
private fun StationRow(row: StationRowUi, onClick: (String) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick(row.id) }) {
        Column(Modifier.padding(16.dp)) {
            Text("${row.name} · ${row.score?.let { "%.1f".format(it) } ?: "평가 중"}", style = MaterialTheme.typography.titleLarge)
            Text(
                "인증 충전 ${row.verifiedCharges}회 · 실측 평균 ${row.avgKw?.let { "%.0f".format(it) } ?: "-"}kW · " +
                    "최근 7일 실패 ${row.recentFailures7d}회 · ${row.distanceM?.let { "%.1f".format(it / 1000) } ?: "-"}km",
            )
        }
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun StationListPreview() {
    ChachelinTheme { Surface { StationListScreen(PreviewData.stationList) {} } }
}
