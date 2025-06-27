package br.dev.marconi.lyricalia.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.dev.marconi.lyricalia.utils.StorageUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.kotlinx.json.json
import java.io.File

class MatchJoinViewModel(
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
        this.baseUrl = "$serverIp"
    }

    suspend fun joinMatch(matchId: String): Boolean {
        var response: HttpResponse
        try {
            response = httpClient.get("$baseUrl/match/$matchId")
            if (response.status.value != 200) {
                throw Exception("Status code ${response.status.value} from server - ${response.body<String>()}")
            }
        } catch (ex: Exception) {
            Log.e("IF1001_P3_LYRICALIA", "Could not create match due to -> ${ex.message}")
            throw ex
        }

        return true
    }
}

@Suppress("UNCHECKED_CAST")
class MatchJoinViewModelFactory(
    private val filesDir: File
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MatchJoinViewModel::class.java)) {
            return MatchJoinViewModel(filesDir) as T
        }
        throw IllegalArgumentException("Unknown requested class for MatchJoinViewModel")
    }
}
