package br.dev.marconi.lyricalia.utils

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import br.dev.marconi.lyricalia.repositories.spotifyCredentials.SpotifyCredentialsDatabase
import br.dev.marconi.lyricalia.repositories.spotifyCredentials.SpotifyCredentialsEntity
import br.dev.marconi.lyricalia.repositories.user.User
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json

class SpotifyUtils {
    companion object {
        const val REQUEST_CODE = 18
        const val REDIRECT_URI = "lyricalia://link"
        const val CLIENT_ID = "20a9c0b160ed45d88f8a3e0103924703"

        private val client = HttpClient(CIO) {
            expectSuccess = true
            install(ContentNegotiation) { json() }
        }

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

        fun buildCredentials(authorizationCode: String) = SpotifyCredentialsEntity(
            authorizationCode = authorizationCode,
            accessToken = null,
        )

        suspend fun exchangeAndSaveTokens(context: Context, authorizationCode: String): SpotifyCredentialsEntity {
            try {
                val serverIp = StorageUtils(context).retrieveServerIp()

                var response = client.post("http://$serverIp:8080/spotify/auth") {
                    contentType(ContentType.Application.Json)
                    setBody(buildCredentials(authorizationCode))
                }
                val body = response.body<SpotifyCredentialsEntity>()
                saveCredentials(context, body)

                return body
            } catch (ex: Exception) {
                Log.e("IF1001_P3_LYRICALIA", "Failed to exchange tokens with spotify", ex)
                throw ex
            }
        }

        fun saveCredentials(context: Context, credentials: SpotifyCredentialsEntity) {
            val db = SpotifyCredentialsDatabase.getInstance(context)
            db.spotifyCredentialsDao().insert(credentials)
        }
    }
}