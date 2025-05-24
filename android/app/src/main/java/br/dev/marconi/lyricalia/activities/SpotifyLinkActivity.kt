package br.dev.marconi.lyricalia.activities

import android.Manifest
import android.app.NotificationManager
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
import androidx.lifecycle.lifecycleScope
import br.dev.marconi.lyricalia.databinding.ActivitySpotifyLinkBinding
import br.dev.marconi.lyricalia.repositories.login.models.User
import br.dev.marconi.lyricalia.utils.NavigationUtils
import br.dev.marconi.lyricalia.utils.NotificationUtils
import br.dev.marconi.lyricalia.utils.StorageUtils
import kotlinx.coroutines.launch

class SpotifyLinkActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySpotifyLinkBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpotifyLinkBinding.inflate(layoutInflater)

        Log.d("IF1001_P3_LYRICALIA", "onCreate called on Menu")
        setupSpotifyLinkActivity()
        setupLogoutButton()
    }

    override fun onStart() {
        super.onStart()
        Log.d("IF1001_P3_LYRICALIA", "onStart called on Menu")

        StorageUtils(this).retrieveUser()?.run {
            setupSpotifyPrompt(this)
        } ?: { Log.d("IF1001_P3_LYRICALIA", "no user, returning to login"); NavigationUtils.navigateToLogin(this) }
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

    fun setupSpotifyPrompt(user: User) {
        Log.d("IF1001_P3_LYRICALIA", "setting up spotify prompt")
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
            linkToSpotify()
        }
    }

    private fun linkToSpotify() {
        lifecycleScope.launch {
            binding.isLoadingSpotify = true

            Log.d("IF1001_P3_LYRICALIA", "LINK SPOTIFY CLICKED")
            setupNotifications()

            binding.isLoadingSpotify = false
        }
    }

    private fun setupNotifications() {
        val requestPermissionLauncher = registerForActivityResult(
            RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                NotificationUtils.createLinkingNotificationChannel(notificationManager)
            } else {
                Log.d("IF1001_P3_LYRICALIA", "Permission NOT granted for notifications")
            }
        }

        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                Toast.makeText(this, "NOTIF PERM GRANTED", Toast.LENGTH_LONG).show()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS) -> {
//                binding.showPermissionRationale
            }

            else -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun setupLogoutButton() {
        binding.logoutButton.setOnClickListener {
            Log.d("IF1001_P3_LYRICALIA", "LOGOOOOOOOOOOOUUUUUUUUUUTTTTTTTT")
            logout()
        }
    }

    private fun logout() {
        StorageUtils(applicationContext).deleteUser()
        NavigationUtils.navigateToLogin(this)
    }
}