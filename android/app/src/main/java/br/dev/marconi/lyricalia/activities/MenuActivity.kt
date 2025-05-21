package br.dev.marconi.lyricalia.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import br.dev.marconi.lyricalia.databinding.ActivityMenuBinding
import br.dev.marconi.lyricalia.repositories.login.models.User
import br.dev.marconi.lyricalia.utils.StorageUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        val user = intent.getParcelableExtra("user", User::class.java)

        setupMenuActivity(user!!)
    }

    @SuppressLint("SetTextI18n")
    private fun setupMenuActivity(user: User) {
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.menu) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupLogoutButton()
        user.spotifyUserId?.run {
            setupGreeting(user)
        } ?: setupSpotifyPrompt(user)

    }

    fun setupSpotifyPrompt(user: User) {
        val firstName = user.name.split(" ").first()
        val hint = "$firstName, conecte com sua biblioteca do Spotify para continuar"
        binding.spotifyHint.text = SpannableString(hint).also {
            it.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                firstName.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        binding.spotifyLink.setOnClickListener {
            linkToSpotify(user)
        }
    }

    fun linkToSpotify(user: User) {
        lifecycleScope.launch {
            binding.isLoadingSpotify = true
            delay(3000)
            binding.isLoadingSpotify = false
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
        StorageUtils(applicationContext).deleteUser()
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }
}