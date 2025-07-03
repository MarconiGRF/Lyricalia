package br.dev.marconi.lyricalia.repositories.match

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MatchService {
    @POST("match")
    suspend fun createMatch(@Body request: CreateMatchRequest): Response<String>

    @GET("match/exists/{matchId}")
    suspend fun joinMatch(@Path("matchId") matchId: String) : Response<Unit>
}