package com.example.swingcoach

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.swingcoach.R

fun displayNotification(context: Context, title: String, text: String, id: Int) {
    // Go to an activity when tapping the notification (not needed for now)
//            val notificationIntent = Intent(this, SomeActivity::class.java).apply {
//                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            }
//            val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.notification_icon)
        .setContentTitle(title)
        .setContentText(text)
//                .setContentIntent(notificationIntent)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true) // remove notification after user tap

    with(NotificationManagerCompat.from(context)) {
        notify(id, builder.build())
    }
}