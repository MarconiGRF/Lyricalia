package br.dev.marconi.lyricalia.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import br.dev.marconi.lyricalia.activities.MainActivity
import br.dev.marconi.lyricalia.activities.MenuActivity
import br.dev.marconi.lyricalia.activities.SpotifyLinkActivity
import br.dev.marconi.lyricalia.activities.match.MatchCreateActivity

class NavigationUtils {
    companion object {
        fun navigateToLogin(packageContext: Context) {
            navigateToActivity(packageContext, MainActivity::class.java, canGoBack = false)
        }

        fun navigateToSpotifyLink(packageContext: Context) {
            navigateToActivity(packageContext, SpotifyLinkActivity::class.java, canGoBack = true)
        }

        fun navigateToMenu(packageContext: Context) {
            navigateToActivity(packageContext, MenuActivity::class.java, canGoBack = false)
        }

        fun navigateToCreateMatch(packageContext: Context) {
            navigateToActivity(packageContext, MatchCreateActivity::class.java, canGoBack = true)
        }

        private fun navigateToActivity(
            packageContext: Context,
            cls: Class<out Activity>,
            canGoBack: Boolean
        ) {
            val intent = Intent(packageContext, cls)
            if (!canGoBack) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            packageContext.startActivity(intent)
        }
    }
}