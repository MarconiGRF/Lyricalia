package br.dev.marconi.lyricalia.utils

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import br.dev.marconi.lyricalia.repositories.spotify.credentials.SpotifyCredentialsDatabase
import br.dev.marconi.lyricalia.repositories.spotify.credentials.SpotifyCredentialsEntity
import br.dev.marconi.lyricalia.repositories.spotify.SpotifyService
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SpotifyUtils {
    companion object {
        const val REQUEST_CODE = 18
        const val REDIRECT_URI = "lyricalia://link"
        const val CLIENT_ID = "20a9c0b160ed45d88f8a3e0103924703"

        fun authenticateUser(activity: AppCompatActivity) {
            val authorizationRequest = AuthorizationRequest.Builder(
                CLIENT_ID,
                AuthorizationResponse.Type.CODE,
                REDIRECT_URI
            ).also { it.setScopes(arrayOf("user-library-read")) }

            AuthorizationClient.openLoginInBrowser(activity, authorizationRequest.build())
        }

        fun buildCredentials(authorizationCode: String) = SpotifyCredentialsEntity(
            authorizationCode = authorizationCode,
            accessToken = null,
        )

        suspend fun exchangeAndSaveTokens(context: Context, authorizationCode: String): SpotifyCredentialsEntity {
            val serverIp = StorageUtils(context.filesDir).retrieveServerIp()
            val retrofit = Retrofit.Builder()
                .baseUrl(serverIp)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val service = retrofit.create<SpotifyService>(SpotifyService::class.java)

            val response = service.exchangeTokens(buildCredentials(authorizationCode))
            if (response.isSuccessful) {
                return response.body()!!
            } else {
                Log.e("IF1001_P3_LYRICALIA", "Failed to exchange tokens with spotify")
                throw Error("Status ${response.code()}")
            }
        }

        fun saveCredentials(context: Context, credentials: SpotifyCredentialsEntity) {
            val db = SpotifyCredentialsDatabase.getInstance(context)
            db.spotifyCredentialsDao().insert(credentials)
        }

        suspend fun dispatchProcessUserLibrary(context: Context) {
            val serverIp = StorageUtils(context.filesDir).retrieveServerIp()
            val retrofit = Retrofit.Builder()
                .baseUrl(serverIp)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val service = retrofit.create<SpotifyService>(SpotifyService::class.java)

            val user = StorageUtils(context.filesDir).retrieveUser()
            val response = service.dispatchLibraryProcessor(user!!)

            if (response.isSuccessful) {
                Log.e("IF1001_P3_LYRICALIA", "Dispatch of library processor successful")
            } else {
                Log.e("IF1001_P3_LYRICALIA", "Failed to dispatch library processor to API")
                throw Error("Status ${response.code()}")
            }
        }
    }
}