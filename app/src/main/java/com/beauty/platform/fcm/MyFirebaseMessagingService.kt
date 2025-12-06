package com.beauty.platform.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.beauty.platform.MainActivity
import com.beauty.platform.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        const val CHANNEL_ID = "mimotok_notifications"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM 토큰이 갱신되었습니다: $token")
        
        // 토큰을 서버로 전송하는 로직 구현
        sendTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "메시지 수신: ${remoteMessage.from}")

        // 데이터 페이로드가 있는지 확인
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "메시지 데이터: ${remoteMessage.data}")
        }

        // 알림 페이로드가 있는지 확인하고 알림 표시
        remoteMessage.notification?.let {
            Log.d(TAG, "알림 제목: ${it.title}")
            Log.d(TAG, "알림 내용: ${it.body}")
            sendNotification(it.title ?: "Mimotok", it.body ?: "", remoteMessage.data)
        }
    }

    private fun sendTokenToServer(token: String) {
        // SharedPreferences에 토큰 저장
        val sharedPref = getSharedPreferences("fcm_token", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("token", token)
            putLong("token_timestamp", System.currentTimeMillis())
            apply()
        }
        
        // WebView의 JavaScript로 토큰 전달 (MainActivity에서 처리)
        Log.d(TAG, "토큰이 로컬에 저장되었습니다. WebView에서 토큰을 요청할 수 있습니다.")
    }

    private fun sendNotification(title: String, messageBody: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // 알림 데이터를 인텐트에 추가
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mimotok 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Mimotok 앱 푸시 알림 채널"
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}