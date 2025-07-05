package br.dev.marconi.lyricalia.viewModels.match

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

class MatchOngoingViewModel(
    private val filesDir: File,
    private val lifecycleScope: LifecycleCoroutineScope
): ViewModel() {
    val hostOnline: MutableLiveData<Boolean?> = MutableLiveData<Boolean?>(null)
    val actionable: MutableLiveData<List<String>?> = MutableLiveData<List<String>?>(null)

    var matchId: String = ""
    var isHost: Boolean = false

    private val baseUrl: String
    private val currentUser: User
    private val ws = MatchWebSocket()

    init {
        val serverIp = StorageUtils(filesDir).retrieveServerIp()
        this.baseUrl = "$serverIp"

        currentUser = StorageUtils(filesDir).retrieveUser()!!
    }

    fun connectAsPlayer() {
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

            ws.send(PlayerMessages.READY(currentUser.id!!))
        } catch (ex: Exception) {
            throw Exception("Failed to join match as player: ${ex.message}")
        }
    }

    fun endMatch() {
        ws.send(HostCommands.SENDABLE_END)
    }

    fun leaveMatch() {
        ws.send(PlayerMessages.LEAVE(currentUser.id!!))
    }
}


@Suppress("UNCHECKED_CAST")
class MatchOngoingViewModelFactory(
    private val filesDir: File,
    private val lifecycleScope: LifecycleCoroutineScope
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MatchOngoingViewModel::class.java)) {
            return MatchOngoingViewModel(filesDir, lifecycleScope) as T
        }
        throw IllegalArgumentException("Unknown requested class for MatchOngoingViewModel")
    }
}
