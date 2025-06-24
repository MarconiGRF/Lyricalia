package br.dev.marconi.lyricalia.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.dev.marconi.lyricalia.repositories.lyric.LyricDatabase
import br.dev.marconi.lyricalia.repositories.user.User
import br.dev.marconi.lyricalia.utils.StorageUtils
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import java.io.File

class MenuViewModel(
    private val filesDir: File
): ViewModel() {
    var currentUser = MutableLiveData<User>()
    var serverIp: String

    var greetingPhrases = arrayOf(
        Pair("Qual a letra de hoje, <USER>?", "¯\\_(ツ)_/¯"),
        Pair("Oi, <USER>!", "¯\\_(ツ)_/¯"),
        Pair("Vamo levantar poeira, <USER> 🎶", "Ivete Sangalo - Poeira"),
        Pair("Deixa a letra te levar, letra leva <USER> 🎶", "Zeca Pagodinho - Deixa a vida me levar"),
        Pair("Não deixe o samba morrer, não deixa a letra acabar <USER>! 🎶", "Alcione - Não deixe o samba morrer"),
        Pair("<USER>, se tu soubesse o poder que a loira tem! 🎶", "Musa do Calypso - A Loira e a Morena"),
        Pair("<USER>, se tu soubesse o poder que a morena tem! 🎶", "Musa do Calypso - A Loira e a Morena"),
        Pair("Sweet dreams are made of lyrics <USER>! 🎶", "Eurythmics - Sweet Dreams"),
        Pair("Alô alô <USER>, aqui quem fala é da terra! 🎶", "Elis Regina - Alô Alô Marciano"),
        Pair("Eu já deitei no teu sorriso <USER>! 🎶", "Marina Sena - Por Supuesto"),
        Pair("Às vezes no silêncio da noite, eu fico imaginando nós dois <USER>... 🎶", "Caetano Veloso - Sozinho"),
        Pair("Estranho é eu gostar tanto do seu all-star azul <USER>! 🎶", "Nando Reis - All Star"),
        Pair("Adivinha, Adivinha, Adivinha, <USER>! 🎶", "Charli XCX & Billie Ellish - Guess"),
        Pair("I'm your biggest fan, I'll follow you until you love me <USER>! 🎶", "Lady Gaga - Paparazzi"),
    )
    var currentGreeting = greetingPhrases.random()

    val httpClient = HttpClient { install(WebSockets) }
    lateinit var lyricDb: LyricDatabase

    init {
        currentUser.value = StorageUtils(filesDir).retrieveUser()
        serverIp = StorageUtils(filesDir).retrieveServerIp()
    }
}

@Suppress("UNCHECKED_CAST")
class MenuViewModelFactory(
    private val filesDir: File
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MenuViewModel::class.java)) {
            return MenuViewModel(filesDir) as T
        }
        throw IllegalArgumentException("Unknown requested class for MenuViewModel")
    }
}