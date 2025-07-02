package br.dev.marconi.lyricalia.repositories.login

import br.dev.marconi.lyricalia.repositories.user.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface LoginService {
    @POST("users")
    suspend fun createUser(@Body user: User): Response<User>
}
