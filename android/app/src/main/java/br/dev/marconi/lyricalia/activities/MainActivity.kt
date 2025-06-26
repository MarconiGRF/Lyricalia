package br.dev.marconi.lyricalia.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import br.dev.marconi.lyricalia.databinding.ActivityMainBinding
import br.dev.marconi.lyricalia.repositories.login.LoginSwiftRepository
import br.dev.marconi.lyricalia.repositories.user.User
import br.dev.marconi.lyricalia.utils.NavigationUtils
import br.dev.marconi.lyricalia.utils.StorageUtils
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        navigateAccordingToUserState()
    }

    private fun navigateAccordingToUserState() {
        val user = StorageUtils(applicationContext.filesDir).retrieveUser()
        if (user != null) {
            when {
                user.spotifyToken != null -> NavigationUtils.navigateToMenu(this)
                else                      -> NavigationUtils.navigateToSpotifyLink(this)
            }
        } else {
            setupMainActivity()
        }
        binding.isLoading = false
    }

    private fun setupMainActivity() {
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.submitUserButton.setOnClickListener {
            doLogin(binding.nameText.text.toString(), binding.usernameText.text.toString())
        }

        setupServerIp()
    }

    @SuppressLint("SetTextI18n")
    private fun setupServerIp() {
        binding.serverIp.setText("http://10.0.2.2:8080")
    }

    private fun showLongToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    private fun doLogin(name: String, username: String) {
        lifecycleScope.launch {
            binding.isLoading = true

            val user: User
            val storageUtils = StorageUtils(applicationContext.filesDir)
            try {
                storageUtils.saveServerIp(binding.serverIp.text.toString())

                user = LoginSwiftRepository(applicationContext).createUser(name, username)
                storageUtils.saveUser(user)

                navigateAccordingToUserState()
            } catch (ex: Exception) {
                showLongToast("Erro ao entrar: " + ex.message.toString())
                binding.isLoading = false
            }
        }
    }
}