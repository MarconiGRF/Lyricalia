package br.dev.marconi.lyricalia.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.dev.marconi.lyricalia.repositories.match.CreateMatchRequest
import br.dev.marconi.lyricalia.utils.StorageUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import java.io.File

class MatchCreateViewModel(
    private val filesDir: File
): ViewModel() {
    private val httpClient = HttpClient {
        install(WebSockets)
        install(ContentNegotiation) {
            json()
        }
    }
    private val baseUrl: String

    init {
        val serverIp = StorageUtils(filesDir).retrieveServerIp()
        this.baseUrl = "http://$serverIp:8080"
    }

    suspend fun createMatch(songLimit: Int): String {
        var response: HttpResponse
        try {
            response = httpClient.post("$baseUrl/match") {
                contentType(ContentType.Application.Json)
                val createMatchRequestBody = CreateMatchRequest(songLimit)
                setBody(createMatchRequestBody)
            }
            if (response.status.value != 200) {
                throw Exception("Status code ${response.status.value} from server - ${response.body<String>()}")
            }
        } catch (ex: Exception) {
            Log.e("IF1001_P3_LYRICALIA", "Could not create match due to -> ${ex.message}")
            throw ex
        }

        return response.body<String>()
    }
}

@Suppress("UNCHECKED_CAST")
class MatchCreateViewModelFactory(
    private val filesDir: File
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MatchCreateViewModel::class.java)) {
            return MatchCreateViewModel(filesDir) as T
        }
        throw IllegalArgumentException("Unknown requested class for MatchCreateViewModel")
    }
}
