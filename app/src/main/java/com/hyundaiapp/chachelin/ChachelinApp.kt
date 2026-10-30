package com.hyundaiapp.chachelin

import android.app.Application

class ChachelinApp : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // 차량 신호는 앱 프로세스가 살아 있는 동안 계속 받는다(충전 중 기록 누락 방지)
        container.signals.start()
        container.chargeTracker.start(container.appScope)
    }
}
