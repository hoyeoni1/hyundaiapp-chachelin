package com.hyundaiapp.chachelin

import android.app.Application

class ChachelinApp : Application() {
    val container by lazy { AppContainer(this) }

    // 차량·위치 신호 수신과 충전 기록은 동의 후 RecordingService(AppContainer.startRecording)가 시작한다.
    // 디버그 빌드의 신호 모니터는 화면에 들어갈 때 따로 시작한다
}
