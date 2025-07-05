package br.dev.marconi.lyricalia.viewModels.match

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.dev.marconi.lyricalia.repositories.match.CreateMatchRequest
import br.dev.marconi.lyricalia.repositories.match.MatchService
import br.dev.marconi.lyricalia.utils.StorageUtils
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class MatchCreateViewModel(
    private val filesDir: File
): ViewModel() {
    private val baseUrl: String

    init {
        val serverIp = StorageUtils(filesDir).retrieveServerIp()
        this.baseUrl = "$serverIp"
    }

    suspend fun createMatch(songLimit: Int): String {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(
                GsonBuilder().setStrictness(Strictness.LENIENT).create()
            ))
            .build()

        val service = retrofit.create<MatchService>(MatchService::class.java)

        try {
            val response = service.createMatch(CreateMatchRequest(songLimit))
            if (response.isSuccessful) {
                return response.body()!!
            } else {
                Log.e("IF1001_P3_LYRICALIA", "Could not create match due to -> ${response.errorBody()}")
                throw Exception("Status ${response.code()}")
            }
        } catch (ex: Exception) {
            throw Exception("Could not create match due to ${ex.message}")
        }
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
