package com.hyundaiapp.chachelin.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/* 차슐랭에만 쓰는 포맷. 두 앱 공통 포맷은 CarComponents.kt 아래쪽 */

private val DateTimeFormat = DateTimeFormatter.ofPattern("M월 d일 HH:mm", Locale.KOREA)

/** 예: 10월 31일 18:40 */
fun formatDateTime(ms: Long): String = DateTimeFormat.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

/** 초 → "18분" / "1시간 5분" */
fun formatDuration(sec: Long): String {
    val min = sec / 60
    return if (min < 60) "${min}분" else "${min / 60}시간 ${min % 60}분"
}

/** 실측 평균 kW. 아직 계산 전(null)이면 "측정 중" */
fun formatKw(kw: Double?): String = kw?.let { "${formatWhole(it)} kW" } ?: "측정 중"
