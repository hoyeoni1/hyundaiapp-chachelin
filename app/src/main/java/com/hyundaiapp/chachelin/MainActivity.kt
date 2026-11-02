package com.hyundaiapp.chachelin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hyundaiapp.chachelin.ui.ChachelinNav
import com.hyundaiapp.chachelin.ui.theme.ChachelinTheme

// DO(주행 중 사용) 미선언 앱: 주행 중에는 플랫폼이 이 화면을 막는다(PRD 3.2)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as ChachelinApp).container
        // 동의를 마친 상태면 기록 서비스를 켠다(앞에 있을 때만 포그라운드 서비스를 시작할 수 있음)
        RecordingService.startIfAllowed(this)
        setContent {
            ChachelinTheme {
                ChachelinNav(container)
            }
        }
    }
}
