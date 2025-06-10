package br.dev.marconi.lyricalia.activities

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.core.Storage
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import br.dev.marconi.lyricalia.databinding.ActivitySpotifyLinkBinding
import br.dev.marconi.lyricalia.repositories.spotifyCredentials.SpotifyCredentialsEntity
import br.dev.marconi.lyricalia.repositories.user.User
import br.dev.marconi.lyricalia.utils.NavigationUtils
import br.dev.marconi.lyricalia.utils.NotificationUtils
import br.dev.marconi.lyricalia.utils.SpotifyUtils
import br.dev.marconi.lyricalia.utils.SpotifyUtils.Companion.REQUEST_CODE
import br.dev.marconi.lyricalia.utils.StorageUtils
import br.dev.marconi.lyricalia.viewModels.SpotifyLinkViewModel
import br.dev.marconi.lyricalia.viewModels.SpotifyLinkViewModelFactory
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SpotifyLinkActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySpotifyLinkBinding
    private lateinit var notificationManager: NotificationManager
    private val requestPermissionLauncher = registerForActivityResult(RequestPermission()) { }
    private lateinit var viewModel: SpotifyLinkViewModel


    // region Permissions
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        binding.showPermissionRationale = false

        if (grantResults[permissions.indexOf("android.permission.POST_NOTIFICATIONS")] == PackageManager.PERMISSION_GRANTED) {
            NotificationUtils.notifySpotifyLinkStarted(notificationManager, this)
        }

        SpotifyUtils.authenticateUser(this)
    }

    private fun setupSpotifyPrompt() {
        val firstName = viewModel.currentUser.value!!.name.split(" ").first()
        val hint = "$firstName, conecte com sua biblioteca do Spotify para continuar"
        binding.spotifyHint.text = SpannableString(hint).also {
            it.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                firstName.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        binding.linkButton.setOnClickListener {
            binding.isLoadingSpotify = true
            checkNotificationPermission()
        }

        binding.isLoadingSpotify = false
    }

    private fun checkNotificationPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                NotificationUtils.notifySpotifyLinkStarted(notificationManager, this)
                SpotifyUtils.authenticateUser(this)
            }
            else -> {
                setupPermissionRationaleButtons()
                binding.showPermissionRationale = true
            }
        }
    }

    private fun setupPermissionRationaleButtons() {
        binding.allowNotificationsButton.setOnClickListener {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        binding.skipNotificationsButton.setOnClickListener {
            binding.showPermissionRationale = false
            SpotifyUtils.authenticateUser(this)
        }
    }
    // endregion

    // region UI Setups
    private fun setupSpotifyLinkActivity() {
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.spotifyLink) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupLogoutButton() {
        binding.logoutButton.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        StorageUtils(applicationContext.filesDir).deleteUser()
        NavigationUtils.navigateToLogin(this)
    }
    // endregion

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        binding.isLoadingSpotify = true
        if (requestCode == SpotifyUtils.REQUEST_CODE) {
            val response = AuthorizationClient.getResponse(resultCode, intent)
            handleSpotifyLoginResult(response)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        binding = ActivitySpotifyLinkBinding.inflate(layoutInflater)

        val vmFactory = SpotifyLinkViewModelFactory(applicationContext.filesDir)
        viewModel = ViewModelProvider(this, vmFactory)[SpotifyLinkViewModel::class.java]

        if (viewModel.currentUser.value == null) {
            Log.d("IF1001_P3_LYRICALIA", "no user, returning to login");
            NavigationUtils.navigateToLogin(this)
            return
        }

        setupSpotifyLinkActivity()
        setupLogoutButton()
    }

    override fun onStart() {
        super.onStart()

        binding.isLoadingSpotify = true

        if (viewModel.currentUser.value!!.spotifyToken != null) {
            NavigationUtils.navigateToMenu(this)
            return
        }

        val spotifyResponse = AuthorizationResponse.fromUri(intent.data)
        if (intent.data != null) {
            handleSpotifyLoginResult(spotifyResponse)
            return
        }

        setupSpotifyPrompt()
    }

    private fun handleSpotifyLoginResult(response: AuthorizationResponse) {
        binding.isLoadingSpotify = true

        when (response.type) {
            AuthorizationResponse.Type.CODE -> {
                verifyAuthenticity(response.code)
            }
            AuthorizationResponse.Type.ERROR -> {
                binding.spotifyHint.text = "Não conseguimos acessar sua biblioteca, quer tentar novamente?"
                binding.isLoadingSpotify = false
                Log.e("IF1001_P3_LYRICALIA", "FAILED on Spotify authorization")
            }
            else -> {
                binding.spotifyHint.text = "Parece que isso não deu certo, quer tentar novamente?"
                binding.isLoadingSpotify = false
                Log.e("IF1001_P3_LYRICALIA", "Maybe the flow was cancelled?")
            }
        }
    }

    private fun verifyAuthenticity(authorizationCode: String) {
        try {
            var credentials: SpotifyCredentialsEntity
            lifecycleScope.launch(Dispatchers.IO) {
                credentials = SpotifyUtils.exchangeAndSaveTokens(
                    applicationContext,
                    authorizationCode
                )

                SpotifyUtils.saveCredentials(applicationContext, credentials)

                val user = viewModel.currentUser.value!!
                user.spotifyToken = credentials.accessToken
                StorageUtils(applicationContext.filesDir).saveUser(user)

                SpotifyUtils.dispatchProcessUserLibrary(applicationContext)
                NavigationUtils.navigateToMenu(applicationContext)
            }
        } catch (ex: Exception) {
            Toast.makeText(this, "Erro ao autenticar: ${ex.message}", Toast.LENGTH_LONG).show()
            binding.spotifyHint.text = "Tivemos problemas ao falar com o Spotify, quer tentar novamente?"
            binding.isLoadingSpotify = false
        }
    }
}