package com.beauty.platform.fcm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.beauty.platform.MainActivity

class NotificationReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "NotificationReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "알림 클릭 수신: ${intent.action}")
        
        when (intent.action) {
            "com.beauty.platform.NOTIFICATION_CLICKED" -> {
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    
                    // 인텐트에서 추가 데이터 전달
                    intent.extras?.let { extras ->
                        putExtras(extras)
                    }
                }
                
                context.startActivity(mainIntent)
            }
        }
    }
}