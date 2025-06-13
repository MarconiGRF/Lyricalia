package br.dev.marconi.lyricalia.activities

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import br.dev.marconi.lyricalia.databinding.ActivityMenuBinding
import br.dev.marconi.lyricalia.repositories.lyric.LyricDatabase
import br.dev.marconi.lyricalia.repositories.spotifyCredentials.SpotifyCredentialsDatabase
import br.dev.marconi.lyricalia.repositories.user.User
import br.dev.marconi.lyricalia.utils.NavigationUtils
import br.dev.marconi.lyricalia.utils.SpotifyUtils
import br.dev.marconi.lyricalia.utils.StorageUtils
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.plugins.websocket.wss
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.readReason
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.Int

class MenuActivity : AppCompatActivity() {
    lateinit var lyricDb: LyricDatabase
    private lateinit var binding: ActivityMenuBinding
    private lateinit var storage: StorageUtils
    private val client = HttpClient {
        install(WebSockets)
    }

    private var greetingPhrases = arrayOf(
        Pair("Qual a letra de hoje, <USER>?", "¯\\_(ツ)_/¯"),
        Pair("Oi, <USER>!", "¯\\_(ツ)_/¯"),
        Pair("Vamos levantar poeira, <USER> 🎶", "Ivete Sangalo - Poeira"),
        Pair("Deixa a letra te levar, letra leva <USER> 🎶", "Zeca Pagodinho - Deixa a vida me levar"),
        Pair("Não deixe o samba morrer, não deixa a letra acabar <USER>! 🎶", "Alcione - Não deixe o samba morrer"),
        Pair("<USER>, se tu soubesse o poder que a loira tem! 🎶", "Musa do Calypso - A Loira e a Morena"),
        Pair("<USER>, se tu soubesse o poder que a morena tem! 🎶", "Musa do Calypso - A Loira e a Morena"),
        Pair("Sweet dreams are made of lyrics <USER>! 🎶", "Eurythmics - Sweet Dreams"),
        Pair("Alô alô <USER>, aqui quem fala é da terra! 🎶", "Elis Regina - Alô Alô Marciano"),
        Pair("Eu já deitei no teu sorriso <USER>! 🎶", "Marina Sena - Por Supuesto"),
        Pair("Às vezes no silêncio da noite, eu fico imaginando nós dois <USER>... 🎶", "Caetano Veloso - Sozinho"),
        Pair("Estranho é eu gostar tanto do seu all-star azul <USER>! 🎶", "Nando Reis - All Star"),
    )

    private var currentGreeting = greetingPhrases.random()

    override fun onStart() {
        super.onStart()

        storage = StorageUtils(applicationContext.filesDir)
        binding.isProcessingLibrary = true

        val user = storage.retrieveUser()
        if (user != null) {
            user.spotifyToken?.run {
                setupGreeting(user)
                setupLogoutButton()
                followLibraryProcessing(user)
            } ?: NavigationUtils.navigateToSpotifyLink(this)
        }
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMenuBinding.inflate(layoutInflater)
        setupMenuActivity()
    }

    private fun followLibraryProcessing(user: User) {

        val serverIp = storage.retrieveServerIp()
        lifecycleScope.launch {
//            SpotifyUtils.dispatchProcessUserLibrary(applicationContext)

            delay(1000)

            client.webSocket(HttpMethod.Get, serverIp, 8080, "/spotify/library") {
                try {
                    send(Frame.Text(user.id!!))

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
//                            binding.libraryProcessingProgress.text = String(frame.data)
                            Log.d("IF1001_P3_LYRICALIA", "Websocket text received: $frame")
                        }
                        else if (frame is Frame.Close) {
                            Log.d("IF1001_P3_LYRICALIA", "Websocket closed: ${frame.readReason()}")
                            Toast.makeText(applicationContext, "Websocket closed: ${frame.readReason()}", Toast.LENGTH_LONG).show()
                        }
                    }

                    Log.d("IF1001_P3_LYRICALIA", "Websocket possibly closed!")
                } catch (ex: Exception) {
                    Log.e("IF1001_P3_LYRICALIA", "Websocket exception: $ex")
                }

            }
        }
    }

    suspend fun counter(): Int {
        delay(1000)
        binding.tv1.text = "thread1"

        return 1
    }
    suspend fun counter2(): Int {
        delay(1000)
        binding.tv2.text = "thread2"

        return 2
    }
    suspend fun counter4(): Int {
        delay(1000)

        return 4
    }

    @SuppressLint("SetTextI18n")
    private fun setupMenuActivity() {
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.menu) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun setupGreeting(user: User) {
        val firstName = user.name.split(" ").first()

        val plainGreeting = currentGreeting.first.replace("<USER>", firstName)

        val firstNameIndex = plainGreeting.indexOf(firstName)
        binding.greeting.text = SpannableString(plainGreeting).also {
            it.setSpan(
                StyleSpan(Typeface.ITALIC),
                0,
                plainGreeting.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            it.setSpan(
                StyleSpan(Typeface.BOLD),
                firstNameIndex,
                firstNameIndex + firstName.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.greeting.setOnClickListener {
            Toast.makeText(this, currentGreeting.second, Toast.LENGTH_LONG).show()
        }
    }

    fun setupLogoutButton() {
        binding.logoutButton.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        StorageUtils(applicationContext.filesDir).deleteUser()
        NavigationUtils.navigateToLogin(this)
    }
}