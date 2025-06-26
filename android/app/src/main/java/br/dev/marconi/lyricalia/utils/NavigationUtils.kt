package br.dev.marconi.lyricalia.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import br.dev.marconi.lyricalia.activities.MainActivity
import br.dev.marconi.lyricalia.activities.MenuActivity
import br.dev.marconi.lyricalia.activities.SpotifyLinkActivity
import br.dev.marconi.lyricalia.activities.match.MatchCreateActivity
import br.dev.marconi.lyricalia.activities.match.MatchWaitingActivity

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

        fun navigateToMatchCreate(packageContext: Context) {
            navigateToActivity(packageContext, MatchCreateActivity::class.java, canGoBack = true)
        }

        fun navigateToMatchWaiting(packageContext: Context, matchId: String, isHost: Boolean) {
            navigateToActivity(
                packageContext,
                MatchWaitingActivity::class.java,
                canGoBack = true,
                matchId,
                isHost)
        }

        private fun navigateToActivity(
            packageContext: Context,
            cls: Class<out Activity>,
            canGoBack: Boolean,
            matchIdExtra: String? = null,
            isHostExtra: Boolean? = false
        ) {
            val intent = Intent(packageContext, cls)
            if (!canGoBack) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            if (matchIdExtra != null) {
                intent.putExtra(MATCH_ID_PARAMETER_ID, matchIdExtra)
            }
            if (isHostExtra != null) {
                intent.putExtra(IS_HOST_PARAMETER_ID, isHostExtra)
            }

            packageContext.startActivity(intent)
        }

        const val IS_HOST_PARAMETER_ID = "isHost"
        const val MATCH_ID_PARAMETER_ID = "matchId"
    }
}