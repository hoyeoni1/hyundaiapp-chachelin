package com.hyundaiapp.chachelin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat

/**
 * 충전 기록 포그라운드 서비스(PRD 8절, 크롱 docs/background-freeze-experiment.md와 같은 해법). 담당: 이상준.
 * 충전 중 앱이 뒤로 가도 프로세스가 얼지 않게 해서 충전구·SoC 신호를 계속 받는다.
 *
 * 시작: S7 동의 직후·동의된 상태로 앱을 열 때([MainActivity]), 시동(부팅) 때([BootReceiver]).
 * 동의 전에는 시작하지 않고, 동의를 철회하면([AppContainer.withdrawConsent]) 멈춘다.
 */
class RecordingService : Service() {
    private val container get() = (application as ChachelinApp).container

    override fun onCreate() {
        super.onCreate()
        // startForegroundService로 시작됐으면 5초 안에 불러야 하므로 동의 확인보다 먼저 부른다.
        // minSdk 28: 3인자 startForeground는 API 29+(ServiceCompat이 나눠 부름), specialUse 타입은 API 34+
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), type)
        // 시스템이 다시 살릴 때(START_STICKY)도 동의를 다시 확인한다
        if (!container.vehicleProfile.onboarded.value) {
            stopSelf()
            return
        }
        container.startRecording()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!container.vehicleProfile.onboarded.value) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        container.stopRecording()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "충전 기록", NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("충전을 자동으로 기록해요")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "recording"
        private const val NOTIFICATION_ID = 1

        /** 동의를 마쳤을 때만 시작한다. 이미 돌고 있으면 아무 일도 안 한다 */
        fun startIfAllowed(context: Context) {
            val container = (context.applicationContext as ChachelinApp).container
            if (!container.vehicleProfile.onboarded.value) return
            context.startForegroundService(Intent(context, RecordingService::class.java))
        }
    }
}

/** 시동(부팅) 때 자동 기록. S7 동의를 마쳤을 때만 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        RecordingService.startIfAllowed(context)
    }
}
