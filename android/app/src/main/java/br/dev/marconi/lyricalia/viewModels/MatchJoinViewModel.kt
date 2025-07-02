package br.dev.marconi.lyricalia.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.dev.marconi.lyricalia.repositories.match.CreateMatchRequest
import br.dev.marconi.lyricalia.repositories.match.MatchService
import br.dev.marconi.lyricalia.utils.StorageUtils
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.File

class MatchJoinViewModel(
    private val filesDir: File
): ViewModel() {
    private val baseUrl: String

    init {
        val serverIp = StorageUtils(filesDir).retrieveServerIp()
        this.baseUrl = "$serverIp"
    }

    suspend fun joinMatch(matchId: String): Boolean {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()

        val service = retrofit.create<MatchService>(MatchService::class.java)

        val response = service.joinMatch(matchId)
        if (!response.isSuccessful) {
            Log.e("IF1001_P3_LYRICALIA", "Could not join match due to -> ${response.errorBody()}")
            throw Error("Status ${response.code()}")
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
