package com.hyundaiapp.chachelin.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hyundaiapp.chachelin.ui.components.CarIcons
import com.hyundaiapp.chachelin.ui.components.Text
import com.hyundaiapp.chachelin.ui.theme.CarColors

/** 정차 화면 왼쪽 고정 이동 막대의 칸(11-09 시연 피드백: 화면 이동 버튼 부재) */
enum class NavTab(val label: String) { HOME("홈"), STATIONS("충전소"), HISTORY("내 기록"), SETTINGS("설정") }

/** 왼쪽 고정 이동 막대. 어느 정차 화면에서든 홈·충전소·내 기록·설정으로 한 번에 간다 */
@Composable
fun AppNavRail(current: NavTab, onSelect: (NavTab) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.width(120.dp).fillMaxHeight().background(CarColors.Surface).padding(vertical = 24.dp, horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NavTab.entries.forEach { tab ->
            val selected = tab == current
            Surface(
                onClick = { onSelect(tab) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp)
                    .semantics { this.selected = selected; role = Role.Tab },
                shape = RoundedCornerShape(16.dp),
                color = if (selected) CarColors.SurfaceVariant else Color.Transparent,
                contentColor = if (selected) CarColors.Primary else CarColors.OnSurfaceVariant,
            ) {
                Column(
                    Modifier.padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(tab.icon, contentDescription = null, modifier = Modifier.size(36.dp))
                    Text(tab.label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
    }
}

private val NavTab.icon: ImageVector
    get() = when (this) {
        NavTab.HOME -> NavIcons.Home
        NavTab.STATIONS -> NavIcons.Pin
        NavTab.HISTORY -> CarIcons.History
        NavTab.SETTINGS -> CarIcons.Settings
    }

/** 차슐랭에만 쓰는 아이콘. 두 앱이 같이 쓰는 CarIcons는 건드리지 않는다 */
object NavIcons {
    val Home = stroke("Home", "M3 11 12 4l9 7", "M5 10v10h5v-6h4v6h5V10")
    val Pin = stroke("Pin", "M12 21s-7-6.2-7-11.5A7 7 0 0 1 19 9.5C19 14.8 12 21 12 21z", "M12 7a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5z")

    private fun stroke(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { d ->
                addPath(
                    pathData = PathParser().parsePathString(d).toNodes(),
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
}
