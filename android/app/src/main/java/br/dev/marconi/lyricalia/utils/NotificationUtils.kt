package br.dev.marconi.lyricalia.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat

class NotificationUtils {
    companion object {
        fun createLinkingNotificationChannel(notificationManager: NotificationManager) {
            val name = "Linking Notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel("linking", name, importance).also {
                it.description = "To tell when spotify linking process is done"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}