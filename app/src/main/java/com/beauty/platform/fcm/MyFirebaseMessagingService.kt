package com.beauty.platform.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.beauty.platform.MainActivity
import com.beauty.platform.R
import com.beauty.platform.api.FcmApiClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "mimotok_default"
        private const val PREF_FCM = "fcm_token"
        private const val KEY_TOKEN = "token"
    }
    
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    /**
     * 토큰 갱신 시 호출 (개선)
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "🔄 FCM 토큰이 갱신되었습니다")
        Log.d(TAG, "  Token: ${token.take(20)}...")
        
        // 1. 로컬에 저장
        saveTokenLocally(token)
        
        // 2. 서버에 전송
        sendTokenToServer(token)
    }

    /**
     * 푸시 메시지 수신 시 호출 (기존 유지 + 개선)
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "📨 푸시 메시지 수신")
        Log.d(TAG, "  From: ${remoteMessage.from}")
        
        // Data 페이로드 처리
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "  Data: ${remoteMessage.data}")
            
            val pushType = remoteMessage.data["pushType"]
            val deepLink = remoteMessage.data["deepLink"]
            
            Log.d(TAG, "  PushType: $pushType")
            Log.d(TAG, "  DeepLink: $deepLink")
        }
        
        // Notification 페이로드 처리
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "  Title: ${notification.title}")
            Log.d(TAG, "  Body: ${notification.body}")
            
            sendNotification(
                title = notification.title ?: "Mimotok",
                messageBody = notification.body ?: "",
                data = remoteMessage.data
            )
        }
    }

    /**
     * 알림 표시 (기존 + 개선)
     */
    private fun sendNotification(
        title: String,
        messageBody: String,
        data: Map<String, String>
    ) {
        // Intent 생성 (딥링크 포함)
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            
            // Data 페이로드를 Intent에 추가
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
            
            // DeepLink가 있으면 추가
            data["deepLink"]?.let { deepLink ->
                putExtra("deepLink", deepLink)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(), // Unique request code
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Notification 생성
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // 앱 아이콘
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            System.currentTimeMillis().toInt(), // Unique notification ID
            notificationBuilder.build()
        )
        
        Log.d(TAG, "✅ 알림 표시 완료")
    }

    /**
     * 로컬에 토큰 저장
     */
    private fun saveTokenLocally(token: String) {
        val sharedPref = getSharedPreferences(PREF_FCM, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(KEY_TOKEN, token)
            putLong("token_timestamp", System.currentTimeMillis())
            apply()
        }
        Log.d(TAG, "💾 토큰 로컬 저장 완료")
    }

    /**
     * 서버에 토큰 전송
     */
    private fun sendTokenToServer(token: String) {
        scope.launch {
            try {
                val deviceId = Settings.Secure.getString(
                    contentResolver,
                    Settings.Secure.ANDROID_ID
                )
                val language = Locale.getDefault().language
                
                Log.d(TAG, "📤 서버에 토큰 전송 중...")
                val result = FcmApiClient.registerToken(
                    fcmToken = token,
                    deviceId = deviceId,
                    platform = "android",
                    preferredLanguage = language
                )
                
                if (result.isSuccess) {
                    Log.d(TAG, "✅ 서버 전송 완료")
                } else {
                    Log.e(TAG, "❌ 서버 전송 실패")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 서버 전송 오류", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Mimotok Notifications"
            val channelDescription = "Mimotok push notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
                description = channelDescription
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}