package br.dev.marconi.lyricalia.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.dev.marconi.lyricalia.repositories.user.User
import br.dev.marconi.lyricalia.utils.StorageUtils
import java.io.File

class SpotifyLinkViewModel(
    private val filesDir: File
): ViewModel() {
    var currentUser = MutableLiveData<User>()

    init {
        currentUser.value = StorageUtils(filesDir).retrieveUser()
    }

    fun updateUser(newUser: User) {
        currentUser.value = newUser
        StorageUtils(filesDir).saveUser(newUser)
    }
}

@Suppress("UNCHECKED_CAST")
class SpotifyLinkViewModelFactory(
    private val filesDir: File
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SpotifyLinkViewModel::class.java)) {
            return SpotifyLinkViewModel(filesDir) as T
        }
        throw IllegalArgumentException("Unknown requested class for SpotifyLinkViewModel")
    }
}