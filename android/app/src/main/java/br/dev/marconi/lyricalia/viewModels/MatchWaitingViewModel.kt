package br.dev.marconi.lyricalia.viewModels

import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.dev.marconi.lyricalia.enums.HostCommands
import br.dev.marconi.lyricalia.enums.PlayerMessages
import br.dev.marconi.lyricalia.repositories.match.MatchWebSocket
import br.dev.marconi.lyricalia.repositories.user.User
import br.dev.marconi.lyricalia.utils.StorageUtils
import java.io.File

class MatchWaitingViewModel(
    private val filesDir: File,
    private val lifecycleScope: LifecycleCoroutineScope
): ViewModel() {
    private var currentColor = 0
    fun getCurrentColor() = currentColor
    fun incrementCurrentColor(availableColors: Int) = {
        currentColor = (currentColor + 1) % availableColors
    }

    val hostOnline: MutableLiveData<Boolean?> = MutableLiveData<Boolean?>(null)
    val actionable: MutableLiveData<List<String>?> = MutableLiveData<List<String>?>(null)

    private val baseUrl: String
    private val currentUser: User
    private val ws = MatchWebSocket()

    init {
        val serverIp = StorageUtils(filesDir).retrieveServerIp()
        this.baseUrl = "$serverIp"

        currentUser = StorageUtils(filesDir).retrieveUser()!!
    }

    fun startMatch() {
        ws.send(HostCommands.START)
    }

    fun endMatch() {
        ws.send(HostCommands.SENDABLE_END)
    }

    fun leaveMatch() {
        ws.send(PlayerMessages.LEAVE(currentUser.id!!))
    }

    fun connectAsHost(matchId: String) {
        try {
            ws.connect(
                StorageUtils(filesDir).retrieveServerIp(),
                matchId,
                lifecycleScope,
                {
                    Log.d("IF1001_P3_LYRICALIA", "Match message received: $it")
                    actionable.value = it.split("$")
                },
                { hostOnline.value = it }
            )

            ws.send(HostCommands.SET(currentUser.id!!))
        } catch (ex: Exception) {
            throw Exception("Failed to join match as host: ${ex.message}")
        }
    }

    fun connectAsPlayer(matchId: String) {
        try {
            ws.connect(
                StorageUtils(filesDir).retrieveServerIp(),
                matchId,
                lifecycleScope,
                {
                    Log.d("IF1001_P3_LYRICALIA", "Match message received: $it")
                    actionable.value = it.split("$")
                },
                { hostOnline.value = it }
            )

            ws.send(PlayerMessages.JOIN(currentUser.id!!))
        } catch (ex: Exception) {
            throw Exception("Failed to join match as player: ${ex.message}")
        }
    }
}

@Suppress("UNCHECKED_CAST")
class MatchWaitingViewModelFactory(
    private val filesDir: File,
    private val lifecycleScope: LifecycleCoroutineScope
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MatchWaitingViewModel::class.java)) {
            return MatchWaitingViewModel(filesDir, lifecycleScope) as T
        }
        throw IllegalArgumentException("Unknown requested class for MatchWaitingViewModel")
    }
}
