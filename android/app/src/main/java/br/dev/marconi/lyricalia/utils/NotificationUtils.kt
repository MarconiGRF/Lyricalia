package br.dev.marconi.lyricalia.utils

import android.app.NotificationChannel
import android.app.NotificationManager

class NotificationUtils {
    companion object {
        fun createSpotifyChannel(notificationManager: NotificationManager) {
            val name = "Spotify Linking Notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel("linking", name, importance).also {
                it.description = "Pra te avisar sobre o processo de conectar com o Spotify"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}