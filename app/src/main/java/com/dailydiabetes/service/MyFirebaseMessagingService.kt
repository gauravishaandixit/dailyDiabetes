package com.dailydiabetes.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dailydiabetes.R
import com.dailydiabetes.constant.Constants.Companion.CHANNEL_ID
import com.dailydiabetes.constant.Constants.Companion.CHANNEL_NAME
import com.dailydiabetes.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService: FirebaseMessagingService() {
    private val TAG = "MyFirebaseMessagingServ"
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.i(TAG, "onMessageReceived: $message")
        val intent = Intent(this, MainActivity::class.java)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationID = Random.nextInt()

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, FLAG_ONE_SHOT)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(message.data["title"])
            .setContentText(message.data["message"])
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_baseline_addchart_24)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationID, notification)

    }

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        Log.i(TAG, "onNewToken: ")
    }

    private fun createNotificationChannel(notificationManager: NotificationManager){
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, IMPORTANCE_HIGH).apply {
            description = "My channel description"
            enableLights(true)
            lightColor = Color.GREEN
        }
        notificationManager.createNotificationChannel(channel)
    }
}