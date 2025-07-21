package br.dev.marconi.lyricalia.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import br.dev.marconi.lyricalia.activities.MainActivity
import br.dev.marconi.lyricalia.activities.MatchPlayers
import br.dev.marconi.lyricalia.activities.MenuActivity
import br.dev.marconi.lyricalia.activities.SpotifyLinkActivity
import br.dev.marconi.lyricalia.activities.match.MatchCreateActivity
import br.dev.marconi.lyricalia.activities.match.MatchJoinActivity
import br.dev.marconi.lyricalia.activities.match.MatchOngoingActivity
import br.dev.marconi.lyricalia.activities.match.MatchPodiumActivity
import br.dev.marconi.lyricalia.activities.match.MatchWaitingActivity
import kotlin.jvm.java

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

        fun navigateToMatchJoin(packageContext: Context) {
            navigateToActivity(packageContext, MatchJoinActivity::class.java, canGoBack = true)
        }

        fun navigateToMatchWaiting(packageContext: Context, matchId: String, isHost: Boolean) {
            navigateToActivity(
                packageContext,
                MatchWaitingActivity::class.java,
                canGoBack = false,
                matchId,
                isHost)
        }

        fun navigateToMatchOngoing(packageContext: Context, matchId: String, isHost: Boolean, matchPlayers: MatchPlayers) =
            navigateToActivity(
                packageContext,
                MatchOngoingActivity::class.java,
                canGoBack = false,
                matchId,
                isHost,
                matchPlayers
            )

        fun navigateToMatchPodium(packageContext: Context, jsonifiedPodium: String, matchPlayers: MatchPlayers) =
            navigateToActivity(
                packageContext = packageContext,
                cls = MatchPodiumActivity::class.java,
                canGoBack = false,
                matchPlayersExtra = matchPlayers,
                jsonifiedPodiumExtra =  jsonifiedPodium
            )

        private fun navigateToActivity(
            packageContext: Context,
            cls: Class<out Activity>,
            canGoBack: Boolean,
            matchIdExtra: String? = null,
            isHostExtra: Boolean? = false,
            matchPlayersExtra: MatchPlayers? = null,
            jsonifiedPodiumExtra: String? = null
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
            if (matchPlayersExtra != null) {
                intent.putExtra(MATCH_PLAYERS_PARAMETER_ID, matchPlayersExtra)
            }
            if (jsonifiedPodiumExtra != null) {
                intent.putExtra(JSONIFIED_PODIUM_PARAMETER_ID, jsonifiedPodiumExtra)
            }

            packageContext.startActivity(intent)
        }

        const val IS_HOST_PARAMETER_ID = "isHost"
        const val MATCH_ID_PARAMETER_ID = "matchId"
        const val MATCH_PLAYERS_PARAMETER_ID = "matchPlayers"
        const val JSONIFIED_PODIUM_PARAMETER_ID = "jsonifiedPodium"
    }
}