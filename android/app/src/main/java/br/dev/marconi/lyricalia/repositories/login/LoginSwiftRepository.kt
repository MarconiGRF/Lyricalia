package br.dev.marconi.lyricalia.repositories.login

import android.util.Log
import br.dev.marconi.lyricalia.repositories.login.models.User
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json

class LoginSwiftRepository(
    private var serverIp: String
) : LoginRepository {
    private val client = HttpClient(CIO) {
        expectSuccess = true
        install(ContentNegotiation) {
            json()
        }
    }
    private val baseUrl = "http://$serverIp:8080"

    override suspend fun createUser(name: String, username: String) {
        var response: HttpResponse
        try {
            response = client.post("$baseUrl/users") {
                contentType(ContentType.Application.Json)
                setBody(User(username, name))
            }
        } catch (e: Exception) {
            throw e
        }

        val body: User = response.body()
        Log.d("IF722_P3_LYRICALIA", body.toString())
    }
}