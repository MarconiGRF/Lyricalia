package br.dev.marconi.lyricalia.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import br.dev.marconi.lyricalia.R

class NotificationUtils {
    companion object {
        object SPOTIFY_LINKING {
            object PROCESS_STARTED {
                const val NOTIFICATION_ID = 1
            }
            const val CHANNEL_ID = "spotify_linking_channel"
            const val NAME = "Spotify Linking Notifications"
            const val DESCRIPTION = "Pra te avisar sobre o processo de conectar com o Spotify"
        }
        private fun createSpotifyChannel(notificationManager: NotificationManager) {
            val channel = NotificationChannel(
                SPOTIFY_LINKING.CHANNEL_ID,
                SPOTIFY_LINKING.NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).also {
                it.description = SPOTIFY_LINKING.DESCRIPTION
                it.enableVibration(true)
                it.vibrationPattern = longArrayOf(200)
            }
            notificationManager.createNotificationChannel(channel)
        }

        fun notifySpotifyLinkStarted(notificationManager: NotificationManager, context: Context) {
            createSpotifyChannel(notificationManager)
            val n = NotificationCompat.Builder(context, SPOTIFY_LINKING.CHANNEL_ID)
                .setContentTitle("Conectando Spotify...")
                .setContentText("estamos te esperando terminar de conectar, avisaremos quando terminar ou se o processo falhar :)")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build()
            notificationManager.notify(SPOTIFY_LINKING.PROCESS_STARTED.NOTIFICATION_ID, n)
        }
    }
}