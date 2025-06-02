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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import br.dev.marconi.lyricalia.databinding.ActivitySpotifyLinkBinding
import br.dev.marconi.lyricalia.repositories.login.models.User
import br.dev.marconi.lyricalia.utils.NavigationUtils
import br.dev.marconi.lyricalia.utils.NotificationUtils
import br.dev.marconi.lyricalia.utils.SpotifyUtils
import br.dev.marconi.lyricalia.utils.SpotifyUtils.Companion.REQUEST_CODE
import br.dev.marconi.lyricalia.utils.StorageUtils
import br.dev.marconi.lyricalia.utils.managers.KeyStoreManager
import br.dev.marconi.lyricalia.utils.managers.KeyStoreManager.Companion.DATA_STORE_KEY_ENCRYPTABLES
import br.dev.marconi.lyricalia.utils.managers.KeyStoreManager.Companion.SPOTIFY_CLIENT_ID_KEY
import br.dev.marconi.lyricalia.utils.managers.KeyStoreManager.Companion.SPOTIFY_CLIENT_SECRET_KEY
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationResponse
import kotlinx.coroutines.launch

class SpotifyLinkActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySpotifyLinkBinding
    private lateinit var notificationManager: NotificationManager
    private val requestPermissionLauncher = registerForActivityResult(RequestPermission()) { }

    private val ds: DataStore<Preferences> by preferencesDataStore(name = DATA_STORE_KEY_ENCRYPTABLES)

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE) {
            val response = AuthorizationClient.getResponse(resultCode, intent)
            handleSpotifyLoginResult(response)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        val ksMgrClient = KeyStoreManager(SPOTIFY_CLIENT_ID_KEY)
        val ksMgrSecret = KeyStoreManager(SPOTIFY_CLIENT_SECRET_KEY)

        val clientId = "\\"
        val encryptedClientId = ksMgrClient.encrypt(clientId, SPOTIFY_CLIENT_ID_KEY)

        val clientSecret = "//"
        val encryptedSecret = ksMgrSecret.encrypt(clientSecret, SPOTIFY_CLIENT_SECRET_KEY)

        lifecycleScope.launch {
            KeyStoreManager.writeToPrefs(ds, stringPreferencesKey(SPOTIFY_CLIENT_ID_KEY), encryptedClientId)
            KeyStoreManager.writeToPrefs(ds, stringPreferencesKey(SPOTIFY_CLIENT_SECRET_KEY), encryptedSecret)
        }

//        lifecycleScope.launch {
//            val pref = KeyStoreManager.readPrefs(ds, stringPreferencesKey(SPOTIFY_CLIENT_ID_KEY_PREFS))
//            Log.d("IF1001_P3_LYRICALIA", pref)
//        }


        super.onCreate(savedInstanceState)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        binding = ActivitySpotifyLinkBinding.inflate(layoutInflater)

        setupSpotifyLinkActivity()
        setupLogoutButton()
    }

    override fun onStart() {
        super.onStart()

        val user = StorageUtils(this).retrieveUser()
        if (user == null) {
            Log.d("IF1001_P3_LYRICALIA", "no user, returning to login");
            NavigationUtils.navigateToLogin(this)
            return
        }

        if (user.spotifyToken != null) {
            NavigationUtils.navigateToMenu(this)
            return
        }


        val spotifyResponse = AuthorizationResponse.fromUri(intent.data)
        if (intent.data != null) {
            handleSpotifyLoginResult(spotifyResponse)
            return
        }

        setupSpotifyPrompt(user)
    }

    private fun setupSpotifyLinkActivity() {
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.spotifyLink) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupSpotifyPrompt(user: User) {
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

        binding.linkButton.setOnClickListener {
            binding.isLoadingSpotify = true
            checkNotificationPermission()
        }
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

    private fun handleSpotifyLoginResult(response: AuthorizationResponse) {
        when (response.type) {
            AuthorizationResponse.Type.CODE -> {
                Log.d("IF1001_P3_LYRICALIA", "SUCCESS on Spotify authorization")
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
            SpotifyUtils.exchangeAndSaveTokens(authorizationCode)
        } catch (_: Exception) {
            binding.spotifyHint.text = "Tivemos problemas ao falar com o Spotify, quer tentar novamente?"
            binding.isLoadingSpotify = false
        }

    }

    private fun setupLogoutButton() {
        binding.logoutButton.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        StorageUtils(applicationContext).deleteUser()
        NavigationUtils.navigateToLogin(this)
    }
}