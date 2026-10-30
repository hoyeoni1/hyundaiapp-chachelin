package com.hyundaiapp.chachelin.ui

import com.hyundaiapp.chachelin.data.ReviewTag

/** @Preview 와 화면 개발용 샘플. 실제 데이터 없이 화면을 먼저 만든다 */
object PreviewData {
    val stationRow = StationRowUi(
        id = "st-001", name = "판교 테크노밸리 E-pit", operator = "현대자동차(E-pit)", distanceM = 820.0,
        score = 4.8, verifiedCharges = 128, avgKw = 68.0, recentFailures7d = 0,
    )
    val stationList = StationListUiState(
        loading = false,
        items = listOf(
            stationRow,
            stationRow.copy(id = "st-002", name = "판교역 공영주차장", operator = "환경부", score = 4.1, verifiedCharges = 64, avgKw = 52.0, recentFailures7d = 1),
            stationRow.copy(id = "st-004", name = "대왕판교IC 휴게소", operator = "GS차지비", score = null, verifiedCharges = 3, avgKw = 88.0),
        ),
    )
    val stationDetail = StationDetailUiState(stationRow, listOf(1, 2, 5, 30, 90), listOf(ReviewTag.FAST, ReviewTag.NO_WAIT))
    val charging = ChargingUiState("판교 테크노밸리 E-pit", socStart = 20, socNow = 54, elapsedSec = 18 * 60, avgKw = 63.0)
    val review = ReviewUiState("s-1", "판교 테크노밸리 E-pit", 20, 80, 32, 63.0)
    val failure = FailureNoticeUiState("s-2", "시흥동 주민센터")
    val history = HistoryUiState(
        listOf(
            HistoryRowUi("s-1", "판교 테크노밸리 E-pit", 0L, 20, 80, verified = true, failed = false),
            HistoryRowUi("s-2", "시흥동 주민센터", 0L, 35, 35, verified = false, failed = true),
        ),
    )
}
