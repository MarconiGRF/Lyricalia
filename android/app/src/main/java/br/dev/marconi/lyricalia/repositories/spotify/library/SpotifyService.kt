package br.dev.marconi.lyricalia.repositories.spotify.library

import br.dev.marconi.lyricalia.repositories.spotify.credentials.SpotifyCredentialsEntity
import br.dev.marconi.lyricalia.repositories.user.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SpotifyService {
    @POST("spotify/auth")
    suspend fun exchangeTokens(@Body credentials: SpotifyCredentialsEntity): Response<SpotifyCredentialsEntity>

    @POST("spotify/library")
    suspend fun dispatchLibraryProcessor(@Body user: User): Response<User>
}