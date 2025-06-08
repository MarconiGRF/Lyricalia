package br.dev.marconi.lyricalia.repositories.login

import android.content.Context
import br.dev.marconi.lyricalia.repositories.user.User
import br.dev.marconi.lyricalia.utils.StorageUtils
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

class LoginSwiftRepository : LoginRepository {
    private val client = HttpClient(CIO) {
        expectSuccess = true
        install(ContentNegotiation) {
            json()
        }
    }
    private var baseUrl: String
    private var serverIp: String

    constructor(context: Context) {
        this.serverIp = StorageUtils(context).retrieveServerIp()
        this.baseUrl = "http://$serverIp:8080"
    }

    override suspend fun createUser(name: String, username: String): User {
        var response: HttpResponse
        try {
            response = client.post("$baseUrl/users") {
                contentType(ContentType.Application.Json)
                setBody(User(username, name))
            }
        } catch (e: Exception) {
            throw e
        }

        return response.body<User>()
    }
}