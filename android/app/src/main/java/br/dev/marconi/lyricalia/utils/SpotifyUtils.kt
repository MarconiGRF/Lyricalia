package br.dev.marconi.lyricalia.utils

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse

class SpotifyUtils {
    companion object {
        const val REQUEST_CODE = 18
        const val REDIRECT_URI = "lyricalia://link"
        const val CLIENT_ID = "20a9c0b160ed45d88f8a3e0103924703"
        val CLIENT_SECRET = System.getenv("SPOTIFY_CLIENT_SECRET")

        fun authenticateUser(activity: AppCompatActivity) {
            val authorizationRequest = AuthorizationRequest.Builder(
                CLIENT_ID,
                AuthorizationResponse.Type.CODE,
                REDIRECT_URI
            ).also {
                it.setScopes(arrayOf("user-library-read"))
            }

            AuthorizationClient.openLoginActivity(activity, REQUEST_CODE, authorizationRequest.build())
        }

        fun exchangeAndSaveTokens(authorizationCode: String) {
            try {

            } catch (ex: Exception) {
                Log.e("IF1001_P3_LYRICALIA", "Failed to exchange tokens with spotify", ex)
                throw ex
            }
        }
    }
}