package com.hyundaiapp.chachelin

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.hyundaiapp.chachelin.domain.ChargeResult
import com.hyundaiapp.chachelin.domain.ChargeSessionRecord

/**
 * 결과 알림 표시([com.hyundaiapp.chachelin.domain.ChargeResultNotices]가 언제 띄울지 정한다).
 * 알림 권한(S7에서 요청)이 없으면 띄우지 않는다. 기록은 그대로 남고 앱을 열면 S4/S5가 보인다.
 */
class ResultNotification(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(NotificationManager::class.java)

    fun show(record: ChargeSessionRecord, stationName: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        manager.createNotificationChannel(
            // 정차 중에 바로 보이도록 화면 위에 잠깐 띄운다(헤드업)
            NotificationChannel(CHANNEL_ID, "충전 결과", NotificationManager.IMPORTANCE_HIGH),
        )
        val open = PendingIntent.getActivity(
            appContext, 0,
            Intent(appContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val place = stationName ?: "충전소"
        val (title, text) = if (record.result == ChargeResult.SUCCESS) {
            "충전 완료 · 3초 평가" to "$place · ${record.socStart}% → ${record.socEnd}% · 눌러서 평가해 주세요"
        } else {
            "충전 실패를 기록했어요" to "$place · 실패가 아니면 눌러서 취소할 수 있어요"
        }
        manager.notify(
            NOTIFICATION_ID,
            Notification.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }

    fun hide() = manager.cancel(NOTIFICATION_ID)

    private companion object {
        const val CHANNEL_ID = "charge_result"

        /** 기록 서비스 알림(1)과 겹치지 않게 */
        const val NOTIFICATION_ID = 2
    }
}
